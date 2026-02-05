#include "vtest_io.h"
#include <unistd.h>
#include <errno.h>
#include <android/log.h>

#define LOG_TAG "VTest-IO"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

VTestIO::VTestIO(int fd) : fd_(fd) {
}

int VTestIO::readHeader(VTestHeader& header) {
    uint32_t buf[VTEST_HDR_SIZE];
    int ret = readFull(buf, sizeof(buf));
    if (ret != 0) {
        return ret;
    }

    header.length = buf[VTEST_CMD_LEN];
    header.cmd_id = buf[VTEST_CMD_ID];

    return 0;
}

int VTestIO::readData(void* data, size_t len) {
    return readFull(data, len);
}

int VTestIO::readData(std::vector<uint32_t>& data, uint32_t count) {
    data.resize(count);
    return readFull(data.data(), count * sizeof(uint32_t));
}

int VTestIO::writeResponse(uint32_t cmd_id, const void* data, size_t len) {
    uint32_t header[VTEST_HDR_SIZE];
    header[VTEST_CMD_LEN] = len / sizeof(uint32_t);
    header[VTEST_CMD_ID] = cmd_id;

    int ret = writeFull(header, sizeof(header));
    if (ret != 0) {
        return ret;
    }

    if (len > 0) {
        ret = writeFull(data, len);
        if (ret != 0) {
            return ret;
        }
    }

    return 0;
}

int VTestIO::writeResponse(uint32_t cmd_id, const std::vector<uint32_t>& data) {
    return writeResponse(cmd_id, data.data(), data.size() * sizeof(uint32_t));
}

int VTestIO::readFull(void* buf, size_t len) {
    size_t total = 0;
    char* ptr = static_cast<char*>(buf);

    while (total < len) {
        ssize_t n = read(fd_, ptr + total, len - total);

        if (n < 0) {
            if (errno == EINTR) {
                continue;  // Interrupted, retry
            }
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                return -EAGAIN;  // No data available (non-blocking)
            }
            LOGE("read error on fd %d: %s", fd_, strerror(errno));
            return -errno;
        }

        if (n == 0) {
            // Connection closed
            return -ECONNRESET;
        }

        total += n;
    }

    return 0;
}

int VTestIO::writeFull(const void* buf, size_t len) {
    size_t total = 0;
    const char* ptr = static_cast<const char*>(buf);

    while (total < len) {
        ssize_t n = write(fd_, ptr + total, len - total);

        if (n < 0) {
            if (errno == EINTR) {
                continue;  // Interrupted, retry
            }
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                return -EAGAIN;  // Would block (rare for write)
            }
            LOGE("write error on fd %d: %s", fd_, strerror(errno));
            return -errno;
        }

        total += n;
    }

    return 0;
}
