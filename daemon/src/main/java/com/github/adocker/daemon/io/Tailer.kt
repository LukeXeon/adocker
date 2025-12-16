package com.github.adocker.daemon.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.charset.Charset

/**
 * Kotlin 协程 + Flow 实现 Tailer 核心功能（tail -f 效果）
 * 对齐 Apache Commons IO Tailer 的核心特性：
 * - 轮询文件新增内容
 * - 从文件末尾开始监控（可配置）
 * - 自动适配文件轮转（截断/重建）
 * - 非阻塞（协程挂起，无额外线程池）
 */
fun File.tailAsFlow(
    pollingDelay: Long = 1000, // 对应 Tailer.setDelayMillis()
    fromEnd: Boolean = false,    // 对应 Tailer.setEnd()
    reOpen: Boolean = true,     // 对应 Tailer.setReOpen()
    charset: Charset = Charsets.UTF_8 // 对应 Tailer.setCharset()
) = flow {
    if (!exists() && !reOpen) {
        throw FileNotFoundException("file not found: $absolutePath")
    }

    var randomAccessFile: RandomAccessFile? = null
    var lastReadPos = if (fromEnd && exists()) length() else 0L

    try {
        while (currentCoroutineContext().isActive) {
            // 1. 处理文件不存在/重建（自动重连）
            if (!exists()) {
                randomAccessFile?.close()
                randomAccessFile = null
                delay(pollingDelay)
                continue
            }

            // 2. 初始化/重建文件连接
            if (randomAccessFile == null) {
                randomAccessFile = RandomAccessFile(this@tailAsFlow, "r")
                randomAccessFile.seek(lastReadPos)
            }

            // 3. 检测文件轮转（长度变小 → 截断/重建）
            val currentFileLength = length()
            if (currentFileLength < lastReadPos) {
                randomAccessFile.seek(0)
            }

            // 4. 读取新增内容（按行发射）
            val buffer = ByteArray(4096) // 对应 Tailer.setBufferSize()
            var bytesRead: Int
            val stringBuilder = StringBuilder()
            while (randomAccessFile.read(buffer).also { bytesRead = it } != -1) {
                stringBuilder.append(String(buffer, 0, bytesRead, charset))
                // 按换行分割，处理跨轮询的换行
                val lines = stringBuilder.split("\n")
                for (i in 0 until lines.size - 1) {
                    emit(lines[i])  // 保留原始内容，包括空行
                }
                // 保留最后一个不完整行（可能跨轮询）
                stringBuilder.clear().append(lines.last())
            }
            // 更新读取位置
            lastReadPos = randomAccessFile.filePointer

            // 5. 轮询休眠（协程挂起，非阻塞）
            delay(pollingDelay)
        }
    } finally {
        // 释放资源（对应 Tailer.stop()）
        randomAccessFile?.close()
    }
}.flowOn(Dispatchers.IO) // 确保文件操作在 IO 线程执行