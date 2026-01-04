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
    runCatching {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val links = ArrayList<Array<String>>()
        TarArchiveInputStream(
            GzipCompressorInputStream(
                zipFile.inputStream().buffered()
            )
        ).use { tarIn ->
            var entry: TarArchiveEntry? = tarIn.nextEntry
            while (entry != null) {
                val outputFile = File(destDir, entry.name)
                if (!outputFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                    throw SecurityException("Path traversal detected: ${entry.name}")
                }
                when {
                    entry.isDirectory -> {
                        outputFile.mkdirs()
                        Timber.d("Create directory: ${outputFile.absolutePath}")
                    }

                    entry.isSymbolicLink -> {
                        links.add(arrayOf("s", entry.linkName, entry.name))
                    }

                    entry.isLink -> {
                        links.add(arrayOf("h", entry.linkName, entry.name))
                    }

                    entry.isFile -> {
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { fos ->
                            tarIn.copyTo(fos)
                        }
                        outputFile.chmod(entry.mode)
                        Timber.d("Copy file: ${outputFile.absolutePath}, mode: ${entry.mode}")
                    }
                }
                entry = tarIn.nextEntry
            }
        }
        for ((type, old, new) in links) {
            when (type) {
                "s" -> {
                    // Create symbolic link using Android Os API
                    val newFile = File(destDir, new)
                    val oldFile = File(destDir, old)
                    newFile.mkdirs()
                    try {
                        // Remove existing file/link if present
                        if (newFile.exists()) {
                            newFile.delete()
                        }
                        Os.symlink(oldFile.absolutePath, newFile.absolutePath)
                        Timber.d("Created symlink: ${newFile.absolutePath} -> ${oldFile.absolutePath}")
                    } catch (e: ErrnoException) {
                        Timber.w(
                            e,
                            "Failed to create symlink: ${newFile.absolutePath} -> ${oldFile.absolutePath}"
                        )
                        throw e
                    }
                }

                "h" -> {
                    val newFile = File(destDir, new)
                    val oldFile = File(destDir, old)
                    newFile.mkdirs()
                    try {
                        // Remove existing file/link if present
                        if (newFile.exists()) {
                            newFile.delete()
                        }
                        Os.link(oldFile.absolutePath, newFile.absolutePath)
                        Timber.d("Created hardlink: ${newFile.absolutePath} -> ${oldFile.absolutePath}")
                    } catch (e: ErrnoException) {
                        Timber.w(
                            e,
                            "Hard link failed, falling back to symlink: ${newFile.absolutePath} -> ${oldFile.absolutePath}"
                        )
                        try {
                            Os.symlink(oldFile.absolutePath, newFile.absolutePath)
                            Timber.d("Created symlink as fallback: ${newFile.absolutePath} -> ${oldFile.absolutePath}")
                        } catch (e: ErrnoException) {
                            Timber.e(
                                e,
                                "Failed to create symlink fallback: ${newFile.absolutePath} -> ${oldFile.absolutePath}"
                            )
                            throw e
                        }
                    }
                }
            }
        }
    }
}
