package com.wesleymarques.walkietalkie.managers

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import java.io.*
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder


/**
 * Created by wesley on 06/12/17.
 */
object RecorderManager {

    private val PORT = 8899
    private val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
    private val SAMPLE_RATE = 44100 // Hz
    private val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private val CHANNEL_MASK = AudioFormat.CHANNEL_IN_STEREO
    private val BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING)
    private var recording:Boolean = false
    private var recordThread:Thread? = null
    private var socket:Socket? =  null
    private var audioRecord:AudioRecord? = null

    fun startRecordAndStream(friendIP:String?, success:()->Unit = {}, error:(String?)->Unit) {

        if ( friendIP.isNullOrBlank() ) {
            error("Friend's IP is empty!")
            return
        }

        if (audioRecord == null) {
            audioRecord = AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE)
        }

        if (!recording) {
            recording = true
            if (recordThread == null) {
                recordThread = Thread({

                    var out:DataOutputStream? = null
                    try {
                        socket = Socket(friendIP, PORT)
                        out = DataOutputStream(socket?.getOutputStream())
                        audioRecord?.startRecording()
                        val buffer:ByteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
                        success()

                        while (recording) {
                            val readedBytes = audioRecord?.read(buffer, BUFFER_SIZE)
                            out?.write(buffer.array(), 0, readedBytes!!)
                            out?.flush()
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        error(e.message)
                    } finally {
                        recording = false
                        out?.close()
                        socket?.close()

                        if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            audioRecord?.stop()
                        }

                        if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                            audioRecord?.release()
                        }

                        audioRecord = null
                        recordThread = null
                        Log.d("RecorderManager", "Record FINISHED")
                    }


                })
                recordThread?.start()
            }
        }

    }

    fun stopRecording() {
        recording = false

    }

    fun getAlbumStorageDir(): File {
        // Get the directory for the user's public pictures directory.
        val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (!file.mkdirs()) {
            Log.e("MainActivity", "Directory not created ${file.absolutePath}")
        }
        return file
    }

    @Throws(IOException::class)
    private fun writeWavHeader(out: OutputStream, channelMask: Int, sampleRate: Int, encoding: Int) {
        val channels: Short
        when (channelMask) {
            AudioFormat.CHANNEL_IN_MONO -> channels = 1
            AudioFormat.CHANNEL_IN_STEREO -> channels = 2
            else -> throw IllegalArgumentException("Unacceptable channel mask")
        }

        val bitDepth: Short
        when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> bitDepth = 8
            AudioFormat.ENCODING_PCM_16BIT -> bitDepth = 16
            AudioFormat.ENCODING_PCM_FLOAT -> bitDepth = 32
            else -> throw IllegalArgumentException("Unacceptable encoding")
        }

        writeWavHeader(out, channels, sampleRate, bitDepth)
    }

    @Throws(IOException::class)
    private fun writeWavHeader(out: OutputStream, channels: Short, sampleRate: Int, bitDepth: Short) {
        // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
        val littleBytes = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels.toInt() * (bitDepth / 8))
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

    /**
     * Updates the given wav file's header to include the final chunk sizes
     *
     * @param wav The wav file to update
     * @throws IOException
     */
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
            accessWave!!.seek(4)
            accessWave!!.write(sizes, 0, 4)

            // Subchunk2Size
            accessWave!!.seek(40)
            accessWave!!.write(sizes, 4, 4)
        } catch (ex: IOException) {
            // Rethrow but we still close accessWave in our finally
            throw ex
        } finally {
            if (accessWave != null) {
                try {
                    accessWave!!.close()
                } catch (ex: IOException) {
                    //
                }

            }
        }
    }
}