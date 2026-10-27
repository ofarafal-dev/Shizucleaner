package dev.ofarafal.enklawa.tor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import kotlin.concurrent.thread

class SocksProbeTest {

    /** Atrapa serwera SOCKS5: odpowiada wg zadanych bajtów i zapamiętuje, o jaki host poproszono. */
    private class FakeSocks(val greeting: ByteArray, val reply: ByteArray) : AutoCloseable {
        val server = ServerSocket(0)
        @Volatile var requestedHost: String? = null
        val port get() = server.localPort
        init {
            thread(isDaemon = true) {
                runCatching {
                    server.accept().use { c ->
                        val i = c.getInputStream(); val o = c.getOutputStream()
                        i.readNBytes(3)
                        o.write(greeting); o.flush()
                        if (greeting.size == 2 && greeting[1] == 0.toByte()) {
                            val head = i.readNBytes(5)
                            val len = head[4].toInt()
                            requestedHost = String(i.readNBytes(len))
                            i.readNBytes(2)
                            o.write(reply); o.flush()
                        }
                    }
                }
            }
        }
        override fun close() = server.close()
    }

    private val ok = byteArrayOf(5, 0, 0, 1, 0, 0, 0, 0, 0, 0)
    private val refused = byteArrayOf(5, 5, 0, 1, 0, 0, 0, 0, 0, 0)

    @Test fun successReplyMeansTorIsReady() {
        FakeSocks(byteArrayOf(5, 0), ok).use { f ->
            assertTrue(SocksProbe.canConnect(f.port, timeoutMs = 3000))
            assertEquals("check.torproject.org", f.requestedHost)
        }
    }

    @Test fun refusedStreamMeansNotReady() {
        FakeSocks(byteArrayOf(5, 0), refused).use { f -> assertFalse(SocksProbe.canConnect(f.port, timeoutMs = 3000)) }
    }

    @Test fun badGreetingMeansNotReady() {
        FakeSocks(byteArrayOf(5, 0xFF.toByte()), ok).use { f -> assertFalse(SocksProbe.canConnect(f.port, timeoutMs = 3000)) }
    }

    @Test fun nothingListeningMeansNotReady() {
        val free = ServerSocket(0).use { it.localPort }
        assertFalse(SocksProbe.canConnect(free, timeoutMs = 1000))
    }
}
