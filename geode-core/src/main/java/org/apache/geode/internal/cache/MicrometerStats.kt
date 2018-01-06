package org.apache.geode.internal.cache

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import io.micrometer.jmx.JmxMeterRegistry
import java.time.Duration

abstract class MicrometerStats {
    protected val metrics = CompositeMeterRegistry(Clock.SYSTEM)
    private val influxMetrics: MeterRegistry = InfluxMeterRegistry(object : InfluxConfig {
        override fun step(): Duration = Duration.ofSeconds(1)
        override fun db(): String = "mydb"
        override fun get(k: String): String? = null
        override fun uri(): String = "http://localhost:8086"
    }, Clock.SYSTEM)

    private val jmxMetrics: MeterRegistry = JmxMeterRegistry()

    init {
        metrics.add(influxMetrics)
//        metrics.add(atlasMetrics)
        metrics.add(jmxMetrics)
    }
}