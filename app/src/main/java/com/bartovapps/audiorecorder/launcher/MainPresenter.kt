package com.bartovapps.audiorecorder.launcher

import android.icu.lang.UCharacter.GraphemeClusterBreak.L
import android.media.*
import android.util.Log
import kotlin.experimental.and
import android.media.AudioTrack.MODE_STREAM
import com.naman14.androidlame.AndroidLame
import com.naman14.androidlame.LameBuilder
import java.io.*
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import io.kvh.media.amr.AmrEncoder
import java.text.DecimalFormat
import java.util.*


/**
 * Created by motibartov on 08/10/2017.
 */

class MainPresenter (val mainView: MainContract.View): MainContract.Presenter  {


    companion object {
        val TAG = MainPresenter::class.java.simpleName
        private val LOW_SAMPLERATE = 8000
        private val HIGH_SAMPLERATE = 44100
        private val bytesPerSecond = (HIGH_SAMPLERATE * 2).toLong()  //This can be used for skip function in player

        private val RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO
        private val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

        private val mp3FilePath = "/sdcard/testRecord.mp3"
        val pcmFilePath = "/sdcard/8k16bitMono.pcm"
        val wavFilePath = "/sdcard/testRecord.wav"
        val amrFilePath = "/sdcard/testRecord.amr"
    }
    val mMediaPlayer = MediaPlayer()
    init {
        mMediaPlayer.setOnCompletionListener {
            mainView.updateProgress(mMediaPlayer.currentPosition)
            mTimer?.cancel()
            mainView.showRecordingStopped()
        }
    }
    lateinit var mTimerTask: TimerTask
    var mTimer : Timer? = null

    lateinit var recorder: AudioRecord
    var recordingThread: Thread? = null
    var isRecording = false

    var BufferElements2Rec = 1024 // want to play 2048 (2K) since 2 bytes we use only 1024
    var BytesPerElement = 2 // 2 bytes in 16bit format
    var readBufferSize = 0
    lateinit var androidLame: AndroidLame
    lateinit var playerFile : String

    override fun subscribe() {
    }

    override fun unSubscribe() {
    }

    override fun startRecordClicked() {
        startRecording(recordingFun)
    }


    override fun stopRecordClicked() {
        stopRecording()
    }

    override fun startPlayClicked() {
        mMediaPlayer.start()
        initializeTimerTask()
    }

    override fun stopPlaybackClicked() {
        mMediaPlayer.pause()
    }

