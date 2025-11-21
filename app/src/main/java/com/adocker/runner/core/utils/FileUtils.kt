package com.adocker.runner.core.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

object FileUtils {

    /**
     * Extract a tar.gz file to destination directory
     */
    suspend fun extractTarGz(
        inputStream: InputStream,
        destDir: File,
        onProgress: ((Long) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            var bytesExtracted = 0L

            GzipCompressorInputStream(inputStream.buffered()).use { gzipIn ->
                TarArchiveInputStream(gzipIn).use { tarIn ->
                    var entry: TarArchiveEntry? = tarIn.nextEntry
                    while (entry != null) {
                        val outputFile = File(destDir, entry.name)

                        // Security check: prevent path traversal
                        if (!outputFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                            throw SecurityException("Path traversal detected: ${entry.name}")
                        }

                        when {
                            entry.isDirectory -> {
                                outputFile.mkdirs()
                            }
                            entry.isSymbolicLink -> {
                                // Handle symbolic links
                                val linkTarget = entry.linkName
                                outputFile.parentFile?.mkdirs()
                                createSymlink(outputFile, linkTarget)
                            }
                            entry.isLink -> {
                                // Handle hard links
                                val linkTarget = File(destDir, entry.linkName)
                                outputFile.parentFile?.mkdirs()
                                linkTarget.copyTo(outputFile, overwrite = true)
                            }
                            else -> {
                                outputFile.parentFile?.mkdirs()
                                FileOutputStream(outputFile).use { fos ->
                                    tarIn.copyTo(fos)
                                }
                                // Preserve file permissions
                                setFilePermissions(outputFile, entry.mode)
                            }
                        }

                        bytesExtracted += entry.size
                        onProgress?.invoke(bytesExtracted)

                        entry = tarIn.nextEntry
                    }
                }
            }
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
        runCatching {
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            var bytesExtracted = 0L

            TarArchiveInputStream(inputStream.buffered()).use { tarIn ->
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
                            val linkTarget = File(destDir, entry.linkName)
                            outputFile.parentFile?.mkdirs()
                            if (linkTarget.exists()) {
                                linkTarget.copyTo(outputFile, overwrite = true)
                            }
                        }
                        else -> {
                            outputFile.parentFile?.mkdirs()
                            FileOutputStream(outputFile).use { fos ->
                                tarIn.copyTo(fos)
                            }
                            setFilePermissions(outputFile, entry.mode)
                        }
                    }

                    bytesExtracted += entry.size
                    onProgress?.invoke(bytesExtracted)

                    entry = tarIn.nextEntry
                }
            }
        }
    }

    /**
     * Create a symbolic link (requires API 26+, falls back to copy on older versions)
     */
    private fun createSymlink(link: File, target: String) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.nio.file.Files.createSymbolicLink(
                    link.toPath(),
                    java.nio.file.Paths.get(target)
                )
            } else {
                // Fallback: create a file with link info
                link.writeText("SYMLINK:$target")
            }
        } catch (e: Exception) {
            // Ignore symlink creation failures
        }
    }

    /**
     * Set file permissions from tar mode
     */
    private fun setFilePermissions(file: File, mode: Int) {
        val ownerExecute = (mode and 0b001_000_000) != 0
        val ownerWrite = (mode and 0b010_000_000) != 0
        val ownerRead = (mode and 0b100_000_000) != 0

        file.setReadable(ownerRead, true)
        file.setWritable(ownerWrite, true)
        file.setExecutable(ownerExecute, true)
    }

    /**
     * Calculate SHA256 hash of a file
     */
    suspend fun sha256(file: File): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculate SHA256 hash from input stream
     */
    suspend fun sha256(inputStream: InputStream): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream.use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Delete directory recursively
     */
    suspend fun deleteRecursively(file: File): Boolean = withContext(Dispatchers.IO) {
        file.deleteRecursively()
    }

    /**
     * Get directory size
     */
    suspend fun getDirectorySize(dir: File): Long = withContext(Dispatchers.IO) {
        if (!dir.exists()) return@withContext 0L
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * Copy directory recursively
     */
    suspend fun copyDirectory(src: File, dest: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            src.copyRecursively(dest, overwrite = true)
            Unit
        }
    }

    /**
     * Make file executable
     */
    fun makeExecutable(file: File): Boolean {
        return file.setExecutable(true, false)
    }

    /**
     * Format file size for display
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }
}
