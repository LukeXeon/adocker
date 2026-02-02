package com.github.andock.gpu.virgl

/**
 * VTest Protocol Commands (vtest_protocol.h)
 *
 * Protocol version: 4
 * Header format: [length_dw: uint32][cmd_id: uint32]
 */
object VTestCommand {
    // Version 1 commands
    const val VCMD_GET_CAPS = 1
    const val VCMD_RESOURCE_CREATE = 2
    const val VCMD_RESOURCE_UNREF = 3
    const val VCMD_TRANSFER_GET = 4
    const val VCMD_TRANSFER_PUT = 5
    const val VCMD_SUBMIT_CMD = 6
    const val VCMD_RESOURCE_BUSY_WAIT = 7
    const val VCMD_CREATE_RENDERER = 8

    // Version 2 commands
    const val VCMD_GET_CAPS2 = 9
    const val VCMD_PING_PROTOCOL_VERSION = 10
    const val VCMD_PROTOCOL_VERSION = 11
    const val VCMD_RESOURCE_CREATE2 = 12
    const val VCMD_TRANSFER_GET2 = 13
    const val VCMD_TRANSFER_PUT2 = 14

    // Version 3 commands (blob resources and sync)
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

    // Version 4 commands (DRM sync)
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

    // Blob resource types
    const val BLOB_TYPE_GUEST = 0x0001
    const val BLOB_TYPE_HOST3D = 0x0002
    const val BLOB_TYPE_HOST3D_GUEST = 0x0003
    const val BLOB_TYPE_GUEST_VRAM = 0x0004

    // Blob resource flags
    const val BLOB_FLAG_MAPPABLE = 0x0001
    const val BLOB_FLAG_SHAREABLE = 0x0002
    const val BLOB_FLAG_CROSS_DEVICE = 0x0004
    const val BLOB_FLAG_EXPORT = 0x0008  // Custom flag for export

    // Resource FD types
    const val FD_TYPE_INVALID = 0
    const val FD_TYPE_DMABUF = 1
    const val FD_TYPE_OPAQUE = 2
    const val FD_TYPE_SHM = 3

    /**
     * Blob resource creation arguments
     */
    data class BlobArgs(
        val resId: Int,
        val blobType: Int,      // BLOB_TYPE_*
        val blobFlags: Int,     // BLOB_FLAG_*
        val blobId: Long,       // 64-bit blob ID
        val size: Long          // 64-bit size
    )

    /**
     * Command submission v2 arguments
     */
    data class SubmitCmdArgs(
        val flags: Int,
        val ringIdx: Int,
        val cmdSize: Int,
        val numInSyncs: Int,
        val inSyncIds: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SubmitCmdArgs
            if (flags != other.flags) return false
            if (ringIdx != other.ringIdx) return false
            if (cmdSize != other.cmdSize) return false
            if (numInSyncs != other.numInSyncs) return false
            if (!inSyncIds.contentEquals(other.inSyncIds)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = flags
            result = 31 * result + ringIdx
            result = 31 * result + cmdSize
            result = 31 * result + numInSyncs
            result = 31 * result + inSyncIds.contentHashCode()
            return result
        }
    }
}
