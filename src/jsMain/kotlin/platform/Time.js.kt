package platform

import kotlinx.browser.window
import kotlin.js.Date

actual fun nanoTime(): Long = Date.now().toLong()  * 1000000
actual fun currentTimeMillis() = Date.now().toLong()
actual fun sleep(millis: Long) {
    window.setTimeout({}, timeout = millis.toInt())}