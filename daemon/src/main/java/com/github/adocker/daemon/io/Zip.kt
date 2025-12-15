package com.github.adocker.daemon.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


/**
 * Extract a tar.gz file to destination directory
 */
suspend fun extractTarGz(
    inputStream: InputStream,
    destDir: File,
    onProgress: ((Long) -> Unit)? = null
): Result<Unit> = withContext(Dispatchers.IO) {
    if (!destDir.exists()) {
        destDir.mkdirs()
    }
    GzipCompressorInputStream(inputStream.buffered()).use { gzipIn ->
        extractTar(TarArchiveInputStream(gzipIn), destDir, onProgress)
    }
}

/**
 * Extract a plain tar file
 */
suspend fun extractTar(
    inputStream: InputStream,
    destDir: File,
    onProgress: ((Long) -> Unit)? = null
): Result<Unit> = withContext(Dispatchers.IO) {
    extractTar(
        TarArchiveInputStream(
            inputStream.buffered()
        ), destDir,
        onProgress
    )
}

/**
 * Common
 * Extract a plain tar file
 */
private fun extractTar(
    inputStream: TarArchiveInputStream,
    destDir: File,
    onProgress: ((Long) -> Unit)? = null
): Result<Unit> = runCatching {
    var bytesExtracted = 0L
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
                    val linkTarget = entry.linkName
                    outputFile.parentFile?.mkdirs()
                    createSymlink(outputFile, linkTarget)
                }

                entry.isLink -> {
                    // Handle hard links using Os.link
                    val linkTarget = File(destDir, entry.linkName)
                    outputFile.parentFile?.mkdirs()
                    createHardLink(outputFile, linkTarget)
                }

                else -> {
                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile).use { fos ->
                        tarIn.copyTo(fos)
                    }
                    outputFile.chmod(entry.mode)
                }
            }

            bytesExtracted += entry.size
            onProgress?.invoke(bytesExtracted)

            entry = tarIn.nextEntry
        }
    }
}