    fun prepareAudioRecorder(sampleRate: Int, channels: Int, encoding: Int, bufferSize: Int = 160) {

//        val writeBufferSize = AudioTrack.getMinBufferSize(HIGH_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING)
        Log.i(TAG, "prepareAudioRecorder: readBufferSize = $readBufferSize")

        recorder = AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channels,
                encoding, bufferSize)
    }

    override fun onAmrRdChecked() {
        prepareAudioRecorder(LOW_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, 160)
        recordingFun = writeAmr
        getFileInfo(amrFilePath)
        prepareMediaPlayer(amrFilePath)
    }

    override fun onMp3RdChecked() {
        readBufferSize = AudioRecord.getMinBufferSize(HIGH_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING)
        prepareAudioRecorder(HIGH_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, readBufferSize * 2)
        recordingFun = writeMp3
        getFileInfo(mp3FilePath)
        prepareMediaPlayer(mp3FilePath)
    }

    override fun onWavRdChecked() {
        readBufferSize = AudioRecord.getMinBufferSize(HIGH_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING)
        prepareAudioRecorder(HIGH_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, readBufferSize * 2)
        recordingFun = writeWav
        getFileInfo(wavFilePath)
        prepareMediaPlayer(wavFilePath)
    }


    private fun startRecording(writeToFile: () -> Unit) {

        recorder.startRecording()
        isRecording = true

        recordingThread = Thread(Runnable {
            //            writeAudioDataToFile()
            writeToFile()
        }, "Recording Thread")
        recordingThread?.start()
        Log.i(TAG, "Audio Recording started")
    }


    private fun writeAudioDataToFile() {
        // Write the output audio in byte

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(pcmFilePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
//        val buffer = ShortArray(readBufferSize * 2 * 5)
        val buffer = ShortArray(readBufferSize)
        val mp3buffer = ByteArray((7200 + buffer.size.toDouble() * 2.0 * 1.25).toInt())

        while (isRecording) {
            // gets the voice output from microphone to byte format

            val read = recorder.read(buffer, 0, readBufferSize)
            Log.i(TAG, "Got $read bytes")
//            Log.i(TAG, Arrays.deepToString(buffer.toTypedArray()))

            if (read > 0) {
//                val bytesEncoded = androidLame.encode(buffer, buffer, read, mp3buffer)
//                Log.i(TAG,"Bytes Encoded: $bytesEncoded")
                try {
                    // writes the data to file from buffer stores the voice buffer
                    fos?.write(short2byte(buffer))
//                    fos?.write(mp3buffer, 0, bytesEncoded)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fos?.close()
//        val outputMp3buf = androidLame.flush(mp3buffer)

//        if (outputMp3buf > 0) {
//            try {
//                Log.i(TAG, "writing final mp3buffer to outputstream")
//                fos?.write(mp3buffer, 0, outputMp3buf)
//                fos?.close()
//                Log.i(TAG, "Recording loop stopped")
//
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }

    }

    private fun stopRecording() {
        // stops the recording activity

        isRecording = false

        recorder.stop()
//        recorder.release()

        recordingThread = null

        Log.i(TAG, "Audio Recording stopped")
    }

    fun short2byte(sData: ShortArray): ByteArray {
        val shortArrsize = sData.size
        val bytes = ByteArray(shortArrsize * 2)

        for (i in 0 until shortArrsize) {
            bytes[i * 2] = (sData[i] and 0x00FF).toByte()
            //This may cause an error as kotlin doesn't support shr on Short, therefore cast to int
            bytes[i * 2 + 1] = ((sData[i].toInt() and 0x0000FFFF) shr 8).toByte()
            sData[i] = 0
        }
        return bytes
    }


    val writeMp3 = {
        val buffer = ShortArray(readBufferSize * 2 * 5)
        val mp3buffer = ByteArray((7200 + buffer.size.toDouble() * 2.0 * 1.25).toInt())


        val fos = FileOutputStream(mp3FilePath)
        androidLame = LameBuilder()
                .setInSampleRate(HIGH_SAMPLERATE)
                .setOutChannels(1)
                .setOutBitrate(32)
                .setOutSampleRate(HIGH_SAMPLERATE)
                .build()

        while (isRecording) {
            // gets the voice output from microphone to byte format

            val read = recorder.read(buffer, 0, readBufferSize)
            Log.i(TAG, "Got $read bytes")
//            Log.i(TAG, Arrays.deepToString(buffer.toTypedArray()))

            if (read > 0) {
                val bytesEncoded = androidLame.encode(buffer, buffer, read, mp3buffer)
//                Log.i(TAG,"Bytes Encoded: $bytesEncoded")
                fos.write(mp3buffer, 0, bytesEncoded)
                try {
                    // writes the data to file from buffer stores the voice buffer
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        val outputMp3buf = androidLame.flush(mp3buffer)

        if (outputMp3buf > 0) {
            try {
                Log.i(TAG, "writing final mp3buffer to outputstream")
                fos.write(mp3buffer, 0, outputMp3buf)
                fos.close()
                Log.i(TAG, "Recording loop stopped")

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    val writeAmr = {
        var read: Int = 0

        val fos = FileOutputStream(amrFilePath)
        AmrEncoder.init(0)

        val wavBuffer = ShortArray(160)//short array read from AudioRecorder, length 160
        val amrBuffer = ByteArray(32)//output amr frame, length 32

        fos.write(byteArrayOf(
                // RIFF header
                '#'.toByte(), '!'.toByte(), 'A'.toByte(),
                'M'.toByte(), 'R'.toByte(), '\n'.toByte())
        )

        while (isRecording) {
            var totalRead = 0
            while (totalRead < 160) {//although unlikely to be necessary, buffer the mic input
                read = recorder.read(wavBuffer, totalRead, 160 - totalRead)
                totalRead += read
            }
            AmrEncoder.encode(AmrEncoder.Mode.MR122.ordinal, wavBuffer, amrBuffer)
//            Log.i(TAG, "writeAmr: $byteEncoded bytes encoded" )
            Log.i(TAG, Arrays.deepToString(amrBuffer.toTypedArray()))
            fos.write(amrBuffer)
        }
        AmrEncoder.exit()

        fos.flush()
        fos.close()
    }


    val writeWav: () -> Unit = {

        val wavBuffer = ByteArray(readBufferSize * 2)
        var read: Int = 0
        var total: Int = 0
        var running: Boolean = true


        var startTime: Long = 0
        var endTime: Long = 0

        val fos = FileOutputStream(wavFilePath)

        try {

            startTime = SystemClock.elapsedRealtime()
            Log.i(TAG, "writeWav: writing wav header...")
            writeWavHeader(fos, RECORDER_CHANNELS, HIGH_SAMPLERATE, RECORDER_AUDIO_ENCODING)

            Log.i(TAG, "writeWav: Starting wav recording...")
            while (running && isRecording) {
                read = recorder.read(wavBuffer, 0, wavBuffer.size)

                // WAVs cannot be > 4 GB due to the use of 32 bit unsigned integers.
                if (total + read > 4294967295L) {
                    // Write as many bytes as we can before hitting the max size
                    var i = 0
                    while (i < read && total <= 4294967295L) {
                        fos.write(wavBuffer[i].toInt())
                        i++
                        total++
                    }
                    running = false
                } else {
                    // Write out the entire read buffer
                    fos.write(wavBuffer, 0, read)
                    total += read
                }
            }
        } catch (ex: IOException) {
            Log.e(TAG, "writeWav: ", ex)
        } finally {
            try {
                if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder.stop()
                    endTime = SystemClock.elapsedRealtime()
                }
            } catch (ex: IllegalStateException) {
                Log.e(TAG, "writeWav: ", ex)
            }
            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                recorder.release()
            }
            try {
                fos.close()
            } catch (ex: IOException) {
                Log.e(TAG, "writeWav: ", ex)
            }
        }

        try {
            updateWavHeader(File(wavFilePath))
        } catch (ex: IOException) {
            Log.e(TAG, "writeWav: ", ex)
        }

        Log.i(TAG, "writeWav: recording done: ${endTime - startTime} sec' recorded")
    }

    var recordingFun = writeAmr

    fun writeWavHeader(out: OutputStream, channelMask: Int, sampleRate: Int, encoding: Int) {
        var channels: Short = 1
        when (channelMask) {
            AudioFormat.CHANNEL_OUT_MONO -> channels = 1
            AudioFormat.CHANNEL_IN_STEREO -> channels = 2
        }

        var bitDepth: Short = 8
        when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> bitDepth = 8
            AudioFormat.ENCODING_PCM_16BIT -> bitDepth = 16
            AudioFormat.ENCODING_PCM_FLOAT -> bitDepth = 32
        }

        val littleBytes = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels * (bitDepth / 8))
                .putShort((channels * (bitDepth / 8)).toShort())
                .putShort(bitDepth)
                .array()

        // Not necessarily the best, but it's very easy to visualize this way
        out.write(byteArrayOf(
                // RIFF header
                'R'.toByte(), 'I'.toByte(), 'F'.toByte(), 'F'.toByte(), // ChunkID
                0, 0, 0, 0, // ChunkSize (must be updated later)
                'W'.toByte(), 'A'.toByte(), 'V'.toByte(), 'E'.toByte(), // Format
                // fmt subchunk
                'f'.toByte(), 'm'.toByte(), 't'.toByte(), ' '.toByte(), // Subchunk1ID
                16, 0, 0, 0, // Subchunk1Size
                1, 0, // AudioFormat
                littleBytes[0], littleBytes[1], // NumChannels
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                littleBytes[10], littleBytes[11], // BlockAlign
                littleBytes[12], littleBytes[13], // BitsPerSample
                // data subchunk
                'd'.toByte(), 'a'.toByte(), 't'.toByte(), 'a'.toByte(), // Subchunk2ID
                0, 0, 0, 0)// Subchunk2Size (must be updated later)
        )
    }

    @Throws(IOException::class)
    private fun updateWavHeader(wav: File) {
        val sizes = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                // There are probably a bunch of different/better ways to calculate
                // these two given your circumstances. Cast should be safe since if the WAV is
                // > 4 GB we've already made a terrible mistake.
                .putInt((wav.length() - 8).toInt()) // ChunkSize
                .putInt((wav.length() - 44).toInt()) // Subchunk2Size
                .array()

        var accessWave: RandomAccessFile? = null

        try {
            accessWave = RandomAccessFile(wav, "rw")
            // ChunkSize
            accessWave.seek(4)
            accessWave.write(sizes, 0, 4)

            // Subchunk2Size
            accessWave.seek(40)
            accessWave.write(sizes, 4, 4)
        } catch (ex: IOException) {
            // Rethrow but we still close accessWave in our finally
            throw ex
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close()
                } catch (ex: IOException) {
                    //
                }

            }
        }
    }

    fun getFileInfo(path: String){

        val units = arrayOf("B", "kB", "MB", "GB", "TB")


        val file = File(path)
        val fileSize: Long = file.length()

        var inputStream: FileInputStream? = null
        var mmr: MediaMetadataRetriever? = null

        val digitGroups = (Math.log10(fileSize.toDouble()) / Math.log10(1024.0)).toInt()


        try {
            inputStream = FileInputStream(path)

            mmr = MediaMetadataRetriever()
            mmr.setDataSource(inputStream.fd)
            val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            mainView.showAudioDuration(java.lang.Long.parseLong(duration))
            mainView.showFileInfo(path, DecimalFormat("#,##0.#").format(fileSize / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups])
        } catch (e: IOException) {
            Log.e(TAG, "getFileInfo: ", e)
        } catch (e: RuntimeException) {
            Log.e(TAG, "getFileInfo: ", e)
        } catch (e: FileNotFoundException){
            Log.e(TAG, "getFileInfo", e)
        }
        finally {
            mmr?.release()
            inputStream?.close()
        }
   }

    fun prepareMediaPlayer( filePath: String){


        var fis : FileInputStream? = null

        try {
           fis = FileInputStream(filePath)
            mMediaPlayer.reset()
            mMediaPlayer.setDataSource(fis.fd)
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mMediaPlayer.prepareAsync()
        }catch (e: FileNotFoundException){
            Log.e(TAG, "prepareMediaPlayer")
        }finally {
            fis?.close()
        }
    }

    fun initializeTimerTask() {

        mTimerTask = object : TimerTask() {

            override fun run() {
                Log.i(TAG, "timerRun:")
                mainView.updateProgress(mMediaPlayer.currentPosition)
                //                Log.i(TAG, "progress: " + mAudioCurrentPosition);
            }
        }
        mTimer = Timer()
        mTimer?.schedule(mTimerTask, 0, 200)
    }

}