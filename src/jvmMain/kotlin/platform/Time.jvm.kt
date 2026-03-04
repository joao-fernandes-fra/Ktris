package platform

actual fun nanoTime(): Long = System.nanoTime()
actual fun currentTimeMillis() = System.currentTimeMillis()
actual fun sleep(millis: Long) = Thread.sleep(millis)