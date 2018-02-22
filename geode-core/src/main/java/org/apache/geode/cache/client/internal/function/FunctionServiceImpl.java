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
package org.apache.geode.cache.client.internal.function;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionService;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.client.PoolManager;
import org.apache.geode.cache.client.function.FunctionService;
import org.apache.geode.cache.client.internal.ProxyCache;
import org.apache.geode.cache.client.internal.ProxyRegion;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionException;
import org.apache.geode.internal.cache.InternalRegion;
import org.apache.geode.internal.cache.execute.ServerFunctionExecutor;
import org.apache.geode.internal.cache.execute.ServerRegionFunctionExecutor;
import org.apache.geode.internal.i18n.LocalizedStrings;

import java.util.Map;

public class FunctionServiceImpl implements FunctionService {

  @Override
  public Execution onRegion(Region region) {
    if (region == null) {
      throw new FunctionException(
          LocalizedStrings.FunctionService_0_PASSED_IS_NULL.toLocalizedString("Region instance "));
    }
    ProxyCache proxyCache = null;
    String poolName = region.getAttributes().getPoolName();
    if (poolName != null) {
      Pool pool = PoolManager.find(poolName);
      if (pool.getMultiuserAuthentication()) {
        if (region instanceof ProxyRegion) {
          ProxyRegion proxyRegion = (ProxyRegion) region;
          region = proxyRegion.getRealRegion();
          proxyCache = proxyRegion.getAuthenticatedCache();
        } else {
          throw new UnsupportedOperationException();
        }
      }
    }

    if (isClientRegion(region)) {
      return new ServerRegionFunctionExecutor(region, proxyCache);
    }
    throw new FunctionException("Cannot find suitable Function Executor");
  }

  @Override
  public Execution onServer(Pool pool) {
    if (pool == null) {
      throw new FunctionException(
          LocalizedStrings.FunctionService_0_PASSED_IS_NULL.toLocalizedString("Pool instance "));
    }

    if (pool.getMultiuserAuthentication()) {
      throw new UnsupportedOperationException();
    }

    return new ServerFunctionExecutor(pool, false, null);
  }

  @Override
  public Execution onServers(Pool pool) {
    if (pool == null) {
      throw new FunctionException(
          LocalizedStrings.FunctionService_0_PASSED_IS_NULL.toLocalizedString("Pool instance "));
    }

    if (pool.getMultiuserAuthentication()) {
      throw new UnsupportedOperationException();
    }

    return new ServerFunctionExecutor(pool, true, null);
  }

  @Override
  public Execution onServer(RegionService regionService) {
    return null;
  }

  @Override
  public Execution onServers(RegionService regionService) {
    return null;
  }

  @Override
  public Function getFunction(String functionId) {
    return null;
  }

  @Override
  public void registerFunction(Function function) {

  }

  @Override
  public void unregisterFunction(String functionId) {

  }

  @Override
  public boolean isRegistered(String functionId) {
    return false;
  }

  @Override
  public Map<String, Function> getRegisteredFunctions() {
    return null;
  }

  /**
   * @return true if the method is called on a region has a {@link Pool}.
   * @since GemFire 6.0
   */
  private boolean isClientRegion(Region region) {
    return ((InternalRegion) region).hasServerProxy();

  }
}
