package com.github.andock.gpu.virgl

/**
 * VirGL Pixel Formats - from virgl_hw.h (enum virgl_formats)
 */
object VirGLFormat {
    const val NONE = 0
    const val B8G8R8A8_UNORM = 1
    const val B8G8R8X8_UNORM = 2
    const val A8R8G8B8_UNORM = 3
    const val X8R8G8B8_UNORM = 4
    const val B5G5R5A1_UNORM = 5
    const val B4G4R4A4_UNORM = 6
    const val B5G6R5_UNORM = 7
    const val R10G10B10A2_UNORM = 8
    const val L8_UNORM = 9
    const val A8_UNORM = 10
    const val I8_UNORM = 11
    const val L8A8_UNORM = 12
    const val L16_UNORM = 13
    const val Z16_UNORM = 16
    const val Z32_UNORM = 17
    const val Z32_FLOAT = 18
    const val Z24_UNORM_S8_UINT = 19
    const val S8_UINT_Z24_UNORM = 20
    const val Z24X8_UNORM = 21
    const val X8Z24_UNORM = 22
    const val S8_UINT = 23
    const val R64_FLOAT = 24
    const val R64G64_FLOAT = 25
    const val R64G64B64_FLOAT = 26
    const val R64G64B64A64_FLOAT = 27
    const val R32_FLOAT = 28
    const val R32G32_FLOAT = 29
    const val R32G32B32_FLOAT = 30
    const val R32G32B32A32_FLOAT = 31
    const val R32_UNORM = 32
    const val R32G32_UNORM = 33
    const val R32G32B32_UNORM = 34
    const val R32G32B32A32_UNORM = 35
    const val R16_UNORM = 48
    const val R16G16_UNORM = 49
    const val R16G16B16_UNORM = 50
    const val R16G16B16A16_UNORM = 51
    const val R16_SNORM = 56
    const val R16G16_SNORM = 57
    const val R16G16B16_SNORM = 58
    const val R16G16B16A16_SNORM = 59
    const val R8_UNORM = 64
    const val R8G8_UNORM = 65
    const val R8G8B8_UNORM = 66
    const val R8G8B8A8_UNORM = 67
    const val X8B8G8R8_UNORM = 68
    const val R8_SNORM = 74
    const val R8G8_SNORM = 75
    const val R8G8B8_SNORM = 76
    const val R8G8B8A8_SNORM = 77
    const val R16_FLOAT = 91
    const val R16G16_FLOAT = 92
    const val R16G16B16_FLOAT = 93
    const val R16G16B16A16_FLOAT = 94
    const val R8G8B8_SRGB = 97
    const val R8G8B8A8_SRGB = 104

    // Compressed formats
    const val DXT1_RGB = 105
    const val DXT1_RGBA = 106
    const val DXT3_RGBA = 107
    const val DXT5_RGBA = 108
    const val DXT1_SRGB = 109
    const val DXT1_SRGBA = 110
    const val DXT3_SRGBA = 111
    const val DXT5_SRGBA = 112

    // RGTC compressed
    const val RGTC1_UNORM = 113
    const val RGTC1_SNORM = 114
    const val RGTC2_UNORM = 115
    const val RGTC2_SNORM = 116

    const val A8B8G8R8_UNORM = 121
    const val R11G11B10_FLOAT = 124
    const val R9G9B9E5_FLOAT = 125
    const val Z32_FLOAT_S8X24_UINT = 126
    const val B10G10R10A2_UNORM = 131
    const val R8G8B8X8_UNORM = 134

    // Integer formats
    const val R8_UINT = 177
    const val R8G8_UINT = 178
    const val R8G8B8_UINT = 179
    const val R8G8B8A8_UINT = 180
    const val R8_SINT = 181
    const val R8G8_SINT = 182
    const val R8G8B8_SINT = 183
    const val R8G8B8A8_SINT = 184
    const val R16_UINT = 185
    const val R16G16_UINT = 186
    const val R16G16B16_UINT = 187
    const val R16G16B16A16_UINT = 188
    const val R16_SINT = 189
    const val R16G16_SINT = 190
    const val R16G16B16_SINT = 191
    const val R16G16B16A16_SINT = 192
    const val R32_UINT = 193
    const val R32G32_UINT = 194
    const val R32G32B32_UINT = 195
    const val R32G32B32A32_UINT = 196
    const val R32_SINT = 197
    const val R32G32_SINT = 198
    const val R32G32B32_SINT = 199
    const val R32G32B32A32_SINT = 200

    const val ETC1_RGB8 = 226

    // BPTC compressed
    const val BPTC_RGBA_UNORM = 255
    const val BPTC_SRGBA = 256
    const val BPTC_RGB_FLOAT = 257
    const val BPTC_RGB_UFLOAT = 258

    // ETC2 compressed
    const val ETC2_RGB8 = 269
    const val ETC2_SRGB8 = 270
    const val ETC2_RGB8A1 = 271
    const val ETC2_SRGB8A1 = 272
    const val ETC2_RGBA8 = 273
    const val ETC2_SRGBA8 = 274
    const val ETC2_R11_UNORM = 275
    const val ETC2_R11_SNORM = 276
    const val ETC2_RG11_UNORM = 277
    const val ETC2_RG11_SNORM = 278

    // ASTC compressed (common sizes)
    const val ASTC_4x4 = 279
    const val ASTC_5x4 = 280
    const val ASTC_5x5 = 281
    const val ASTC_6x5 = 282
    const val ASTC_6x6 = 283
    const val ASTC_8x5 = 284
    const val ASTC_8x6 = 285
    const val ASTC_8x8 = 286
    const val ASTC_10x5 = 287
    const val ASTC_10x6 = 288
    const val ASTC_10x8 = 289
    const val ASTC_10x10 = 290
    const val ASTC_12x10 = 291
    const val ASTC_12x12 = 292
    const val ASTC_4x4_SRGB = 293
    const val ASTC_5x4_SRGB = 294
    const val ASTC_5x5_SRGB = 295
    const val ASTC_6x5_SRGB = 296
    const val ASTC_6x6_SRGB = 297
    const val ASTC_8x5_SRGB = 298
    const val ASTC_8x6_SRGB = 299
    const val ASTC_8x8_SRGB = 300
    const val ASTC_10x5_SRGB = 301
    const val ASTC_10x6_SRGB = 302
    const val ASTC_10x8_SRGB = 303
    const val ASTC_10x10_SRGB = 304
    const val ASTC_12x10_SRGB = 305
    const val ASTC_12x12_SRGB = 306

    const val R10G10B10X2_UNORM = 308

    const val MAX = 565  // VIRGL_FORMAT_MAX
}
