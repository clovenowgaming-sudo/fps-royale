package com.vinz.fpsroyale.net

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Client TCP sederhana buat mode Multiplayer Online.
 * Dipakai dari FPSGame: connect(ip, port) -> kirim State tiap frame -> baca pesan masuk
 * dari incoming queue di game loop (JANGAN sentuh objek libGDX langsung dari thread network!).
 *
 * Catatan penting: ini cuma CLIENT. Supaya "online" beneran (bukan cuma LAN), harus ada
 * GameServer (lihat net/GameServer.kt) yang jalan 24/7 di VPS/PC dengan IP publik atau
 * port-forward. Kalau cuma mau main LAN/WiFi yang sama, jalanin GameServer di salah satu HP/PC
 * di jaringan yang sama lalu connect ke IP lokalnya.
 */
class MultiplayerClient {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private val running = AtomicBoolean(false)

    val incoming = ConcurrentLinkedQueue<NetMessage>()
    var isConnected = false
        private set
    var lastError: String? = null
        private set

    fun connect(host: String, port: Int, playerId: String, playerName: String) {
        thread(isDaemon = true) {
            try {
                val s = Socket(host, port)
                socket = s
                writer = PrintWriter(s.getOutputStream(), true)
                running.set(true)
                isConnected = true
                send(NetMessage.Hello(playerId, playerName))

                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                while (running.get()) {
                    val line = reader.readLine() ?: break
                    NetMessage.decode(line)?.let { incoming.add(it) }
                }
            } catch (e: Exception) {
                lastError = e.message
            } finally {
                isConnected = false
                running.set(false)
            }
        }
    }

    fun send(msg: NetMessage) {
        try {
            writer?.println(msg.encode())
        } catch (_: Exception) {
            // koneksi putus, biarkan loop baca yang nangani reconnect/disconnect state
        }
    }

    fun disconnect() {
        running.set(false)
        try { socket?.close() } catch (_: Exception) {}
        isConnected = false
    }
}
