package org.apache.geode.internal.cache

import com.netflix.spectator.impl.AtomicDouble
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.apache.geode.Statistics
import java.lang.Number
import java.util.concurrent.atomic.AtomicInteger

open class MicrometerPartitionRegionStats(val meterRegistry: MeterRegistry, val regionName: String) : PartitionedRegionStats {
    override fun getStats(): Statistics? {
        //we do nothing here... because we don't need to
        return null;
    }

    @Suppress("PropertyName")
    protected val PARTITIONED_REGION = "PartitionedRegion"
    private val tags = listOf<Tag>(Tag.of("region", regionName), Tag.of("regionType", PARTITIONED_REGION))

    private fun constructCounterForMetric(metricName: String): Counter {
        return meterRegistry.counter("${metricName}", tags)
    }

    private fun <T> constructGaugeForMetric(metricName: String, atomic: T, function: (T) -> Double): Gauge = Gauge.builder(metricName, atomic, function).tags(tags).register(meterRegistry)


    private fun incrementAtomic(atomic: AtomicInteger, value: Int) {
        atomic.addAndGet(value)
    }

    private fun incrementAtomic(atomic: AtomicDouble, value: Double) {
        atomic.addAndGet(value)
    }

    //Atomic values to track
    private val bucketCountAtomic = AtomicInteger(0)
    private val lowBucketCountAtomic = AtomicInteger(0)
    private val numberCopiesBucketCountAtomic = AtomicInteger(0)
    private val totalNumberOfBucketsAtomic = AtomicInteger(0)
    private val primaryBucketCountAtomic = AtomicInteger(0)
    private val numberVolunteeringThreadsAtomic = AtomicInteger(0)

    //Micrometer Meters
    private val putCounter = constructCounterForMetric("put")
    private val putAllCounter = constructCounterForMetric("putAll")
    private val createCounter = constructCounterForMetric("create")
    private val removeAllCounter = constructCounterForMetric("removeAll")
    private val getCounter = constructCounterForMetric("get")
    private val destroyCounter = constructCounterForMetric("destroy")
    private val invalidateCounter = constructCounterForMetric("invalidate")
    private val containsKeyCounter = constructCounterForMetric("containsKey")
    private val containValueForKeyCounter = constructCounterForMetric("containValueForKey")
    private val containsKeyValueRetriesCounter = constructCounterForMetric("containsKeyValueRetries")
    private val containsKeyValueOpsRetriedCounter = constructCounterForMetric("containsKeyValueOpsRetried")
    private val invalidateRetriesCounter = constructCounterForMetric("invalidateRetries")
    private val invalidateOpsRetriedCounter = constructCounterForMetric("invalidateOpsRetried")
    private val destroyRetriesCounter = constructCounterForMetric("destroyRetries")
    private val destroyOpsRetriedCounter = constructCounterForMetric("destroyOpsRetried")
    private val putRetriesCounter = constructCounterForMetric("putRetries")
    private val putOpsRetriedCounter = constructCounterForMetric("putOpsRetried")
    private val getOpsRetriedCounter = constructCounterForMetric("getOpsRetried")
    private val getRetriesCounter = constructCounterForMetric("getRetries")
    private val createOpsRetriedCounter = constructCounterForMetric("createOpsRetried")
    private val createRetriesCounter = constructCounterForMetric("createRetries")
    private val preferredReadLocalCounter = constructCounterForMetric("preferredReadLocal")
    private val preferredReadRemoteCounter = constructCounterForMetric("preferredReadRemote")
    private val putAllRetriesCounter = constructCounterForMetric("putAllRetries")
    private val putAllMsgsRetriedCounter = constructCounterForMetric("putAllMsgsRetried")
    private val removeAllRetriesCounter = constructCounterForMetric("removeAllRetries")
    private val removeAllMsgsRetriedCounter = constructCounterForMetric("removeAllMsgsRetried")
    private val partitionMessagesSentCounter = constructCounterForMetric("partitionMessagesSent")
    private val prMetaDataSentCounter = constructCounterForMetric("prMetaDataSentCounter")
    private val bucketCountGauge = constructGaugeForMetric("bucketCount", bucketCountAtomic, { it.get().toDouble() })
    private val lowBucketCountGauge = constructGaugeForMetric("lowBucketCount", lowBucketCountAtomic, { it.get().toDouble() })
    private val numberCopiesBucketCountGauge = constructGaugeForMetric("numberCopiesBucketCount", numberCopiesBucketCountAtomic, { it.get().toDouble() })
    private val totalNumberOfBucketsGauge = constructGaugeForMetric("totalNumberOfBuckets", totalNumberOfBucketsAtomic, { it.get().toDouble() })
    private val primaryBucketCountGauge = constructGaugeForMetric("primaryBucketCount", primaryBucketCountAtomic, { it.get().toDouble() })
    private val numberVolunteeringThreadsGauge = constructGaugeForMetric("numberVolunteeringThreads", numberVolunteeringThreadsAtomic, { it.get().toDouble() })

