package rpg.gamepad

import rpg.world.Direction
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Reads gamepad input from the Linux joystick device (/dev/input/js0).
 *
 * Uses a daemon thread for blocking reads so the main thread never stalls.
 * The Linux joystick protocol sends 8-byte events:
 *   uint32 time | int16 value | uint8 type | uint8 number
 * type 1 = button, type 2 = axis (value range -32767..32767).
 *
 * Standard USB gamepad axis mapping:
 *   0 = left stick X, 1 = left stick Y, 6 = d-pad X, 7 = d-pad Y
 * Button 0 = south face button (A / Cross / B depending on layout).
 */
private class LinuxJoystickPoller(devicePath: String) : ControllerPoller {

    private data class JsEvent(val type: Int, val number: Int, val value: Int)

    private val events = ConcurrentLinkedQueue<JsEvent>()

    private var axisX = 0f
    private var axisY = 0f
    private var dpadX = 0f
    private var dpadY = 0f
    private var btnPrev = false
    private var btnCurr = false
    private var btnAttackPrev = false
    private var btnAttackCurr = false

    @Volatile private var running = true

    private val thread: Thread? = try {
        val fis = FileInputStream(devicePath)
        Thread {
            val buf = ByteArray(8)
            val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
            try {
                while (running) {
                    val n = fis.read(buf)
                    if (n < 8) break
                    bb.rewind()
                    bb.int                              // timestamp — ignored
                    val value  = bb.short.toInt()
                    val type   = bb.get().toInt() and 0x7F   // strip init flag
                    val number = bb.get().toInt()
                    events.offer(JsEvent(type, number, value))
                }
            } catch (_: Exception) {
            } finally {
                fis.close()
            }
        }.also { it.isDaemon = true; it.name = "js-reader"; it.start() }
    } catch (_: Exception) { null }

    override fun poll(): Boolean {
        if (thread == null || !thread.isAlive) return false
        btnPrev = btnCurr
        btnAttackPrev = btnAttackCurr
        while (true) {
            val e = events.poll() ?: break
            when (e.type) {
                1 -> when (e.number) {
                    0 -> btnCurr = e.value != 0
                    1 -> btnAttackCurr = e.value != 0
                }
                2 -> {
                    val v = e.value / 32767f
                    when (e.number) {
                        0 -> axisX = v
                        1 -> axisY = v
                        6 -> dpadX = v
                        7 -> dpadY = v
                    }
                }
            }
        }
        return true
    }

    override fun direction(): Direction? {
        val D = 0.4f
        return when {
            dpadX < -D -> Direction.LEFT
            dpadX >  D -> Direction.RIGHT
            dpadY < -D -> Direction.UP
            dpadY >  D -> Direction.DOWN
            axisX < -D -> Direction.LEFT
            axisX >  D -> Direction.RIGHT
            axisY < -D -> Direction.UP
            axisY >  D -> Direction.DOWN
            else -> null
        }
    }

    override fun consumeInteract(): Boolean = btnCurr && !btnPrev
    override fun consumeAttack(): Boolean = btnAttackCurr && !btnAttackPrev

    override fun release() {
        running = false
        thread?.interrupt()
    }
}

actual fun createControllerPoller(): ControllerPoller? {
    val candidates = listOf(
        "/dev/input/js0", "/dev/input/js1",
        "/dev/input/js2", "/dev/input/js3"
    )
    for (path in candidates) {
        val p = try { LinuxJoystickPoller(path) } catch (_: Throwable) { continue }
        // Give the reader thread a moment, then check if it's alive
        Thread.sleep(20)
        if (p.poll()) return p
        p.release()
    }
    return null
}
