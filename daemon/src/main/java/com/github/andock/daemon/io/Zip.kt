package com.github.andock.daemon.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths


/**
 * Extract a tar.gz file to destination directory
 */
suspend fun extractTarGz(
    tarGzPath: String,
    outputDir: String
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val outputDirPath = Paths.get(outputDir)
            .toAbsolutePath()
            .normalize()
        Files.createDirectories(outputDirPath)
        FileInputStream(tarGzPath).use { fis ->
            BufferedInputStream(fis).use { bis ->
                GzipCompressorInputStream(bis).use { gzis ->
                    TarArchiveInputStream(gzis).use { tais ->
                        var entry: TarArchiveEntry? = tais.nextEntry
                        while (entry != null) {
                            val entryPath = outputDirPath.resolve(entry.name).normalize()
                            when {
                                entry.isDirectory -> {
                                    Files.createDirectories(entryPath)
                                    Timber.i("Create Directories: $entryPath")
                                }

                                entry.isFile -> {
                                    Files.createDirectories(entryPath.parent)
                                    Files.newOutputStream(entryPath).use { os ->
                                        tais.copyTo(os)
                                    }
                                    entryPath.toFile().chmod(entry.mode)

                                    Timber.i("Copy file: $entryPath mode: ${entry.mode}")
                                }

                                entry.isSymbolicLink -> {
                                    // 本身就是软链接，直接创建
                                    val linkTarget = Paths.get(entry.linkName).normalize()
                                    Files.createDirectories(entryPath.parent)
                                    Files.createSymbolicLink(entryPath, linkTarget)
                                    Timber.i("Created symbolic link: $entryPath -> $linkTarget")
                                }

                                entry.isLink -> {
                                    val linkName = entry.linkName
                                    val linkTarget = outputDirPath.resolve(linkName).normalize()
                                    Files.createDirectories(entryPath.parent)
                                    try {
                                        // 尝试创建硬链接
                                        Files.createLink(entryPath, linkTarget)
                                        Timber.i("Created hard link: $entryPath -> $linkTarget")
                                    } catch (e: UnsupportedOperationException) {
                                        // 硬链接不支持，尝试软链接
                                        Files.createSymbolicLink(entryPath, linkTarget)
                                        Timber.e(
                                            e,
                                            "Created symbolic link (fallback): $entryPath -> $linkTarget"
                                        )
                                    } catch (e: SecurityException) {
                                        // 权限不足，尝试软链接
                                        Files.createSymbolicLink(entryPath, linkTarget)
                                        Timber.e(
                                            e,
                                            "Created symbolic link (fallback): $entryPath -> $linkTarget"
                                        )
                                    }
                                }
                            }

                            entry = tais.nextEntry
                        }
                    }
                }
            }
        }
    }
}
