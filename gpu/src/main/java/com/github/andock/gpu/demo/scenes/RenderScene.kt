package com.github.andock.gpu.demo.scenes

import com.github.andock.gpu.demo.client.VirGLClient

/**
 * Render Scene Interface
 *
 * Defines lifecycle for a rendering scene that submits commands via VirGL client.
 */
interface RenderScene {
    /**
     * Human-readable name of the scene
     */
    val name: String

    /**
     * Description of what this scene demonstrates
     */
    val description: String

    /**
     * Initialize scene resources
     * Called once when scene is loaded.
     *
     * @param client VirGL client for communication
     * @param width Surface width in pixels
     * @param height Surface height in pixels
     */
    suspend fun init(client: VirGLClient, width: Int, height: Int): Result<Unit>

    /**
     * Render one frame
     * Called repeatedly at ~60 FPS.
     *
     * @param deltaTime Time since last frame in seconds
     */
    suspend fun render(deltaTime: Float): Result<Unit>

    /**
     * Handle surface size change
     *
     * @param width New width in pixels
     * @param height New height in pixels
     */
    suspend fun resize(width: Int, height: Int)

    /**
     * Cleanup scene resources
     * Called when scene is being destroyed.
     */
    suspend fun cleanup()
}
