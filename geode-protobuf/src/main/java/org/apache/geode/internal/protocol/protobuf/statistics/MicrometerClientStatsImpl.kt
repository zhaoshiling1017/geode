package org.apache.geode.internal.protocol.protobuf.statistics

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import io.micrometer.jmx.JmxMeterRegistry
import org.apache.geode.internal.protocol.statistics.ProtocolClientStatistics
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class MicrometerClientStatsImpl : ProtocolClientStatistics {

    private val clientsConnected = AtomicInteger(0)

    private val influxMetrics: MeterRegistry = InfluxMeterRegistry(object : InfluxConfig {
        override fun step(): Duration = Duration.ofSeconds(1)
        override fun db(): String = "mydb"
        override fun get(k: String): String? = null
        override fun uri(): String = "http://localhost:8086"
    }, Clock.SYSTEM)

//    private val atlasMetrics: MeterRegistry = AtlasMeterRegistry(object : AtlasConfig {
//        override fun get(k: String?): String? = null
//        override fun enabled(): Boolean = true
//        override fun uri(): String = "http://localhost:7101/api/v1/publish"
//        override fun step(): Duration = Duration.ofSeconds(10)
//    }, Clock.SYSTEM)

    private val jmxMetrics: MeterRegistry = JmxMeterRegistry()

    private val metrics = CompositeMeterRegistry(Clock.SYSTEM)

    init {
        metrics.add(influxMetrics)
//        metrics.add(atlasMetrics)
        metrics.add(jmxMetrics)
    }

    val clientConnectedCounter = metrics.gauge("clientConnected", clientsConnected)
    val messageReceivedCounter = metrics.summary("messageReceived")
    val messageSentCounter = metrics.summary("messageSent")
    val authorizationViolationsCounter = metrics.counter("authorizationViolations")
    val authenticationFailureCounter = metrics.counter("authenticationFailures")

    override fun clientConnected() {
        clientsConnected.incrementAndGet()
    }

    override fun clientDisconnected() {
        clientsConnected.decrementAndGet()
    }

    override fun messageReceived(bytes: Int) {
        messageReceivedCounter.record(bytes.toDouble())
    }

    override fun messageSent(bytes: Int) {
        messageSentCounter.record(bytes.toDouble())
    }

    override fun incAuthorizationViolations() {
        authorizationViolationsCounter.increment()
    }

    override fun incAuthenticationFailures() {
        authenticationFailureCounter.increment()
    }
}