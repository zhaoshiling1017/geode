package org.apache.geode.cache.function.internal

import org.apache.geode.cache.Region
import org.apache.geode.cache.execute.Execution
import org.apache.geode.cache.execute.Function
import org.apache.geode.cache.execute.FunctionException
import org.apache.geode.cache.execute.internal.FunctionServiceManager
import org.apache.geode.cache.execute.internal.FunctionServiceManager.RANDOM_onMember
import org.apache.geode.cache.function.FunctionService
import org.apache.geode.cache.partition.PartitionRegionHelper
import org.apache.geode.distributed.DistributedMember
import org.apache.geode.distributed.DistributedSystem
import org.apache.geode.internal.cache.execute.DistributedRegionFunctionExecutor
import org.apache.geode.internal.cache.execute.MemberFunctionExecutor
import org.apache.geode.internal.cache.execute.PartitionedRegionFunctionExecutor
import org.apache.geode.internal.i18n.LocalizedStrings
import java.util.*

class FunctionServiceImpl(val distributedSystem: DistributedSystem) : FunctionService {
    private val functionServiceManager = FunctionServiceManager()

    override fun onRegion(region: Region<*, *>?): Execution<*, *, *> {
        region?.let {
            return if (PartitionRegionHelper.isPartitionedRegion(it)) {
                PartitionedRegionFunctionExecutor(it)
            } else {
                DistributedRegionFunctionExecutor(it)
            }
        }
        throw FunctionException(
                LocalizedStrings.FunctionService_0_PASSED_IS_NULL.toLocalizedString("Region instance "))
    }

    override fun onMember(distributedMember: DistributedMember?): Execution<*, *, *> {
        if (distributedMember == null) {
            throw FunctionException(LocalizedStrings.FunctionService_0_PASSED_IS_NULL
                    .toLocalizedString("DistributedMember instance "))
        }
        return MemberFunctionExecutor(distributedSystem, distributedMember)
    }

    override fun onMembers(vararg groups: String?): Execution<*, *, *> {
        if (groups.isEmpty()) {
            return MemberFunctionExecutor(distributedSystem)
        }
        val members = HashSet<DistributedMember>()
        for (group in groups) {
            members.addAll(distributedSystem.getGroupMembers(group))
        }
        if (members.isEmpty()) {
            throw FunctionException(LocalizedStrings.FunctionService_NO_MEMBERS_FOUND_IN_GROUPS
                    .toLocalizedString(Arrays.toString(groups)))
        }
        return MemberFunctionExecutor(distributedSystem, members)
    }

    override fun onMembers(distributedMembers: Set<DistributedMember>?): Execution<*, *, *> {
        if (distributedMembers == null) {
            throw FunctionException(LocalizedStrings.FunctionService_0_PASSED_IS_NULL
                    .toLocalizedString("distributedMembers set "))
        }
        return MemberFunctionExecutor(distributedSystem, distributedMembers)
    }

    override fun onMember(vararg groups: String?): Execution<*, *, *> {
        val members = HashSet<DistributedMember>()
        for (group in groups) {
            val grpMembers = ArrayList<DistributedMember>(distributedSystem.getGroupMembers(group))
            if (!grpMembers.isEmpty()) {
                if (!RANDOM_onMember && grpMembers.contains(distributedSystem.distributedMember)) {
                    members.add(distributedSystem.distributedMember)
                } else {
                    grpMembers.shuffle()
                    members.add(grpMembers[0])
                }
            }
        }
        if (members.isEmpty()) {
            throw FunctionException(LocalizedStrings.FunctionService_NO_MEMBERS_FOUND_IN_GROUPS
                    .toLocalizedString(Arrays.toString(groups)))
        }
        return MemberFunctionExecutor(distributedSystem, members)
    }


    override fun getFunction(functionId: String?): Function<*> = functionServiceManager.getFunction(functionId)
    override fun registerFunction(function: Function<*>?) = functionServiceManager.registerFunction(function)
    override fun unregisterFunction(functionId: String?) = functionServiceManager.unregisterFunction(functionId)
    override fun isRegistered(functionId: String?): Boolean = functionServiceManager.isRegistered(functionId)
    override fun getRegisteredFunctions(): MutableMap<String, Function<Any>> = functionServiceManager.registeredFunctions
}