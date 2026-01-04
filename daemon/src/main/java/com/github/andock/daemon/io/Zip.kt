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

                    entry.isSymbolicLink || entry.isLink -> {
                        val newFile = File(destDir, entry.name)
                        newFile.parentFile?.mkdirs()
                        try {
                            val newPath = newFile.toPath()
                            // Remove existing file/link if present
                            if (!Files.isSymbolicLink(newPath)
                                || Files.readSymbolicLink(newPath).toString() != entry.linkName
                            ) {
                                Files.deleteIfExists(newFile.toPath())
                                Files.createSymbolicLink(newPath, Path(entry.linkName))
                                Timber.d("Created symlink: ${newFile.absolutePath} -> ${entry.linkName}")
                            }
                        } catch (e: Exception) {
                            Timber.w(
                                e,
                                "Failed to create symlink: ${newFile.absolutePath} -> ${entry.linkName}"
                            )
                            throw e
                        }
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
    }
}

