package com.github.andock.gpu.virgl

/**
 * VTest Protocol Constants - from vtest_protocol.h
 */
object VTestProtocol {
    const val PROTOCOL_VERSION = 4
    const val DEFAULT_SOCKET_NAME = "/tmp/.virgl_test"

    // Header structure
    const val HDR_SIZE = 2
    const val CMD_LEN = 0
    const val CMD_ID = 1
    const val CMD_DATA_START = 2

    // Protocol commands
    const val VCMD_GET_CAPS = 1
    const val VCMD_RESOURCE_CREATE = 2
    const val VCMD_RESOURCE_UNREF = 3
    const val VCMD_TRANSFER_GET = 4
    const val VCMD_TRANSFER_PUT = 5
    const val VCMD_SUBMIT_CMD = 6
    const val VCMD_RESOURCE_BUSY_WAIT = 7
    const val VCMD_CREATE_RENDERER = 8
    const val VCMD_GET_CAPS2 = 9
    const val VCMD_PING_PROTOCOL_VERSION = 10
    const val VCMD_PROTOCOL_VERSION = 11
    const val VCMD_RESOURCE_CREATE2 = 12
    const val VCMD_TRANSFER_GET2 = 13
    const val VCMD_TRANSFER_PUT2 = 14
    const val VCMD_GET_PARAM = 15
    const val VCMD_GET_CAPSET = 16
    const val VCMD_CONTEXT_INIT = 17
    const val VCMD_RESOURCE_CREATE_BLOB = 18
    const val VCMD_SYNC_CREATE = 19
    const val VCMD_SYNC_UNREF = 20
    const val VCMD_SYNC_READ = 21
    const val VCMD_SYNC_WRITE = 22
    const val VCMD_SYNC_WAIT = 23
    const val VCMD_SUBMIT_CMD2 = 24
    const val VCMD_DRM_SYNC_CREATE = 25
    const val VCMD_DRM_SYNC_DESTROY = 26
    const val VCMD_DRM_SYNC_HANDLE_TO_FD = 27
    const val VCMD_DRM_SYNC_FD_TO_HANDLE = 28
    const val VCMD_DRM_SYNC_IMPORT_SYNC_FILE = 29
    const val VCMD_DRM_SYNC_EXPORT_SYNC_FILE = 30
    const val VCMD_DRM_SYNC_WAIT = 31
    const val VCMD_DRM_SYNC_RESET = 32
    const val VCMD_DRM_SYNC_SIGNAL = 33
    const val VCMD_DRM_SYNC_TIMELINE_SIGNAL = 34
    const val VCMD_DRM_SYNC_TIMELINE_WAIT = 35
    const val VCMD_DRM_SYNC_QUERY = 36
    const val VCMD_DRM_SYNC_TRANSFER = 37
    const val VCMD_RESOURCE_EXPORT_FD = 38

    // Resource create message structure
    const val RES_CREATE_SIZE = 10
    const val RES_CREATE_RES_HANDLE = 0
    const val RES_CREATE_TARGET = 1
    const val RES_CREATE_FORMAT = 2
    const val RES_CREATE_BIND = 3
    const val RES_CREATE_WIDTH = 4
    const val RES_CREATE_HEIGHT = 5
    const val RES_CREATE_DEPTH = 6
    const val RES_CREATE_ARRAY_SIZE = 7
    const val RES_CREATE_LAST_LEVEL = 8
    const val RES_CREATE_NR_SAMPLES = 9

    // Resource create2 message structure
    const val RES_CREATE2_SIZE = 11
    const val RES_CREATE2_DATA_SIZE = 10

    // Resource unref
    const val RES_UNREF_SIZE = 1
    const val RES_UNREF_RES_HANDLE = 0

    // Transfer header structure
    const val TRANSFER_HDR_SIZE = 11
    const val TRANSFER_RES_HANDLE = 0
    const val TRANSFER_LEVEL = 1
    const val TRANSFER_STRIDE = 2
    const val TRANSFER_LAYER_STRIDE = 3
    const val TRANSFER_X = 4
    const val TRANSFER_Y = 5
    const val TRANSFER_Z = 6
    const val TRANSFER_WIDTH = 7
    const val TRANSFER_HEIGHT = 8
    const val TRANSFER_DEPTH = 9
    const val TRANSFER_DATA_SIZE = 10

    // Transfer2 header structure
    const val TRANSFER2_HDR_SIZE = 10
    const val TRANSFER2_OFFSET = 9

    // Busy wait
    const val BUSY_WAIT_FLAG_WAIT = 1
    const val BUSY_WAIT_SIZE = 2
    const val BUSY_WAIT_HANDLE = 0
    const val BUSY_WAIT_FLAGS = 1

    // Protocol version
    const val PING_PROTOCOL_VERSION_SIZE = 0
    const val PROTOCOL_VERSION_SIZE = 1

    // Get param
    const val GET_PARAM_SIZE = 1
    const val PARAM_MAX_TIMELINE_COUNT = 1
    const val PARAM_HAS_TIMELINE_SYNCOBJ = 2

    // Get capset
    const val GET_CAPSET_SIZE = 2
    const val GET_CAPSET_ID = 0
    const val GET_CAPSET_VERSION = 1

    // Context init
    const val CONTEXT_INIT_SIZE = 1
    const val CONTEXT_INIT_CAPSET_ID = 0

    // Blob types
    const val BLOB_TYPE_GUEST = 1
    const val BLOB_TYPE_HOST3D = 2
    const val BLOB_TYPE_HOST3D_GUEST = 3

    // Blob flags
    const val BLOB_FLAG_MAPPABLE = 1 shl 0
    const val BLOB_FLAG_SHAREABLE = 1 shl 1
    const val BLOB_FLAG_CROSS_DEVICE = 1 shl 2

    // Resource create blob
    const val RES_CREATE_BLOB_SIZE = 6
    const val RES_CREATE_BLOB_TYPE = 0
    const val RES_CREATE_BLOB_FLAGS = 1
    const val RES_CREATE_BLOB_SIZE_LO = 2
    const val RES_CREATE_BLOB_SIZE_HI = 3
    const val RES_CREATE_BLOB_ID_LO = 4
    const val RES_CREATE_BLOB_ID_HI = 5

    // Sync wait flags
    const val SYNC_WAIT_FLAG_ANY = 1 shl 0

    // Submit cmd2 flags
    const val SUBMIT_CMD2_FLAG_RING_IDX = 1 shl 0
    const val SUBMIT_CMD2_FLAG_IN_FENCE_FD = 1 shl 1
    const val SUBMIT_CMD2_FLAG_OUT_FENCE_FD = 1 shl 2
}
