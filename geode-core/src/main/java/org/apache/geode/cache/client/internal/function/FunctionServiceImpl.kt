/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geode.cache.client.internal.function

import org.apache.geode.cache.Region
import org.apache.geode.cache.RegionService
import org.apache.geode.cache.client.Pool
import org.apache.geode.cache.client.PoolManager
import org.apache.geode.cache.client.function.FunctionService
import org.apache.geode.cache.client.internal.InternalClientCache
import org.apache.geode.cache.client.internal.ProxyCache
import org.apache.geode.cache.client.internal.ProxyRegion
import org.apache.geode.cache.execute.Execution
import org.apache.geode.cache.execute.Function
import org.apache.geode.cache.execute.FunctionException
import org.apache.geode.cache.execute.internal.FunctionServiceManager
import org.apache.geode.internal.cache.InternalRegion
import org.apache.geode.internal.cache.execute.ServerFunctionExecutor
import org.apache.geode.internal.cache.execute.ServerRegionFunctionExecutor
import org.apache.geode.internal.i18n.LocalizedStrings

class FunctionServiceImpl : FunctionService {
    private val functionServiceManager = FunctionServiceManager()

    override fun onRegion(region: Region<*, *>?): Execution<*, *, *> {
        region?.let {
            var proxyCache: ProxyCache? = null
            var tmpRegion: Region<*, *> = it

            val poolName = it.attributes.poolName
            if (poolName != null) {
                val pool = PoolManager.find(poolName)
                if (pool.multiuserAuthentication) {
                    if (it is ProxyRegion) {
                        tmpRegion = it.realRegion
                        proxyCache = it.authenticatedCache
                    } else {
                        throw UnsupportedOperationException()
                    }
                }
            }
            if (tmpRegion is InternalRegion && isClientRegion(tmpRegion)) {
                return ServerRegionFunctionExecutor(region, proxyCache)
            }
            throw FunctionException("Cannot find suitable Function Executor")
        }
        throw FunctionException(LocalizedStrings.FunctionService_0_PASSED_IS_NULL.toLocalizedString("Region instance "))
    }

    override fun onServer(pool: Pool?): Execution<*, *, *> =
            createServerFunctionExecutorForPool(pool, false)

    override fun onServers(pool: Pool?): Execution<*, *, *> =
            createServerFunctionExecutorForPool(pool, true)

    override fun onServer(regionService: RegionService?): Execution<*, *, *> =
            createServerFunctionExecutorForRegionService(regionService, false)

    override fun onServers(regionService: RegionService?): Execution<*, *, *> =
            createServerFunctionExecutorForRegionService(regionService, true)

    private fun createServerFunctionExecutorForPool(pool: Pool?, runOnAllServers: Boolean, proxyCache: ProxyCache? = null): Execution<*, *, *> {
        pool?.let {
            if (it.multiuserAuthentication) {
                throw UnsupportedOperationException()
            }

            return ServerFunctionExecutor(pool, runOnAllServers, proxyCache, *returnServerGroupAsTypeArray(it.serverGroup))
        }
        throw FunctionException(
                LocalizedStrings.FunctionService_0_PASSED_IS_NULL.toLocalizedString("Pool instance "))
    }

    private fun createServerFunctionExecutorForRegionService(regionService: RegionService?, runOnAllServers: Boolean)
            : Execution<*, *, *> {
        regionService?.let {
            if (it is InternalClientCache) {
                if (!it.isClient) {
                    throw FunctionException("The cache was not a client cache")
                } else {
                    it.defaultPool?.let {
                        return createServerFunctionExecutorForPool(it, runOnAllServers)
                    }
                    throw FunctionException("The client cache does not have a default pool")
                }
            } else {
                val proxyCache = it as ProxyCache
                val pool = proxyCache.userAttributes.pool
                return createServerFunctionExecutorForPool(pool, runOnAllServers, proxyCache)
            }
        }
        throw FunctionException(LocalizedStrings.FunctionService_0_PASSED_IS_NULL
                .toLocalizedString("RegionService instance "))
    }

    private fun returnServerGroupAsTypeArray(serverGroup: String): Array<String> = serverGroup.split(",").toTypedArray()


    override fun getFunction(functionId: String?): Function<*> = functionServiceManager.getFunction(functionId)
    override fun registerFunction(function: Function<*>?) = functionServiceManager.registerFunction(function)
    override fun unregisterFunction(functionId: String?) = functionServiceManager.unregisterFunction(functionId)
    override fun isRegistered(functionId: String?): Boolean = functionServiceManager.isRegistered(functionId)
    override fun getRegisteredFunctions(): MutableMap<String, Function<Any>> = functionServiceManager.registeredFunctions

    /**
     * @return true if the method is called on a region has a [Pool].
     * @since GemFire 6.0
     */
    private fun isClientRegion(region: InternalRegion): Boolean = region.hasServerProxy()
}
