package com.github.andock.daemon.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.io.path.Path


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
                if (!outputFile.absolutePath.startsWith(destDir.absolutePath)) {
                    throw SecurityException("Path traversal detected: ${entry.name}")
                }
                when {
                    entry.isDirectory -> {
                        outputFile.mkdirs()
                        Timber.d("Create directory: ${outputFile.absolutePath}")
                    }

                    entry.isSymbolicLink -> {
                        links.add(arrayOf(entry.linkName, entry.name))
                    }

                    entry.isLink -> {
                        links.add(arrayOf(entry.linkName, entry.name))
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
        for ((old, new) in links) {
            // Create symbolic link using Android Os API
            val newFile = File(destDir, new)
            newFile.mkdirs()
            try {
                val newPath = newFile.toPath()
                // Remove existing file/link if present
                if (Files.isSymbolicLink(newPath)
                    && Files.readSymbolicLink(newPath).toString() == old
                ) {
                    continue
                }
                Files.deleteIfExists(newFile.toPath())
                Files.createSymbolicLink(newPath, Path(old))
                Timber.d("Created symlink: ${newFile.absolutePath} -> $old")
            } catch (e: Exception) {
                Timber.w(
                    e,
                    "Failed to create symlink: ${newFile.absolutePath} -> $old"
                )
                throw e
            }
        }
    }
}

