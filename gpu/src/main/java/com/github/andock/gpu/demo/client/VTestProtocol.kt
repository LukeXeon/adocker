package com.github.andock.gpu.demo.client

/**
 * VTest Protocol Constants
 *
 * vtest protocol uses TLV format: [Length(4)][Command(4)][Data(variable)]
 */
object VTestProtocol {
    // Protocol version
    const val VTEST_PROTOCOL_VERSION = 4u

    // Command IDs
    const val VCMD_PROTOCOL_VERSION = 0u
    const val VCMD_GET_CAPS2 = 3u
    const val VCMD_GET_CAPSET = 4u
    const val VCMD_CONTEXT_INIT = 5u
    const val VCMD_RESOURCE_CREATE_BLOB = 10u
    const val VCMD_RESOURCE_UNREF = 11u
    const val VCMD_SUBMIT_CMD = 12u
    const val VCMD_TRANSFER_PUT2 = 13u
    const val VCMD_TRANSFER_GET2 = 14u

    // Capset IDs
    const val VIRGL_CAPSET_VIRGL = 1u
    const val VIRGL_CAPSET_VIRGL2 = 2u

    // Blob types
    const val VIRGL_BLOB_TYPE_HOST3D = 1u
    const val VIRGL_BLOB_TYPE_GUEST = 4u

    // Blob flags
    const val VIRGL_BLOB_FLAG_USE_MAPPABLE = (1 shl 0)
    const val VIRGL_BLOB_FLAG_USE_SHAREABLE = (1 shl 1)
    const val VIRGL_BLOB_FLAG_USE_CROSS_DEVICE = (1 shl 2)

    // virgl commands (subset for demo)
    const val VIRGL_CCMD_NOP = 0u
    const val VIRGL_CCMD_CREATE_OBJECT = 1u
    const val VIRGL_CCMD_BIND_OBJECT = 2u
    const val VIRGL_CCMD_DESTROY_OBJECT = 3u
    const val VIRGL_CCMD_SET_VIEWPORT_STATE = 4u
    const val VIRGL_CCMD_SET_FRAMEBUFFER_STATE = 5u
    const val VIRGL_CCMD_CLEAR = 8u
    const val VIRGL_CCMD_DRAW_VBO = 9u

    // Object types
    const val VIRGL_OBJECT_BLEND = 1u
    const val VIRGL_OBJECT_RASTERIZER = 2u
    const val VIRGL_OBJECT_DSA = 3u
    const val VIRGL_OBJECT_SHADER = 4u
    const val VIRGL_OBJECT_VERTEX_ELEMENTS = 5u
    const val VIRGL_OBJECT_SURFACE = 6u
}
