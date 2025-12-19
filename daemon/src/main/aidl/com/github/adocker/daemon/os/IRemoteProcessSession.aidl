package com.github.adocker.daemon.os;

import android.os.ParcelFileDescriptor;

interface IRemoteProcessSession {

    ParcelFileDescriptor getOutputStream() = 1;

    ParcelFileDescriptor getInputStream() = 2;

    ParcelFileDescriptor getErrorStream() = 3;

    int waitFor() = 4;

    int exitValue() = 5;

    void destroy() = 6;

    boolean isAlive() = 7;

    String toStringInternal() = 8;
}