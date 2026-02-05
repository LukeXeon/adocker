#ifndef VTEST_EPOLL_SERVER_H
#define VTEST_EPOLL_SERVER_H

#include "virgl_callbacks.h"
#include <string>
#include <unordered_map>
#include <memory>
#include <atomic>

// Forward declaration
class VTestClient;

/**
 * Epoll-based vtest Server
 *
 * Features:
 * - Unix domain socket listener
 * - Edge-triggered epoll for multi-client I/O multiplexing
 * - Single-threaded event loop
 * - Non-blocking I/O
 * - Automatic client lifecycle management
 *
 * Architecture:
 * - One server socket for accepting new connections
 * - Multiple client sockets for data exchange
 * - All managed by a single epoll instance
 */
class EpollServer {
public:
    EpollServer();
    ~EpollServer();

    /**
     * Initialize server
     * @param socket_path Unix socket path
     * @return true if successful
     */
    bool init(const char* socket_path);

    /**
     * Run server event loop (blocks until stopped)
     */
    void run();

    /**
     * Stop server (can be called from another thread)
     */
    void stop();

    /**
     * Get client by file descriptor
     * @param fd Client file descriptor
     * @return VTestClient pointer or nullptr
     */
    VTestClient* getClient(int fd);

private:
    /**
     * Handle new client connection
     */
    void handleNewConnection();

    /**
     * Handle client data or error
     * @param fd Client file descriptor
     * @param events Epoll events
     */
    void handleClientData(int fd, uint32_t events);

    /**
     * Remove and cleanup client
     * @param fd Client file descriptor
     */
    void removeClient(int fd);

    int epoll_fd_ = -1;
    int server_fd_ = -1;
    std::string socket_path_;
    std::atomic<bool> running_{false};

    ServerContext server_ctx_;
    std::unordered_map<int, std::unique_ptr<VTestClient>> clients_;

    static constexpr int MAX_EVENTS = 32;
    static constexpr int BACKLOG = 128;
};

#endif // VTEST_EPOLL_SERVER_H
