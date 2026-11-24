package com.vinz.fpsroyale.net

import java.net.ServerSocket
import java.net.Socket
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Server relay sederhana untuk mode Multiplayer Online: terima koneksi dari banyak client,
 * lalu broadcast ulang semua pesan (STATE/SHOOT/HIT/DEATH/LEAVE) ke semua client lain.
 * Tidak melakukan validasi/anti-cheat — ini relay polos buat main bareng teman/komunitas kecil.
 *
 * CARA PAKAI (di luar APK, dijalankan di PC/laptop/VPS yang nyala terus):
 *   1. Install JDK 17+
 *   2. kotlinc GameServer.kt Protocol.kt -include-runtime -d server.jar
 *   3. java -jar server.jar 7777        (7777 = port, bebas pilih)
 *   4. Kalau mau diakses dari luar jaringan sendiri: port-forward 7777 di router,
 *      atau jalankan di VPS (DigitalOcean/AWS/dsb) yang punya IP publik.
 *   5. Di app, masuk ke menu Multiplayer > masukin IP:port server ini.
 */
object GameServer {
    @JvmStatic
    fun main(args: Array<String>) {
        val port = if (args.isNotEmpty()) args[0].toIntOrNull() ?: 7777 else 7777
        val clients = ConcurrentHashMap<String, PrintWriter>()

        println("FPS Royale relay server starting on port $port ...")
        val server = ServerSocket(port)
        println("Listening on 0.0.0.0:$port")

        while (true) {
            val socket: Socket = server.accept()
            thread(isDaemon = true) { handleClient(socket, clients) }
        }
    }

    private fun handleClient(socket: Socket, clients: ConcurrentHashMap<String, PrintWriter>) {
        var playerId: String? = null
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            while (true) {
                val line = reader.readLine() ?: break
                val msg = NetMessage.decode(line) ?: continue

                if (msg is NetMessage.Hello) {
                    playerId = msg.playerId
                    clients[msg.playerId] = writer
                    println("Player joined: ${msg.name} (${msg.playerId}) — total ${clients.size}")
                }

                // broadcast ke semua client LAIN (bukan ke pengirim sendiri)
                val senderId = playerId
                clients.forEach { (id, w) ->
                    if (id != senderId) {
                        try { w.println(line) } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {
            // koneksi putus
        } finally {
            playerId?.let {
                clients.remove(it)
                clients.values.forEach { w -> try { w.println(NetMessage.Leave(it).encode()) } catch (_: Exception) {} }
                println("Player left: $it — total ${clients.size}")
            }
            try { socket.close() } catch (_: Exception) {}
        }
    }
}
