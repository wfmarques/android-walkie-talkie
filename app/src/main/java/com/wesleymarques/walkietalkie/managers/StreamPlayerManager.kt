package com.wesleymarques.walkietalkie.managers

import android.content.Context
import android.media.*
import java.io.DataInputStream
import java.net.ServerSocket
import java.net.Socket
import android.media.AudioAttributes
import android.util.Log
import java.net.InetAddress
import android.media.AudioManager




/**
 * Created by wesley on 07/12/17.
 */
object StreamPlayerManager {


    private val PORT = 8899
    private val SAMPLE_RATE = 44100 // Hz
    private val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private val CHANNEL_MASK = AudioFormat.CHANNEL_OUT_STEREO
    private val BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING)
    var playing:Boolean = false
    private var playerThread:Thread? = null
    private var serverSocket: ServerSocket? =  null
    private var socket: Socket? =  null
    private var audioTrack: AudioTrack? = null
    private var serverRunning = false


    fun startServer(context: Context?) {
        if (serverSocket == null) {
            serverSocket = ServerSocket(PORT, 0, InetAddress.getByName(NodeManager.myIp))

            if (audioTrack == null) {

                audioTrack = AudioTrack(AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE, //sample rate
                        CHANNEL_MASK, //2 channel
                        ENCODING, // 16-bit
                        BUFFER_SIZE,
                        AudioTrack.MODE_STREAM )
                var mAudioManager:AudioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
            }
            serverRunning = true
        }

        Log.d("StreamPlayerManager", "Server Socket OPENED")
    }

    fun stopServer() {
        stopPlay()

        if (audioTrack?.state != AudioTrack.PLAYSTATE_STOPPED) {
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null

        serverRunning = false
        if (serverSocket != null) {
            serverSocket?.close()
            serverSocket = null
        }


        Log.d("StreamPlayerManager", "StreamPlayerManager Server Socket closed")
    }
    fun playStream(success:()->Unit = {}, error:(String?)->Unit) {
        if (!playing) {
            playing = true
            Thread {
                synchronized(playing, {
                    if (playing && socket == null) {
                        Log.d("StreamPlayerManager", "Accept Socket")
                        socket = serverSocket?.accept()
                        Log.d("StreamPlayerManager", "TRY PLAY")
                        Thread(SocketHandler(socket, success, error)).start()
                    }
                })
                Log.d("StreamPlayerManager", "Server Socket Thread FINISHED")

            }.start()
        }
    }
    fun stopPlay() {
        Log.d("StreamPlayerManager", "StreamPlayerManager Stop Play $playing")
        playing = false

    }

    class SocketHandler : Runnable {
        private val socket:Socket?
        private val success:()->Unit
        private val error:(String?)->Unit
        constructor(socket: Socket?, success:()->Unit = {}, error:(String?)->Unit) {
            this.socket = socket
            this.success = success
            this.error = error
        }
        override fun run() {
            if (playerThread == null) {
                playerThread = Thread({

                    try {
                        var input = DataInputStream(socket?.getInputStream())
                        val buffer = ByteArray(BUFFER_SIZE)
                        success()

                        while (playing && input != null) {
                            val bytesRead = input.read(buffer, 0, BUFFER_SIZE)
                            if (bytesRead > -1) {
                                audioTrack?.play()
                                audioTrack?.write(buffer, 0, bytesRead)
                                audioTrack?.pause()
                            } else {
                                break
                            }
                        }
                        if (input != null) {
                            input.close()
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        error(e.message)
                    } finally {
                        NodeManager.status = NodeManager.Status.LINKED
                        playing = false


                        playerThread = null

                        if (StreamPlayerManager.socket != null) {
                            StreamPlayerManager.socket?.close()
                            StreamPlayerManager.socket = null
                        }

                    }
                    Log.d("StreamPlayerManager", "StreamPlayer Client Socket CLOSED")
                })
                playerThread?.start()
            }
        }
    }
}