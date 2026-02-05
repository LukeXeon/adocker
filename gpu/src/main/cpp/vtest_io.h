#ifndef VTEST_IO_H
#define VTEST_IO_H

#include <cstdint>
#include <vector>
#include <sys/uio.h>

// vtest protocol constants
#define VTEST_PROTOCOL_VERSION 4
#define VTEST_HDR_SIZE 2  // 2 uint32_t

// Header indices
#define VTEST_CMD_LEN 0  // Data length in uint32_t (excluding header)
#define VTEST_CMD_ID  1  // Command ID

/**
 * vtest Protocol Header
 */
struct VTestHeader {
    uint32_t length;  // Data length in uint32_t
    uint32_t cmd_id;  // Command ID
};

/**
 * vtest I/O Helper Class
 *
 * Provides buffered I/O for vtest protocol messages.
 * Handles reading headers, data, and writing responses.
 */
class VTestIO {
public:
    explicit VTestIO(int fd);

    /**
     * Read message header
     * @param header Output header
     * @return 0 on success, -errno on error
     */
    int readHeader(VTestHeader& header);

    /**
     * Read raw data
     * @param data Output buffer
     * @param len Length in bytes
     * @return 0 on success, -errno on error
     */
    int readData(void* data, size_t len);

    /**
     * Read data into vector
     * @param data Output vector (will be resized)
     * @param count Number of uint32_t to read
     * @return 0 on success, -errno on error
     */
    int readData(std::vector<uint32_t>& data, uint32_t count);

    /**
     * Write response
     * @param cmd_id Command ID
     * @param data Response data
     * @param len Data length in bytes
     * @return 0 on success, -errno on error
     */
    int writeResponse(uint32_t cmd_id, const void* data, size_t len);

    /**
     * Write response from vector
     * @param cmd_id Command ID
     * @param data Response data
     * @return 0 on success, -errno on error
     */
    int writeResponse(uint32_t cmd_id, const std::vector<uint32_t>& data);

    /**
     * Get file descriptor
     */
    int getFd() const { return fd_; }

private:
    /**
     * Read exactly len bytes (handles partial reads)
     * @return 0 on success, -errno on error
     */
    int readFull(void* buf, size_t len);

    /**
     * Write exactly len bytes (handles partial writes)
     * @return 0 on success, -errno on error
     */
    int writeFull(const void* buf, size_t len);

    int fd_;
};

#endif // VTEST_IO_H
