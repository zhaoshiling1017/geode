package org.apache.geode.internal.cache

import io.micrometer.core.instrument.MeterRegistry

object MicroMeterRegistryFactory {
    private val meterRegistry = MicrometerStats().meterRegistry
    fun getMeterRegistry(): MeterRegistry = meterRegistry
}