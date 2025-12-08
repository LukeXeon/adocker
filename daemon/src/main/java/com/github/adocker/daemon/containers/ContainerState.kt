package com.github.adocker.daemon.containers

/**
 * Docker-compatible container states.
 *
 * Represents the full lifecycle of a container following Docker's state model.
 * These states match Docker's official container states for API compatibility.
 *
 * State transitions:
 * ```
 * Created → Running → Paused → Running
 *    ↓         ↓                   ↓
 * Exited ← Exited ← Restarting ← Exited
 *    ↓
 * Removing → (deleted)
 *    ↓
 *  Dead (error state)
 * ```
 *
 * @see <a href="https://docs.docker.com/engine/api/v1.43/#tag/Container">Docker API - Container States</a>
 */
enum class ContainerState {
    /**
     * Container has been created but never started.
     * - Created via `docker create` or similar
     * - Filesystem is ready, but no process is running
     * - Can be started with `docker start`
     */
    Created,

    /**
     * Container is currently running.
     * - Main process (PID 1) is active
     * - Started via `docker start` or `docker run`
     * - Can be paused, stopped, or restarted
     */
    Running,

    /**
     * Container processes are paused (frozen).
     * - Uses cgroups freezer to suspend all processes
     * - Process state remains in memory but doesn't consume CPU
     * - Can be unpaused to resume execution
     * - Triggered by `docker pause`
     */
    Paused,

    /**
     * Container is in the process of restarting.
     * - Transitional state, typically brief
     * - Triggered by restart policy (e.g., `--restart=always`)
     * - Will become Running or Exited shortly
     */
    Restarting,

    /**
     * Container is being removed.
     * - Transitional state during `docker rm`
     * - Resources are being cleaned up
     * - Container will be deleted soon
     */
    Removing,

    /**
     * Container has stopped running.
     * - Main process has terminated (normally or abnormally)
     * - Exit code and logs are preserved
     * - Can be restarted with `docker start`
     * - Triggered by `docker stop` or process completion
     *
     * Note: This is the official Docker state. There is no "Stopped" state.
     */
    Exited,

    /**
     * Container is in an unrecoverable error state.
     * - Resource cleanup failed after stopping
     * - Typically caused by:
     *   - Network namespace deletion failure
     *   - Mount point unmount failure (device busy)
     *   - cgroup cleanup failure
     *   - Storage driver errors
     * - Container cannot be started or removed normally
     * - Requires manual intervention or Docker daemon restart
     * - Rare in user-space implementations like ADocker
     */
    Dead
}