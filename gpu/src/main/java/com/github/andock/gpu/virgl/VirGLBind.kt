package com.github.andock.gpu.virgl

/**
 * VirGL Resource Bind Flags and Capabilities - from virgl_hw.h
 */
object VirGLBind {
    // Resource bind flags
    const val DEPTH_STENCIL = 1 shl 0
    const val RENDER_TARGET = 1 shl 1
    const val SAMPLER_VIEW = 1 shl 3
    const val VERTEX_BUFFER = 1 shl 4
    const val INDEX_BUFFER = 1 shl 5
    const val CONSTANT_BUFFER = 1 shl 6
    const val DISPLAY_TARGET = 1 shl 7
    const val COMMAND_ARGS = 1 shl 8
    const val STREAM_OUTPUT = 1 shl 11
    const val SHADER_BUFFER = 1 shl 14
    const val QUERY_BUFFER = 1 shl 15
    const val CURSOR = 1 shl 16
    const val CUSTOM = 1 shl 17
    const val SCANOUT = 1 shl 18
    const val STAGING = 1 shl 19
    const val SHARED = 1 shl 20
    const val LINEAR = 1 shl 22

    // Shared subflags
    const val SHARED_SUBFLAGS = 0xff shl 24
    const val MINIGBM_CAMERA_WRITE = 1 shl 24
    const val MINIGBM_CAMERA_READ = 1 shl 25
    const val MINIGBM_HW_VIDEO_DECODER = 1 shl 26
    const val MINIGBM_HW_VIDEO_ENCODER = 1 shl 27
    const val MINIGBM_SW_READ_OFTEN = 1 shl 28
    const val MINIGBM_SW_READ_RARELY = 1 shl 29
    const val MINIGBM_SW_WRITE_OFTEN = 1 shl 30
    const val MINIGBM_SW_WRITE_RARELY = 1 shl 31
}

/**
 * VirGL Capability Bits - from virgl_hw.h
 */
object VirGLCap {
    const val NONE = 0
    const val TGSI_INVARIANT = 1 shl 0
    const val TEXTURE_VIEW = 1 shl 1
    const val SET_MIN_SAMPLES = 1 shl 2
    const val COPY_IMAGE = 1 shl 3
    const val TGSI_PRECISE = 1 shl 4
    const val TXQS = 1 shl 5
    const val MEMORY_BARRIER = 1 shl 6
    const val COMPUTE_SHADER = 1 shl 7
    const val FB_NO_ATTACH = 1 shl 8
    const val ROBUST_BUFFER_ACCESS = 1 shl 9
    const val TGSI_FBFETCH = 1 shl 10
    const val SHADER_CLOCK = 1 shl 11
    const val TEXTURE_BARRIER = 1 shl 12
    const val TGSI_COMPONENTS = 1 shl 13
    const val GUEST_MAY_INIT_LOG = 1 shl 14
    const val SRGB_WRITE_CONTROL = 1 shl 15
    const val QBO = 1 shl 16
    const val TRANSFER = 1 shl 17
    const val FBO_MIXED_COLOR_FORMATS = 1 shl 18
    const val HOST_IS_GLES = 1 shl 19
    const val BIND_COMMAND_ARGS = 1 shl 20
    const val MULTI_DRAW_INDIRECT = 1 shl 21
    const val INDIRECT_PARAMS = 1 shl 22
    const val TRANSFORM_FEEDBACK3 = 1 shl 23
    const val ASTC_3D = 1 shl 24
    const val INDIRECT_INPUT_ADDR = 1 shl 25
    const val COPY_TRANSFER = 1 shl 26
    const val CLIP_HALFZ = 1 shl 27
    const val APP_TWEAK_SUPPORT = 1 shl 28
    const val BGRA_SRGB_IS_EMULATED = 1 shl 29
    const val CLEAR_TEXTURE = 1 shl 30
    const val ARB_BUFFER_STORAGE = 1 shl 31
}

/**
 * VirGL Capability Bits V2 - from virgl_hw.h
 */
object VirGLCapV2 {
    const val BLEND_EQUATION = 1 shl 0
    const val UNTYPED_RESOURCE = 1 shl 1
    const val VIDEO_MEMORY = 1 shl 2
    const val MEMINFO = 1 shl 3
    const val STRING_MARKER = 1 shl 4
    const val DIFFERENT_GPU = 1 shl 5
    const val IMPLICIT_MSAA = 1 shl 6
    const val COPY_TRANSFER_BOTH_DIRECTIONS = 1 shl 7
    const val SCANOUT_USES_GBM = 1 shl 8
    const val SSO = 1 shl 9
    const val TEXTURE_SHADOW_LOD = 1 shl 10
    const val VS_VERTEX_LAYER = 1 shl 11
    const val VS_VIEWPORT_INDEX = 1 shl 12
    const val PIPELINE_STATISTICS_QUERY = 1 shl 13
    const val DRAW_PARAMETERS = 1 shl 14
    const val GROUP_VOTE = 1 shl 15
    const val MIRROR_CLAMP_TO_EDGE = 1 shl 16
    const val MIRROR_CLAMP = 1 shl 17
    const val RESOURCE_LAYOUT = 1 shl 18
}

/**
 * VirGL Texture Targets - PIPE_TEXTURE_*
 */
object VirGLTarget {
    const val BUFFER = 0
    const val TEXTURE_1D = 1
    const val TEXTURE_2D = 2
    const val TEXTURE_3D = 3
    const val TEXTURE_CUBE = 4
    const val TEXTURE_RECT = 5
    const val TEXTURE_1D_ARRAY = 6
    const val TEXTURE_2D_ARRAY = 7
    const val TEXTURE_CUBE_ARRAY = 8
}

/**
 * VirGL Error Codes - from virgl_hw.h
 */
object VirGLError {
    const val NONE = 0
    const val UNKNOWN = 1
    const val UNKNOWN_RESOURCE_FORMAT = 2
}

/**
 * VirGL Context Error Codes - from virgl_hw.h
 */
object VirGLCtxError {
    const val NONE = 0
    const val UNKNOWN = 1
    const val ILLEGAL_SHADER = 2
    const val ILLEGAL_HANDLE = 3
    const val ILLEGAL_RESOURCE = 4
    const val ILLEGAL_SURFACE = 5
    const val ILLEGAL_VERTEX_FORMAT = 6
    const val ILLEGAL_CMD_BUFFER = 7
    const val GLES_HAVE_TES_BUT_MISS_TCS = 8
    const val GL_ANY_SAMPLES_PASSED = 9
    const val ILLEGAL_FORMAT = 10
    const val ILLEGAL_SAMPLER_VIEW_TARGET = 11
    const val TRANSFER_IOV_BOUNDS = 12
    const val ILLEGAL_DUAL_SRC_BLEND = 13
    const val UNSUPPORTED_FUNCTION = 14
    const val ILLEGAL_PROGRAM_PIPELINE = 15
    const val TOO_MANY_VERTEX_ATTRIBUTES = 16
    const val UNSUPPORTED_TEX_WRAP = 17
    const val CUBE_MAP_FACE_OUT_OF_RANGE = 18
    const val BLIT_AREA_OUT_OF_RANGE = 19
    const val SSBO_BINDING_RANGE = 20
    const val RESOURCE_OUT_OF_RANGE = 21
}

/**
 * VirGL Resource Flags - from virgl_hw.h
 */
object VirGLResourceFlag {
    const val Y_0_TOP = 1 shl 0
    const val MAP_PERSISTENT = 1 shl 1
    const val MAP_COHERENT = 1 shl 2
}