    override fun close() {
        //Noop
    }

    override fun endPut(startTimeInNanos: Long) = putCounter.increment()
    override fun endPutAll(startTimeInNanos: Long) = putAllCounter.increment()
    override fun endCreate(startTimeInNanos: Long) = createCounter.increment()
    override fun endRemoveAll(startTimeInNanos: Long) = removeAllCounter.increment()
    override fun endGet(startTimeInNanos: Long) = getCounter.increment()
    override fun endDestroy(startTimeInNanos: Long) = destroyCounter.increment()
    override fun endInvalidate(startTimeInNanos: Long) = invalidateCounter.increment()
    override fun endContainsKey(startTimeInNanos: Long) = containsKeyCounter.increment()
    override fun endContainsValueForKey(startTimeInNanos: Long) = containValueForKeyCounter.increment()
    override fun incContainsKeyValueRetries() = containsKeyValueRetriesCounter.increment()
    override fun incContainsKeyValueOpsRetried() = containsKeyValueOpsRetriedCounter.increment()
    override fun incInvalidateRetries() = invalidateRetriesCounter.increment()
    override fun incInvalidateOpsRetried() = invalidateOpsRetriedCounter.increment()
    override fun incDestroyRetries() = destroyRetriesCounter.increment()
    override fun incDestroyOpsRetried() = destroyOpsRetriedCounter.increment()
    override fun incPutRetries() = putRetriesCounter.increment()
    override fun incPutOpsRetried() = putOpsRetriedCounter.increment()
    override fun incGetOpsRetried() = getOpsRetriedCounter.increment()
    override fun incGetRetries() = getRetriesCounter.increment()
    override fun incCreateOpsRetried() = createOpsRetriedCounter.increment()
    override fun incCreateRetries() = createRetriesCounter.increment()
    override fun incPreferredReadLocal() = preferredReadLocalCounter.increment()
    override fun incPreferredReadRemote() = preferredReadRemoteCounter.increment()
    override fun incPutAllRetries() = putAllRetriesCounter.increment()
    override fun incPutAllMsgsRetried() = putAllMsgsRetriedCounter.increment()
    override fun incRemoveAllRetries() = removeAllRetriesCounter.increment()
    override fun incRemoveAllMsgsRetried() = removeAllMsgsRetriedCounter.increment()
    override fun incPartitionMessagesSent() = partitionMessagesSentCounter.increment()
    override fun incBucketCount(bucketCount: Int) = incrementAtomic(bucketCountAtomic,bucketCount)
    override fun incLowRedundancyBucketCount(lowBucketCount: Int) = incrementAtomic(lowBucketCountAtomic,lowBucketCount)
    override fun incNoCopiesBucketCount(numberCopiesBucketCount: Int) = incrementAtomic(numberCopiesBucketCountAtomic, numberCopiesBucketCount)
    override fun incTotalNumBuckets(totalNumberOfBuckets: Int) = incrementAtomic(totalNumberOfBucketsAtomic, totalNumberOfBuckets)
    override fun incPrimaryBucketCount(primaryBucketCount: Int) = incrementAtomic(primaryBucketCountAtomic, primaryBucketCount)
    override fun incVolunteeringThreads(numberVolunteeringThreads: Int) = incrementAtomic(numberVolunteeringThreadsAtomic, numberVolunteeringThreads)
    override fun incPRMetaDataSentCount() = prMetaDataSentCounter.increment()

