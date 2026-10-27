package dev.ofarafal.enklawa.tor

import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Prawdziwy test, czy Tor przepuszcza ruch: otwiera połączenie SOCKS5 z lokalnym demonem Tora i prosi o strumień
 * do wskazanego hosta (nazwę przekazujemy Torowi, więc DNS nie wycieka). Odpowiedź „sukces” przychodzi dopiero,
 * gdy Tor zbudował obwód i podłączył strumień. Nie wysyłamy żadnych danych aplikacji; zamykamy od razu po odpowiedzi.
 */
object SocksProbe {
    fun canConnect(
        proxyPort: Int,
        targetHost: String = "check.torproject.org",
        targetPort: Int = 443,
        proxyHost: String = "127.0.0.1",
        timeoutMs: Int = 20_000,
    ): Boolean {
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(proxyHost, proxyPort), 3_000)
                s.soTimeout = timeoutMs
                val out = s.getOutputStream()
                val input = s.getInputStream()
                out.write(byteArrayOf(5, 1, 0))
                out.flush()
                val greeting = readN(input, 2)
                if (greeting[0] != 5.toByte() || greeting[1] != 0.toByte()) return false
                val host = targetHost.toByteArray(Charsets.US_ASCII)
                out.write(byteArrayOf(5, 1, 0, 3, host.size.toByte()) + host + byteArrayOf((targetPort shr 8).toByte(), targetPort.toByte()))
                out.flush()
                val reply = readN(input, 4)
                reply[0] == 5.toByte() && reply[1] == 0.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun readN(input: InputStream, n: Int): ByteArray {
        val buf = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = input.read(buf, read, n - read)
            if (r < 0) throw java.io.EOFException()
            read += r
        }
        return buf
    }
}
