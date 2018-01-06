package org.apache.geode.internal.cache

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Tag
import java.util.concurrent.atomic.AtomicInteger

open class MicrometerPartitionRegionStats(val regionName: String) : MicrometerStats() {

    @Suppress("PropertyName")
    protected val PARTITIONED_REGION = "PartitionedRegion"

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
    private val incInvalidateRetriesCounter = constructCounterForMetric("incInvalidateRetries")
    private val incInvalidateOpsRetriedCounter = constructCounterForMetric("incInvalidateOpsRetried")
    private val incDestroyRetriesCounter = constructCounterForMetric("incDestroyRetries")
    private val incDestroyOpsRetriedCounter = constructCounterForMetric("incDestroyOpsRetried")
    private val incPutRetriesCounter = constructCounterForMetric("incPutRetries")
    private val incPutOpsRetriedCounter = constructCounterForMetric("incPutOpsRetried")
    private val incGetOpsRetriedCounter = constructCounterForMetric("incGetOpsRetried")
    private val incGetRetriesCounter = constructCounterForMetric("incGetRetries")
    private val incCreateOpsRetriedCounter = constructCounterForMetric("incCreateOpsRetried")
    private val incCreateRetriesCounter = constructCounterForMetric("incCreateRetries")
    private val incPreferredReadLocalCounter = constructCounterForMetric("incPreferredReadLocal")
    private val incPreferredReadRemoteCounter = constructCounterForMetric("incPreferredReadRemote")
    private val incPutAllRetriesCounter = constructCounterForMetric("incPutAllRetries")
    private val incPutAllMsgsRetriedCounter = constructCounterForMetric("incPutAllMsgsRetried")
    private val incRemoveAllRetriesCounter = constructCounterForMetric("incRemoveAllRetries")
    private val incRemoveAllMsgsRetriedCounter = constructCounterForMetric("incRemoveAllMsgsRetried")
    private val incPartitionMessagesSentCounter = constructCounterForMetric("incPartitionMessagesSent")
    private val incBucketCountCounter = constructCounterForMetric("incBucketCount")

    private fun constructCounterForMetric(metricName: String): Counter =
            metrics.counter("${metricName}Counter", regionName, PARTITIONED_REGION)

    private fun constructAtomicIntegerToMonitor(metricName: String): AtomicInteger =
            metrics.gauge("${metricName}Gauge",listOf(regionName,PARTITIONED_REGION), AtomicInteger(0),AtomicInteger::get)

