package pw.modder.answernator4.db

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.api.logging.Log
import org.flywaydb.core.api.logging.LogCreator

class FlywayLogCreator : LogCreator {
    override fun createLogger(clazz: Class<*>): Log {
        val logger = KotlinLogging.logger(clazz.name)
        return KotlinLoggerLog(logger)
    }

    private class KotlinLoggerLog(val logger: KLogger) : Log {
        override fun debug(message: String) {
            logger.debug { message }
        }

        override fun info(message: String) {
            logger.info { message }
        }

        override fun warn(message: String) {
            logger.warn { message }
        }

        override fun error(message: String) {
            logger.error { message }
        }

        override fun error(message: String, e: Exception?) {
            logger.error(e) { message }
        }

        override fun notice(message: String) {}
    }
}
