package com.adocker.runner.core.utils

import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import timber.log.Timber
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
     * Create a symbolic link using Android's Os.symlink API (API 21+)
     */
    private fun createSymlink(link: File, target: String) {
        try {
            android.util.Log.d("FileUtils", "Creating symlink: ${link.absolutePath} -> $target")

            // Delete existing file/link if it exists
            if (link.exists()) {
                link.delete()
                android.util.Log.d("FileUtils", "Deleted existing file before creating symlink")
            }

            // Use Android's Os.symlink (available since API 21)
            Os.symlink(target, link.absolutePath)
            android.util.Log.d("FileUtils", "✓ Symlink created successfully: ${link.name} -> $target")
        } catch (e: Exception) {
            android.util.Log.e("FileUtils", "✗ Failed to create symlink ${link.name} -> $target", e)
            // Don't throw - let extraction continue
        }
    }

    /**
     * Create a hard link using Android's Os.link API (API 21+)
     */
    private fun createHardLink(newPath: File, existingPath: File) {
        try {
            android.util.Log.d("FileUtils", "Creating hard link: ${newPath.absolutePath} -> ${existingPath.absolutePath}")

            // Delete existing file if it exists
            if (newPath.exists()) {
                newPath.delete()
                android.util.Log.d("FileUtils", "Deleted existing file before creating hard link")
            }

            if (!existingPath.exists()) {
                android.util.Log.w("FileUtils", "Hard link target doesn't exist: ${existingPath.absolutePath}, copying will be attempted later")
                // Fallback: copy the file when it becomes available
                return
            }

            // Use Android's Os.link (available since API 21)
            Os.link(existingPath.absolutePath, newPath.absolutePath)
            android.util.Log.d("FileUtils", "✓ Hard link created successfully: ${newPath.name} -> ${existingPath.name}")
        } catch (e: Exception) {
            android.util.Log.e("FileUtils", "✗ Failed to create hard link ${newPath.name} -> ${existingPath.name}", e)
            // Fallback: try copying the file
            try {
                if (existingPath.exists()) {
                    existingPath.copyTo(newPath, overwrite = true)
                    android.util.Log.d("FileUtils", "Copied file as fallback for hard link")
                }
            } catch (copyException: Exception) {
                android.util.Log.e("FileUtils", "Failed to copy file as fallback", copyException)
            }
        }
    }

    /**
     * Set file permissions from tar mode using Android's Os.chmod (API 21+)
     */
    private fun setFilePermissions(file: File, mode: Int) {
        try {
            // Use Android's Os.chmod directly with the mode from tar
            // The mode from tar is already in the correct format (octal)
            Os.chmod(file.absolutePath, mode)
        } catch (e: Exception) {
            android.util.Log.w("FileUtils", "Failed to set permissions for ${file.name}: mode=$mode", e)
            // Fallback: use Java File API (less precise)
            val ownerExecute = (mode and 0b001_000_000) != 0
            val ownerWrite = (mode and 0b010_000_000) != 0
            val ownerRead = (mode and 0b100_000_000) != 0
            file.setReadable(ownerRead, true)
            file.setWritable(ownerWrite, true)
            file.setExecutable(ownerExecute, true)
        }
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
