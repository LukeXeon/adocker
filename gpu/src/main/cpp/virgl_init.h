#ifndef VIRGL_INIT_H
#define VIRGL_INIT_H

#include "virgl_callbacks.h"

/**
 * Initialize VirGL Renderer
 *
 * This function:
 * 1. Initializes EGL Manager
 * 2. Creates shared base EGL context and surface
 * 3. Sets up virglrenderer callbacks
 * 4. Calls virgl_renderer_init() with appropriate flags
 *
 * @param server_ctx ServerContext to be initialized
 * @return true if successful
 */
bool initializeVirGL(ServerContext* server_ctx);

/**
 * Cleanup VirGL Renderer
 *
 * @param server_ctx ServerContext to cleanup
 */
void cleanupVirGL(ServerContext* server_ctx);

#endif // VIRGL_INIT_H
