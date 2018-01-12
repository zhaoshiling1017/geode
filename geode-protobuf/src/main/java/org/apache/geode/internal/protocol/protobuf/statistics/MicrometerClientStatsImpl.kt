package org.apache.geode.internal.protocol.protobuf.statistics

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import io.micrometer.jmx.JmxMeterRegistry
import org.apache.geode.internal.cache.MicroMeterRegistryFactory
import org.apache.geode.internal.protocol.statistics.ProtocolClientStatistics
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class MicrometerClientStatsImpl(val meterRegistry: MeterRegistry) : ProtocolClientStatistics {
    constructor() : this(MicroMeterRegistryFactory.getMeterRegistry())

    private val clientsConnected = AtomicInteger(0)

    val clientConnectedCounter = meterRegistry.gauge("clientConnected", clientsConnected)
    val messageReceivedCounter = meterRegistry.summary("messageReceived")
    val messageSentCounter = meterRegistry.summary("messageSent")
    val authorizationViolationsCounter = meterRegistry.counter("authorizationViolations")
    val authenticationFailureCounter = meterRegistry.counter("authenticationFailures")

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