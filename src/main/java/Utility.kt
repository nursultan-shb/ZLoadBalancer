@file:JvmName("Utility")

import org.apache.logging.log4j.LogManager

private val logger = LogManager.getLogger()

fun sleep(millis: Long, nanos: Int) {
    try {
        Thread.sleep(millis, nanos)
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        logger.error("Interrupted my sleep :S -> interrupting!", e)
    }

}

fun sleepNoLog(millis: Long, nanos: Int) {
    try {
        Thread.sleep(millis, nanos)
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
    }
}