    open fun endPut(startTimeInNanos: Long) = putCounter.increment()
    open fun endPutAll(startTimeInNanos: Long) = putAllCounter.increment()
    open fun endCreate(startTimeInNanos: Long) = createCounter.increment()
    open fun endRemoveAll(startTimeInNanos: Long) = removeAllCounter.increment()
    open fun endGet(startTimeInNanos: Long) = getCounter.increment()
    open fun endDestroy(startTimeInNanos: Long) = destroyCounter.increment()
    open fun endInvalidate(startTimeInNanos: Long) = invalidateCounter.increment()
    open fun endContainsKey(startTimeInNanos: Long) = containsKeyCounter.increment()
    open fun endContainsValueForKey(startTimeInNanos: Long) = containValueForKeyCounter.increment()
    fun incContainsKeyValueRetries() = containsKeyValueRetriesCounter.increment()
    fun incContainsKeyValueOpsRetried() = containsKeyValueOpsRetriedCounter.increment()
    fun incInvalidateRetries() = incInvalidateRetriesCounter.increment()
    fun incInvalidateOpsRetried() = incInvalidateOpsRetriedCounter.increment()
    fun incDestroyRetries() = incDestroyRetriesCounter.increment()
    fun incDestroyOpsRetried() = incDestroyOpsRetriedCounter.increment()
    fun incPutRetries() = incPutRetriesCounter.increment()
    fun incPutOpsRetried() = incPutOpsRetriedCounter.increment()
    fun incGetOpsRetried() = incGetOpsRetriedCounter.increment()
    fun incGetRetries() = incGetRetriesCounter.increment()
    fun incCreateOpsRetried() = incCreateOpsRetriedCounter.increment()
    fun incCreateRetries() = incCreateRetriesCounter.increment()
    fun incPreferredReadLocal() = incPreferredReadLocalCounter.increment()
    fun incPreferredReadRemote() = incPreferredReadRemoteCounter.increment()
    fun incPutAllRetries() = incPutAllRetriesCounter.increment()
    fun incPutAllMsgsRetried() = incPutAllMsgsRetriedCounter.increment()
    fun incRemoveAllRetries() = incRemoveAllRetriesCounter.increment()
    fun incRemoveAllMsgsRetried() = incRemoveAllMsgsRetriedCounter.increment()
    fun incPartitionMessagesSent() = incPartitionMessagesSentCounter.increment()
    fun incBucketCount(bucketCount: Int) = incBucketCountGauge.increment(bucketCount.toDouble())
    fun incLowRedundancyBucketCount(`val`: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun incNoCopiesBucketCount(`val`: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun incTotalNumBuckets(`val`: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun incPrimaryBucketCount(`val`: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun incVolunteeringThreads(`val`: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun incPRMetaDataSentCount() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun incDataStoreEntryCount(amt: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun incBytesInUse(delta: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getVolunteeringInProgress(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startPartitionMessageProcessing(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endPartitionMessagesProcessing(start: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun setBucketCount(i: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    fun getDataStoreEntryCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    fun getDataStoreBytesInUse(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getTotalBucketCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getVolunteeringBecamePrimary(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getVolunteeringBecamePrimaryTime(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getVolunteeringOtherPrimary(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getVolunteeringOtherPrimaryTime(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getVolunteeringClosed(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getVolunteeringClosedTime(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startVolunteering(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endVolunteeringBecamePrimary(start: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endVolunteeringOtherPrimary(start: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endVolunteeringClosed(start: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getTotalNumBuckets(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    fun getPrimaryBucketCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    fun getVolunteeringThreads(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    fun getLowRedundancyBucketCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getNoCopiesBucketCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    fun getConfiguredRedundantCopies(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun setConfiguredRedundantCopies(`val`: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun setLocalMaxMemory(l: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getActualRedundantCopies(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun setActualRedundantCopies(`val`: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun putStartTime(key: Any?, startTime: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun removeStartTime(key: Any?): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endGetEntry(startTime: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endGetEntry(start: Long, numInc: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startRecovery(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endRecovery(start: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startBucketCreate(isRebalance: Boolean): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endBucketCreate(start: Long, success: Boolean, isRebalance: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startPrimaryTransfer(isRebalance: Boolean): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endPrimaryTransfer(start: Long, success: Boolean, isRebalance: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getBucketCreatesInProgress(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getBucketCreatesCompleted(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getBucketCreatesFailed(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getBucketCreateTime(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getPrimaryTransfersInProgress(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getPrimaryTransfersCompleted(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getPrimaryTransfersFailed(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getPrimaryTransferTime(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startRebalanceBucketCreate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endRebalanceBucketCreate(start: Long, end: Long, success: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startRebalancePrimaryTransfer() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endRebalancePrimaryTransfer(start: Long, end: Long, success: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getRebalanceBucketCreatesInProgress(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getRebalanceBucketCreatesCompleted(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getRebalanceBucketCreatesFailed(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getRebalanceBucketCreateTime(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getRebalancePrimaryTransfersInProgress(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getRebalancePrimaryTransfersCompleted(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getRebalancePrimaryTransfersFailed(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getRebalancePrimaryTransferTime(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startApplyReplication(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endApplyReplication(start: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startSendReplication(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endSendReplication(start: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startPutRemote(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endPutRemote(start: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun startPutLocal(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endPutLocal(start: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    fun getPRMetaDataSentCount(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}