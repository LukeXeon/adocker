package com.github.adocker.daemon.io

import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


/**
 * Set file permissions from tar mode using Android's Os.chmod (API 21+)
 */
fun File.chmod(mode: Int) {
    try {
        // Use Android's Os.chmod directly with the mode from tar
        // The mode from tar is already in the correct format (octal)
        Os.chmod(absolutePath, mode)
    } catch (e: Exception) {
        Timber.w(e, "Failed to set permissions for ${name}: mode=$mode")
    }
}

/**
 * Create a symbolic link using Android's Os.symlink API (API 21+)
 */
fun createSymlink(link: File, target: String) {
    try {
        Timber.d("Creating symlink: ${link.absolutePath} -> $target")

        // Delete existing file/link if it exists
        if (link.exists()) {
            link.delete()
            Timber.d("Deleted existing file before creating symlink")
        }

        // Use Android's Os.symlink (available since API 21)
        Os.symlink(target, link.absolutePath)
        Timber.d("✓ Symlink created successfully: ${link.name} -> $target")
    } catch (e: Exception) {
        Timber.e(e, "✗ Failed to create symlink ${link.name} -> $target")
        // Don't throw - let extraction continue
    }
}

/**
 * Create a hard link using Android's Os.link API (API 21+)
 */
fun createHardLink(newPath: File, existingPath: File) {
    try {
        Timber.d("Creating hard link: ${newPath.absolutePath} -> ${existingPath.absolutePath}")

        // Delete existing file if it exists
        if (newPath.exists()) {
            newPath.delete()
            Timber.d("Deleted existing file before creating hard link")
        }

        if (!existingPath.exists()) {
            Timber.w("Hard link target doesn't exist: ${existingPath.absolutePath}, copying will be attempted later")
            // Fallback: copy the file when it becomes available
            return
        }

        // Use Android's Os.link (available since API 21)
        Os.link(existingPath.absolutePath, newPath.absolutePath)
        Timber.d("✓ Hard link created successfully: ${newPath.name} -> ${existingPath.name}")
    } catch (e: Exception) {
        Timber.e(e, "✗ Failed to create hard link ${newPath.name} -> ${existingPath.name}")
    }
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
 * Format file size for display
 */
fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    }
}