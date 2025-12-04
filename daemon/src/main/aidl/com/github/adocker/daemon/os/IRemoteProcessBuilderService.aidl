package com.github.adocker.daemon.os;
import com.github.adocker.daemon.os.IRemoteProcessSession;

/**
 * AIDL interface for executing shell commands with Shizuku privileges
 */
interface IRemoteProcessBuilderService {

    IRemoteProcessSession newProcess(in String[] cmd, in String[] env, String dir) = 1;

    /**
     * Destroy the service (called by Shizuku)
     * Transaction code must be 16777114 for Shizuku to call it
     */
    void destroy() = 16777114;
}
