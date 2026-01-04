package com.github.andock.daemon.io

import android.system.ErrnoException
import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream


/**
 * Extract a tar.gz file to destination directory
 */
suspend fun extractTarGz(
    zipFile: File,
    destDir: File
): Result<Unit> = withContext(Dispatchers.IO) {
    if (!destDir.exists()) {
        destDir.mkdirs()
    }
    GzipCompressorInputStream(zipFile.inputStream().buffered()).use { gzipIn ->
        extractTar(TarArchiveInputStream(gzipIn), destDir)
    }
}

/**
 * Common
 * Extract a plain tar file
 */
private fun extractTar(
    inputStream: TarArchiveInputStream,
    destDir: File
): Result<Unit> = runCatching {
    inputStream.use { tarIn ->
        var entry: TarArchiveEntry? = tarIn.nextEntry
        while (entry != null) {
            val outputFile = File(destDir, entry.name)

            if (!outputFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                throw SecurityException("Path traversal detected: ${entry.name}")
            }

            when {
                entry.isDirectory -> {
                    outputFile.mkdirs()
                }

                entry.isSymbolicLink -> {
                    // Create symbolic link using Android Os API
                    outputFile.parentFile?.mkdirs()
                    val linkTarget = entry.linkName
                    try {
                        // Remove existing file/link if present
                        if (outputFile.exists()) {
                            outputFile.delete()
                        }
                        Os.symlink(linkTarget, outputFile.absolutePath)
                        Timber.d("Created symlink: ${outputFile.absolutePath} -> $linkTarget")
                    } catch (e: ErrnoException) {
                        Timber.w(e, "Failed to create symlink: ${outputFile.absolutePath} -> $linkTarget")
                        throw e
                    }
                }

                entry.isLink -> {
                    // Create hard link using Android Os API
                    // Fallback to symlink if hard link creation fails (SELinux restriction)
                    outputFile.parentFile?.mkdirs()
                    val linkTarget = File(destDir, entry.linkName)
                    try {
                        // Remove existing file/link if present
                        if (outputFile.exists()) {
                            outputFile.delete()
                        }
                        Os.link(linkTarget.absolutePath, outputFile.absolutePath)
                        Timber.d("Created hard link: ${outputFile.absolutePath} -> ${linkTarget.absolutePath}")
                    } catch (e: ErrnoException) {
                        // Hard link failed (likely SELinux restriction), fallback to symlink
                        Timber.w(e, "Hard link failed, falling back to symlink: ${outputFile.absolutePath} -> ${linkTarget.absolutePath}")
                        try {
                            Os.symlink(linkTarget.absolutePath, outputFile.absolutePath)
                            Timber.d("Created symlink as fallback: ${outputFile.absolutePath} -> ${linkTarget.absolutePath}")
                        } catch (e2: ErrnoException) {
                            Timber.e(e2, "Failed to create symlink fallback: ${outputFile.absolutePath} -> ${linkTarget.absolutePath}")
                            throw e2
                        }
                    }
                }

                else -> {
                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile).use { fos ->
                        tarIn.copyTo(fos)
                    }
                    outputFile.chmod(entry.mode)
                }
            }
            entry = tarIn.nextEntry
        }
    }
}