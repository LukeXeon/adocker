package com.github.andock.gpu.demo.scenes

import com.github.andock.gpu.demo.client.VirGLClient
import com.github.andock.gpu.demo.client.VTestProtocol
import timber.log.Timber
import kotlin.math.cos
import kotlin.math.sin

/**
 * Triangle Scene
 *
 * Renders a rotating colored triangle using VirGL.
 * This is a simplified demo that demonstrates the basic vtest protocol flow.
 */
class TriangleScene : RenderScene {

    override val name = "Rotating Triangle"
    override val description = "Simple rotating triangle with vertex colors"

    private var client: VirGLClient? = null
    private var width = 0
    private var height = 0
    private var rotation = 0f

    // Resource IDs (allocated by server)
    private var framebufferResourceId: UInt = 0u
    private var vertexBufferResourceId: UInt = 0u

    override suspend fun init(client: VirGLClient, width: Int, height: Int): Result<Unit> {
        this.client = client
        this.width = width
        this.height = height

        return runCatching {
            Timber.i("Initializing TriangleScene ($width x $height)")

            // 1. Initialize context with VIRGL2 capset
            client.initContext(VTestProtocol.VIRGL_CAPSET_VIRGL2).getOrThrow()

            // 2. Create framebuffer resource (simplified - using blob)
            framebufferResourceId = client.createBlobResource(
                blobType = VTestProtocol.VIRGL_BLOB_TYPE_HOST3D,
                blobFlags = VTestProtocol.VIRGL_BLOB_FLAG_USE_MAPPABLE.toUInt(),
                size = (width * height * 4).toULong(), // RGBA8
                blobId = 1u
            ).getOrThrow()

            Timber.d("Created framebuffer resource: $framebufferResourceId")

            // 3. Create vertex buffer resource
            val vertexData = floatArrayOf(
                // Position (x, y)    Color (r, g, b)
                0.0f,  0.5f,         1.0f, 0.0f, 0.0f,  // Top (red)
                -0.5f, -0.5f,        0.0f, 1.0f, 0.0f,  // Bottom-left (green)
                0.5f, -0.5f,         0.0f, 0.0f, 1.0f   // Bottom-right (blue)
            )

            val vertexBytes = ByteArray(vertexData.size * 4).also { bytes ->
                val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.nativeOrder())
                vertexData.forEach { buffer.putFloat(it) }
            }

            vertexBufferResourceId = client.createBlobResource(
                blobType = VTestProtocol.VIRGL_BLOB_TYPE_GUEST,
                blobFlags = 0u,
                size = vertexBytes.size.toULong(),
                blobId = 2u
            ).getOrThrow()

            Timber.d("Created vertex buffer resource: $vertexBufferResourceId")

            // 4. Upload vertex data
            client.transferPut(
                resourceId = vertexBufferResourceId,
                level = 0u,
                x = 0u, y = 0u, z = 0u,
                width = vertexBytes.size.toUInt(),
                height = 1u,
                depth = 1u,
                data = vertexBytes
            ).getOrThrow()

            Timber.i("TriangleScene initialized successfully")
        }
    }

    override suspend fun render(deltaTime: Float): Result<Unit> {
        val cli = client ?: return Result.failure(IllegalStateException("Scene not initialized"))

        return runCatching {
            // Update rotation
            rotation += deltaTime * 45f // 45 degrees per second
            if (rotation > 360f) rotation -= 360f

            // Build command buffer (simplified virgl commands)
            val commands = buildCommandBuffer()

            // Submit commands
            cli.submitCommands(commands).getOrThrow()
        }
    }

    private fun buildCommandBuffer(): UIntArray {
        val cmds = mutableListOf<UInt>()

        // Command 1: Set viewport
        cmds.add(VTestProtocol.VIRGL_CCMD_SET_VIEWPORT_STATE)
        cmds.add(4u) // num dwords
        cmds.add(0u) // viewport index
        cmds.add(floatToUInt(width.toFloat() / 2)) // scale x
        cmds.add(floatToUInt(height.toFloat() / 2)) // scale y
        cmds.add(floatToUInt(0.5f)) // scale z

        // Command 2: Clear framebuffer
        cmds.add(VTestProtocol.VIRGL_CCMD_CLEAR)
        cmds.add(4u) // num dwords
        cmds.add(0x01u) // clear color buffer
        cmds.add(floatToUInt(0.1f)) // red
        cmds.add(floatToUInt(0.1f)) // green
        cmds.add(floatToUInt(0.1f)) // blue

        // Command 3: Draw triangle (simplified)
        cmds.add(VTestProtocol.VIRGL_CCMD_DRAW_VBO)
        cmds.add(3u) // num dwords
        cmds.add(0u) // start vertex
        cmds.add(3u) // vertex count
        cmds.add(1u) // instance count

        return cmds.toUIntArray()
    }

    override suspend fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
        Timber.d("TriangleScene resized: $width x $height")
    }

    override suspend fun cleanup() {
        client?.let { cli ->
            runCatching {
                if (vertexBufferResourceId != 0u) {
                    cli.unrefResource(vertexBufferResourceId)
                }
                if (framebufferResourceId != 0u) {
                    cli.unrefResource(framebufferResourceId)
                }
                Timber.i("TriangleScene cleaned up")
            }
        }
        client = null
    }

    private fun floatToUInt(value: Float): UInt {
        return java.nio.ByteBuffer.allocate(4)
            .order(java.nio.ByteOrder.nativeOrder())
            .putFloat(value)
            .getInt(0)
            .toUInt()
    }
}