    override fun incDataStoreEntryCount(amt: Int) {

    }

    override fun incBytesInUse(delta: Long) {
        0
    }

    override fun getVolunteeringInProgress(): Int = 0

    override fun startPartitionMessageProcessing(): Long = 0

    override fun endPartitionMessagesProcessing(start: Long) {

    }

    override fun setBucketCount(i: Int) {

    }


    override fun getDataStoreEntryCount(): Int = 0


    override fun getDataStoreBytesInUse(): Long = 0

    override fun getTotalBucketCount(): Int = 0

    override fun getVolunteeringBecamePrimary(): Int = 0

    override fun getVolunteeringBecamePrimaryTime(): Long = 0

    override fun getVolunteeringOtherPrimary(): Int = 0

    override fun getVolunteeringOtherPrimaryTime(): Long = 0

    override fun getVolunteeringClosed(): Int = 0

    override fun getVolunteeringClosedTime(): Long = 0

    override fun startVolunteering(): Long = 0

    override fun endVolunteeringBecamePrimary(start: Long) {

    }

    override fun endVolunteeringOtherPrimary(start: Long) {

    }

    override fun endVolunteeringClosed(start: Long) {

    }

    override fun getTotalNumBuckets(): Int = 0


    override fun getPrimaryBucketCount(): Int = 0


    override fun getVolunteeringThreads(): Int = 0


    override fun getLowRedundancyBucketCount(): Int = 0

    override fun getNoCopiesBucketCount(): Int = 0


    override fun getConfiguredRedundantCopies(): Int = 0

    override fun setConfiguredRedundantCopies(value: Int) {

    }

    override fun setLocalMaxMemory(l: Long) {

    }

    override fun getActualRedundantCopies(): Int = 0

    override fun setActualRedundantCopies(value: Int) {

    }

    override fun putStartTime(key: Any?, startTime: Long) {

    }

    override fun removeStartTime(key: Any?): Long = 0

    override fun endGetEntry(startTime: Long) {

    }

    override fun endGetEntry(start: Long, numInc: Int) {

    }

    override fun startRecovery(): Long = 0

    override fun endRecovery(start: Long) {

    }

    override fun startBucketCreate(isRebalance: Boolean): Long = 0

    override fun endBucketCreate(start: Long, success: Boolean, isRebalance: Boolean) {

    }

    override fun startPrimaryTransfer(isRebalance: Boolean): Long = 0

    override fun endPrimaryTransfer(start: Long, success: Boolean, isRebalance: Boolean) {

    }

    override fun getBucketCreatesInProgress(): Int = 0

    override fun getBucketCreatesCompleted(): Int = 0

    override fun getBucketCreatesFailed(): Int = 0

    override fun getBucketCreateTime(): Long = 0

    override fun getPrimaryTransfersInProgress(): Int = 0

    override fun getPrimaryTransfersCompleted(): Int = 0

    override fun getPrimaryTransfersFailed(): Int = 0

    override fun getPrimaryTransferTime(): Long = 0

    override fun getRebalanceBucketCreatesInProgress(): Int = 0

    override fun getRebalanceBucketCreatesCompleted(): Int = 0

    override fun getRebalanceBucketCreatesFailed(): Int = 0

    override fun getRebalanceBucketCreateTime(): Long = 0

    override fun getRebalancePrimaryTransfersInProgress(): Int = 0

    override fun getRebalancePrimaryTransfersCompleted(): Int = 0

    override fun getRebalancePrimaryTransfersFailed(): Int = 0

    override fun getRebalancePrimaryTransferTime(): Long = 0

    override fun startApplyReplication(): Long = 0

    override fun endApplyReplication(start: Long) {

    }

    override fun startSendReplication(): Long = 0

    override fun endSendReplication(start: Long) {

    }

    override fun startPutRemote(): Long = 0

    override fun endPutRemote(start: Long) {

    }

    override fun startPutLocal(): Long = 0

    override fun endPutLocal(start: Long) {

    }


    override fun getPRMetaDataSentCount(): Long = 0
}