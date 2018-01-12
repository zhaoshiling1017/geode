package org.apache.geode.internal.cache

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit

class TimedMicrometerPartitionedRegionStats(meterRegistry: MeterRegistry, regionName: String) : MicrometerPartitionRegionStats(meterRegistry,regionName) {

    constructor(regionName:String) : this(MicroMeterRegistryFactory.getMeterRegistry(),regionName)

    private fun constructTimerForMetric(metricName: String): Timer =
            meterRegistry.timer("${metricName}Latency", listOf(Tag.of("region", regionName), Tag.of("regionType", PARTITIONED_REGION)))

    private val putTimer = constructTimerForMetric("put")
    private val putAllTimer = constructTimerForMetric("putAll")
    private val createTimer = constructTimerForMetric("create")
    private val removeAllTimer = constructTimerForMetric("removeAll")
    private val getTimer = constructTimerForMetric("get")
    private val destroyTimer = constructTimerForMetric("destroy")
    private val invalidateTimer = constructTimerForMetric("invalidate")
    private val containsKeyTimer = constructTimerForMetric("containsKey")
    private val containValueForKeyTimer = constructTimerForMetric("containValueForKey")

    override fun endPut(startTimeInNanos: Long) {
        super.endPut(startTimeInNanos)
        updateTimer(startTimeInNanos, putTimer)
    }

    override fun endPutAll(startTimeInNanos: Long) {
        super.endPutAll(startTimeInNanos)
        updateTimer(startTimeInNanos, putAllTimer)
    }

    override fun endCreate(startTimeInNanos: Long) {
        super.endCreate(startTimeInNanos)
        updateTimer(startTimeInNanos, createTimer)
    }

    override fun endRemoveAll(startTimeInNanos: Long) {
        super.endRemoveAll(startTimeInNanos)
        updateTimer(startTimeInNanos, removeAllTimer)
    }

    override fun endGet(startTimeInNanos: Long) {
        super.endGet(startTimeInNanos)
        updateTimer(startTimeInNanos, getTimer)
    }

    override fun endDestroy(startTimeInNanos: Long) {
        super.endDestroy(startTimeInNanos)
        updateTimer(startTimeInNanos, destroyTimer)
    }

    override fun endInvalidate(startTimeInNanos: Long) {
        super.endInvalidate(startTimeInNanos)
        updateTimer(startTimeInNanos, invalidateTimer)
    }

    override fun endContainsKey(startTimeInNanos: Long) {
        super.endContainsKey(startTimeInNanos)
        updateTimer(startTimeInNanos, containsKeyTimer)
    }
    override fun endContainsValueForKey(startTimeInNanos: Long) {
        super.endContainsValueForKey(startTimeInNanos)
        updateTimer(startTimeInNanos, containValueForKeyTimer)
    }

    private fun updateTimer(startTimeInNanos: Long, timer: Timer) {
        timer.record((System.nanoTime() - startTimeInNanos), TimeUnit.NANOSECONDS)
    }
}