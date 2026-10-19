package com.lucida.studio.domain.imaging

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.lucida.studio.ui.workspace.components.StudioCameraController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

/** Timed JPEG sequence recorder and snapshot manager. */
class TimelapseFrameRecorder(
    context: Context,
    private val controller: StudioCameraController
) {
    data class RecordingState(
        val isRecording: Boolean = false,
        val frames: Int = 0,
        val status: String = "Ready"
    )

    private val appContext = context.applicationContext
    private val frameRoot = File(appContext.filesDir, "lucida-frames")
    private val captureMutex = Mutex()
    private val recordingLock = Any()
    private val mutableState = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = mutableState.asStateFlow()
    @Volatile private var recordingJob: Job? = null

    /** Capture one clean still and save it to both private storage and Android Gallery (Pictures/Lucida). */
    suspend fun snapshot(): File = captureMutex.withLock {
        withContext(Dispatchers.IO) {
            val folder = File(frameRoot, "snapshots")
            check(folder.mkdirs() || folder.isDirectory) { "Could not create the app's private snapshots directory" }
            val filename = "lucida_snap_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val file = File(folder, filename)
            controller.capture(file)

            // Save copy directly to device's public Photos / Gallery in Pictures/Lucida
            runCatching {
                saveToGallery(file, filename)
            }
            file
        }
    }

    private fun saveToGallery(sourceFile: File, displayName: String) {
        val resolver = appContext.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lucida")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return
        resolver.openOutputStream(uri)?.use { out ->
            sourceFile.inputStream().buffered().use { input ->
                input.copyTo(out)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
    }

    /** Starts a fresh timestamped frame sequence; each frame is taken as a camera still. */
    fun start(scope: CoroutineScope, intervalSeconds: Long = 10L) {
        val interval = intervalSeconds.coerceIn(5L, 60L)
        synchronized(recordingLock) {
            if (recordingJob?.isActive == true) return
            val session = File(frameRoot, "sessions/session_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}")
            if (!session.mkdirs() && !session.isDirectory) {
                mutableState.value = mutableState.value.copy(isRecording = false, status = "Could not create the private sequence folder")
                return
            }
            mutableState.value = RecordingState(isRecording = true, frames = 0, status = "Recording one clean still every $interval seconds")
            lateinit var job: Job
            job = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
                var frameNumber = 0
                try {
                    while (coroutineContext.isActive) {
                        frameNumber++
                        val timestamp = System.currentTimeMillis()
                        val frame = File(session, "frame_${frameNumber.toString().padStart(6, '0')}_$timestamp.jpg")
                        captureMutex.withLock { controller.capture(frame) }
                        mutableState.value = mutableState.value.copy(frames = frameNumber)
                        delay(interval * 1_000L)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    mutableState.value = mutableState.value.copy(
                        isRecording = false,
                        status = "Sequence stopped after $frameNumber frame(s): ${error.message ?: "camera capture failed"}"
                    )
                } finally {
                    synchronized(recordingLock) {
                        if (recordingJob === job) recordingJob = null
                        if (mutableState.value.isRecording) {
                            mutableState.value = mutableState.value.copy(
                                isRecording = false,
                                status = "Sequence stopped · ${mutableState.value.frames} frame(s) saved"
                            )
                        }
                    }
                }
            }
            recordingJob = job
            job.start()
        }
    }

    suspend fun stop() {
        val job = synchronized(recordingLock) { recordingJob.also { recordingJob = null } }
        job?.cancelAndJoin()
        mutableState.value = mutableState.value.copy(
            isRecording = false,
            status = "Sequence stopped · ${mutableState.value.frames} frame(s) saved"
        )
    }

    /** Exports saved clean JPEGs with a relative path and UTC capture time in manifest.csv. */
    suspend fun exportSequence(output: OutputStream) = withContext(Dispatchers.IO) {
        check(!mutableState.value.isRecording) { "Stop the time-lapse before exporting" }
        val files = frameRoot.walkTopDown()
            .filter { it.isFile && it.extension.equals("jpg", ignoreCase = true) && it.length() > 0L }
            .sortedBy { it.relativeTo(frameRoot).invariantSeparatorsPath }
            .toList()
        check(files.isNotEmpty()) { "No clean frames have been captured yet" }
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            val manifestRows = ArrayList<String>(files.size)
            for (file in files) {
                val relative = file.relativeTo(frameRoot).invariantSeparatorsPath
                val entryPath = "frames/$relative"
                zip.putNextEntry(ZipEntry(entryPath))
                file.inputStream().buffered().use { input -> input.copyTo(zip) }
                zip.closeEntry()
                val timeUtc = utcFormatter().format(Date(file.lastModified()))
                manifestRows += "${csv(entryPath)},${csv(timeUtc)},${file.length()}"
            }
            zip.putNextEntry(ZipEntry("manifest.csv"))
            zip.write(("path,captured_at_utc,bytes\n" + manifestRows.joinToString("\n") + "\n").toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.finish()
        }
    }

    private fun utcFormatter() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"" + escaped + "\""
    }
}
