package com.github.xbaimiao.shoppro.core.database

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader
import java.util.UUID
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class DatabaseLoggingTest {

    @Test
    fun `packaged database logging discovers its provider and sends messages to Java logging`() {
        val artifact = File(System.getProperty("shoppro.artifact"))
        // Prevent Gradle, MockBukkit or the server from supplying a missing API/provider.
        URLClassLoader(arrayOf(artifact.toURI().toURL()), ClassLoader.getPlatformClassLoader()).use { loader ->
            assertNotNull(loader.getResource("META-INF/services/org.slf4j_2_0_8.spi.SLF4JServiceProvider"))
            val factoryClass = loader.loadClass("org.slf4j_2_0_8.LoggerFactory")
            val factory = factoryClass.getMethod("getILoggerFactory").invoke(null)
            assertEquals("org.slf4j_2_0_8.jul.JDK14LoggerFactory", factory.javaClass.name)
            assertEquals(factoryClass.classLoader, factory.javaClass.classLoader)

            val loggerName = "ShopPro packaged database test ${UUID.randomUUID()}"
            val records = mutableListOf<LogRecord>()
            val handler = object : Handler() {
                override fun publish(record: LogRecord) { records += record }
                override fun flush() {}
                override fun close() {}
            }
            val julLogger = Logger.getLogger(loggerName)
            julLogger.useParentHandlers = false
            julLogger.level = Level.ALL
            julLogger.addHandler(handler)
            try {
                val logger = factoryClass.getMethod("getLogger", String::class.java).invoke(null, loggerName)
                loader.loadClass("org.slf4j_2_0_8.Logger").getMethod("info", String::class.java)
                    .invoke(logger, "Database logging is available")
                assertEquals(listOf("Database logging is available"), records.map { it.message })
                assertEquals(Level.INFO, records.single().level)
            } finally {
                julLogger.removeHandler(handler)
            }
        }
    }
}
