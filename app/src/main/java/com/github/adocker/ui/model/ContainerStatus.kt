package com.github.adocker.ui.model

/**
 * Container status in UI layer
 * - CREATED: Container exists but not running
 * - RUNNING: Container is currently running (determined by RunningContainer.isActive)
 * - EXITED: Container has exited (currently same as CREATED, reserved for future use)
 */
enum class ContainerStatus {
    CREATED,
    RUNNING,
    EXITED
}
