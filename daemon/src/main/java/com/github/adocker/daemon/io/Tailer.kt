package com.github.adocker.daemon.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter
import java.io.File
import java.time.Duration

/**
 * Tails a file and emits each new line as a Flow element.
 *
 * This function creates a [Tailer] that monitors the file for changes and emits
 * new lines as they are written. The tailer runs in a background thread and is
 * automatically cleaned up when the flow is cancelled.
 *
 * **Backpressure Handling:**
 * - Lines are sent using suspending [send()] which blocks the Tailer thread when
 *   the channel buffer is full
 * - This prevents data loss by applying backpressure to the file reading
 * - The default channel buffer size is 64 elements
 *
 * **Performance Considerations:**
 * - If the consumer is slow, the Tailer thread will be blocked, which naturally
 *   rate-limits the file reading
 * - For high-throughput scenarios with a slow consumer, consider using downstream
 *   operators like [kotlinx.coroutines.flow.buffer] or [kotlinx.coroutines.flow.conflate]
 *
 * @param delayMillis Delay between checks of the file for new content in milliseconds.
 *                    Default is 100ms. Smaller values provide more real-time updates
 *                    but consume more CPU.
 * @param startFromEnd If true, only new lines added after tailing starts are emitted.
 *                     If false, existing content is emitted first. Default is false.
 * @param reOpen If true, the tailer will attempt to reopen the file if it is deleted
 *               or rotated. Default is false.
 * @param bufferSize Size of the internal buffer used by Tailer to read the file.
 *                   Default is 8192 bytes. Larger buffers can improve performance
 *                   for files with high write rates.
 *
 * @return A cold Flow that emits each line from the file as a String.
 *         The flow will continue emitting lines until cancelled or an error occurs.
 *         Applies backpressure when consumer is slow.
 *
 * @throws IllegalArgumentException if the file does not exist when tailing starts
 *
 * Example usage:
 * ```kotlin
 * val logFile = File("/path/to/log.txt")
 * logFile.tailAsFlow(delayMillis = 100, startFromEnd = true)
 *     .collect { line ->
 *         println("New line: $line")
 *     }
 * ```
 */
fun File.tailAsFlow(
    delayMillis: Long = 1000,
    startFromEnd: Boolean = false,
    reOpen: Boolean = false,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
) = callbackFlow {
    val tailer = Tailer.builder()
        .setFile(this@tailAsFlow)
        .setTailerListener(object : TailerListenerAdapter() {
            override fun handle(line: String) {
                // Use runBlocking to call the suspending send() function
                // This blocks the tailer thread when channel is full, providing backpressure
                runBlocking { send(line) }
            }

            override fun handle(ex: Exception) {
                // Close the flow with an exception
                close(ex)
            }

            override fun fileNotFound() {
                // Close the flow if file is not found and reOpen is false
                if (!reOpen) {
                    close(IllegalArgumentException("File not found: ${this@tailAsFlow.absolutePath}"))
                }
            }
        })
        .setDelayDuration(Duration.ofMillis(delayMillis))
        .setStartThread(false) // Don't auto-start, we'll start it manually in a coroutine
        .setTailFromEnd(startFromEnd)
        .setReOpen(reOpen)
        .setBufferSize(bufferSize) // Use the bufferSize parameter
        .get()
    // Launch the tailer in a dedicated coroutine with its own dispatcher
    launch(Dispatchers.IO) {
        runInterruptible {
            tailer.run()
        }
    }
    // When the flow is cancelled, stop the tailer
    awaitClose {
        tailer.close()
    }
}