package com.wesleymarques.walkietalkie.managers

import com.wesleymarques.walkietalkie.R
import com.wesleymarques.walkietalkie.model.Friend
import com.wesleymarques.walkietalkie.util.ResourceManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Created by wesley on 04/12/17.
 *
 * Find and tell other guys using UDP packets, i know about Android's Service discovery
 * but the idea is in future communicate with others platforms too.
 *
 *
 */
object NodeManager {

    enum class Status {
        IDLE,
        LINKED,
        UNLINK,
        SPEAKING
    }

    var status = Status.IDLE
    var running:Boolean = false
    var selectedFriend:Friend? = null
    var onFriendFound:(Friend?) -> Unit = {}
    var onFriendLinked:(Friend?) -> Unit = {}
    var onFriendUnLinked:() -> Unit = {}
    var onFriendSpeaking:() -> Unit = {}
    var myName:String? = ResourceManager.getString(R.string.my_name)
    var myIp:String? = null

    private val PORT = 8888
    private var broadcast:InetAddress? = null
    private var stateMap:MutableMap<String, (String, String)->Unit > = mutableMapOf()
    private var sendLastMessage:Boolean = false
    private var listenSocket:DatagramSocket? = null
    private var broadcastSocket:DatagramSocket? = null

    private var listenThread:Thread? = null
    private var broadcastThread:Thread? = null

    init {
        loadNetworkInfo()
        setupStates()
    }

    private fun setupStates() {

        val idleAction:(String, String)->Unit = { host, name ->
            status = Status.IDLE
            selectedFriend = null
            onFriendUnLinked()
        }

        val linkAction:(String, String)->Unit = { host, name ->
            status = Status.LINKED
            selectedFriend = Friend(host!!, name!!)
            onFriendLinked?.invoke(selectedFriend)

        }

        val checkIsPlaying:(String, String)->Unit = { host, name ->
            if ( StreamPlayerManager.playing ) {
                StreamPlayerManager.stopPlay()
            }
            onFriendLinked?.invoke(selectedFriend)
            stateMap.remove("[LINKED][LINKED][MATCHED]")
            println("checkIsPlaying")
        }

        val friendSpeakingAction:(String, String)->Unit = { host, name ->
            if (!StreamPlayerManager.playing) {
                status = Status.LINKED
                onFriendSpeaking?.invoke()
                StreamPlayerManager.playStream({}, {})
                stateMap["[LINKED][LINKED][MATCHED]"] = checkIsPlaying
                println("friendSpeakingAction")
            }
        }

        stateMap["[IDLE][LINKED][MATCHED]"] = linkAction
        stateMap["[IDLE][UNLINK][MATCHED]"] = idleAction
        stateMap["[LINKED][UNLINK][MATCHED]"] = idleAction
        stateMap["[LINKED][SPEAKING][MATCHED]"] = friendSpeakingAction
        stateMap["[UNLINK][IDLE][MATCHED]"] = idleAction
        stateMap["[UNLINK][IDLE]"] = idleAction


    }

    private fun loadNetworkInfo() : String? {
        if (myIp.isNullOrBlank()) {
            val regex = Regex("^((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])\\.){0,3}"+
                    "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])){0,1}$")
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (i in interfaces) {
                if (i.displayName.contains("wlan")) {
                    for (addr in i.interfaceAddresses) {
                        if (regex.matchEntire(addr.address.hostAddress) != null) {
                            myIp = addr.address.hostAddress
                            broadcast = addr.broadcast

                            var unique = "${System.currentTimeMillis()}"
                            unique = unique.substring(7)
                            myName = "I am $unique"
                            println("My IP: ${myIp} Broadcast ${broadcast}")
                            break
                        }

                    }
                }
            }
        }
        return myIp
    }

    fun broadcast() {
        running = true
        if (broadcastThread == null) {
            broadcastThread = Thread( {
                broadcastSocket = DatagramSocket()
                while (running) {
                    broadcast.let {
                        val data = "${myName}|${status.name}|${selectedFriend?.host}|${selectedFriend?.name}".toByteArray(Charsets.UTF_8)
                        val packet = DatagramPacket(data, data.size,  broadcast, PORT )
                        packet.let {
                            try {
                                broadcastSocket?.send(packet)
                            } catch (e:Exception) {
                                e.printStackTrace()
                                running = false
                            }
                            sendLastMessage = false

                        }
                    }
                    Thread.sleep(1000)
                }
                println("broadcastThread END")
            })
            broadcastThread?.start()
        }
    }

    fun listen() {
        running = true
        if (listenThread == null) {
            listenThread = Thread( {
                listenSocket = DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"))
                listenSocket?.broadcast = true
                while (running) {
                    val buf = ByteArray(512)
                    val packet = DatagramPacket(buf, buf.size)
                    if (! listenSocket!!.isClosed) {
                        try {
                            listenSocket?.receive(packet)
                            if (packet.address.hostAddress != myIp) {
                                val cleanBuf = buf.copyOf(packet.length)
                                onFriendFound?.let {
                                    val data = String(cleanBuf)
                                    val datArr = data.split("|")
                                    val name = datArr[0]
                                    val friendStatus = Status.valueOf(datArr[1])
                                    val friendHost = datArr[2]
                                    val friendName = datArr[3]
                                    val matched = if (myIp.equals(friendHost) || selectedFriend?.host.equals(packet.address.hostAddress) ) "[MATCHED]" else ""
                                    val state = "[$status][$friendStatus]$matched"

                                    if (stateMap.containsKey(state)) {
                                        println(state)
                                        stateMap[state]?.invoke(packet.address.hostAddress, name)
                                    }

                                    it(Friend(packet.address.hostAddress, name))
                                }
                            }
                        } catch (e:Exception) {
                            e.printStackTrace()
                            running = false
                        }

                    }

                }
                println("listenThread END")
            })
            listenThread?.start()
        }
    }

    fun stopAll() {
        status = Status.UNLINK
        selectedFriend = null
        Thread().run {
            sendLastMessage = true
            while (sendLastMessage) {
                Thread.sleep(1000)
            }
            running = false
            broadcastSocket?.close()
            listenSocket?.close()
            listenThread = null
            broadcastThread = null
        }
    }
}