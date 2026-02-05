#include "vtest_epoll_server.h"
#include "vtest_client.h"
#include "virgl_init.h"
#include <android/log.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/epoll.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <cstring>

#define LOG_TAG "VTest-Epoll"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

EpollServer::EpollServer() {
}

EpollServer::~EpollServer() {
    stop();
}

bool EpollServer::init(const char* socket_path) {
    LOGI("Initializing epoll server on socket: %s", socket_path);

    socket_path_ = socket_path;

    // 1. Create Unix domain socket
    server_fd_ = socket(AF_UNIX, SOCK_STREAM | SOCK_CLOEXEC | SOCK_NONBLOCK, 0);
    if (server_fd_ < 0) {
        LOGE("Failed to create socket: %s", strerror(errno));
        return false;
    }

    // 2. Bind to socket path
    struct sockaddr_un addr = {};
    addr.sun_family = AF_UNIX;
    strncpy(addr.sun_path, socket_path, sizeof(addr.sun_path) - 1);

    // Remove existing socket file
    unlink(socket_path);

    if (bind(server_fd_, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        LOGE("Failed to bind socket: %s", strerror(errno));
        close(server_fd_);
        server_fd_ = -1;
        return false;
    }

    // 3. Listen
    if (listen(server_fd_, BACKLOG) < 0) {
        LOGE("Failed to listen: %s", strerror(errno));
        close(server_fd_);
        server_fd_ = -1;
        unlink(socket_path);
        return false;
    }

    LOGI("Server socket listening on %s (fd=%d)", socket_path, server_fd_);

    // 4. Create epoll instance
    epoll_fd_ = epoll_create1(EPOLL_CLOEXEC);
    if (epoll_fd_ < 0) {
        LOGE("Failed to create epoll: %s", strerror(errno));
        close(server_fd_);
        server_fd_ = -1;
        unlink(socket_path);
        return false;
    }

    // 5. Add server socket to epoll (edge-triggered)
    struct epoll_event ev = {};
    ev.events = EPOLLIN | EPOLLET;  // Edge-triggered
    ev.data.fd = server_fd_;
    if (epoll_ctl(epoll_fd_, EPOLL_CTL_ADD, server_fd_, &ev) < 0) {
        LOGE("Failed to add server socket to epoll: %s", strerror(errno));
        close(epoll_fd_);
        close(server_fd_);
        epoll_fd_ = -1;
        server_fd_ = -1;
        unlink(socket_path);
        return false;
    }

    LOGI("Epoll instance created (fd=%d)", epoll_fd_);

    // 6. Initialize VirGL renderer
    if (!initializeVirGL(&server_ctx_)) {
        LOGE("Failed to initialize VirGL");
        close(epoll_fd_);
        close(server_fd_);
        epoll_fd_ = -1;
        server_fd_ = -1;
        unlink(socket_path);
        return false;
    }

    LOGI("Epoll server initialized successfully");
    return true;
}

void EpollServer::run() {
    LOGI("Starting epoll event loop");

    struct epoll_event events[MAX_EVENTS];
    running_ = true;

    while (running_) {
        // Wait for events (1 second timeout)
        int nfds = epoll_wait(epoll_fd_, events, MAX_EVENTS, 1000);

        if (nfds < 0) {
            if (errno == EINTR) {
                continue;  // Interrupted by signal, retry
            }
            LOGE("epoll_wait error: %s", strerror(errno));
            break;
        }

        // Process events
        for (int i = 0; i < nfds; i++) {
            int fd = events[i].data.fd;

            if (fd == server_fd_) {
                // New connection
                handleNewConnection();
            } else {
                // Client data or error
                handleClientData(fd, events[i].events);
            }
        }
    }

    LOGI("Epoll event loop stopped");

    // Cleanup
    cleanupVirGL(&server_ctx_);

    clients_.clear();

    if (epoll_fd_ >= 0) {
        close(epoll_fd_);
        epoll_fd_ = -1;
    }

    if (server_fd_ >= 0) {
        close(server_fd_);
        server_fd_ = -1;
    }

    if (!socket_path_.empty()) {
        unlink(socket_path_.c_str());
    }
}

void EpollServer::stop() {
    LOGI("Stopping epoll server");
    running_ = false;
}

void EpollServer::handleNewConnection() {
    // Edge-triggered: must accept ALL pending connections
    while (true) {
        int client_fd = accept4(server_fd_, nullptr, nullptr, SOCK_CLOEXEC | SOCK_NONBLOCK);

        if (client_fd < 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                // No more connections
                break;
            }
            LOGE("accept4 error: %s", strerror(errno));
            break;
        }

        LOGI("New client connected (fd=%d)", client_fd);

        // Create client handler
        auto client = std::make_unique<VTestClient>(client_fd, &server_ctx_);

        // Add to epoll (edge-triggered)
        struct epoll_event ev = {};
        ev.events = EPOLLIN | EPOLLET;
        ev.data.fd = client_fd;
        if (epoll_ctl(epoll_fd_, EPOLL_CTL_ADD, client_fd, &ev) < 0) {
            LOGE("Failed to add client to epoll: %s", strerror(errno));
            // Client will be destroyed automatically
            continue;
        }

        clients_[client_fd] = std::move(client);
        LOGI("Client %d added (total clients: %zu)", client_fd, clients_.size());
    }
}

void EpollServer::handleClientData(int fd, uint32_t events) {
    auto it = clients_.find(fd);
    if (it == clients_.end()) {
        LOGW("Unknown client fd=%d", fd);
        return;
    }

    VTestClient* client = it->second.get();

    // Check for errors or hangup
    if (events & (EPOLLERR | EPOLLHUP)) {
        LOGI("Client %d disconnected (events=0x%x)", fd, events);
        removeClient(fd);
        return;
    }

    // Handle input data
    if (events & EPOLLIN) {
        // Edge-triggered: must read until EAGAIN
        while (true) {
            int ret = client->processCommands();

            if (ret == -EAGAIN || ret == -EWOULDBLOCK) {
                // No more data available
                break;
            }

            if (ret < 0) {
                // Error or disconnection
                LOGI("Client %d error: %d", fd, ret);
                removeClient(fd);
                break;
            }
        }
    }
}

void EpollServer::removeClient(int fd) {
    auto it = clients_.find(fd);
    if (it == clients_.end()) {
        return;
    }

    LOGI("Removing client %d", fd);

    // Remove from epoll
    epoll_ctl(epoll_fd_, EPOLL_CTL_DEL, fd, nullptr);

    // Erase from map (will destroy VTestClient)
    clients_.erase(it);

    LOGI("Client %d removed (remaining clients: %zu)", fd, clients_.size());
}

VTestClient* EpollServer::getClient(int fd) {
    auto it = clients_.find(fd);
    if (it == clients_.end()) {
        return nullptr;
    }
    return it->second.get();
}
