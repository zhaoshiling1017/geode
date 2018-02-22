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
package org.apache.geode.cache.common.function;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionService;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionException;
import org.apache.geode.cache.execute.FunctionService;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

public interface FunctionServiceBase {
  /**
   * Returns an {@link Execution} object that can be used to execute a data dependent function on
   * the specified Region.<br>
   * When invoked from a GemFire client, the method returns an Execution instance that sends a
   * message to one of the connected servers as specified by the {@link Pool} for the region. <br>
   * Depending on the filters setup on the {@link Execution}, the function is executed on all
   * GemFire members that define the data region, or a subset of members.
   * {@link Execution#withFilter(Set)}).
   *
   * For DistributedRegions with DataPolicy.NORMAL, it throws UnsupportedOperationException. For
   * DistributedRegions with DataPolicy.EMPTY, execute the function on any random member which has
   * DataPolicy.REPLICATE <br>
   * . For DistributedRegions with DataPolicy.REPLICATE, execute the function locally. For Regions
   * with DataPolicy.PARTITION, it executes on members where the data resides as specified by the
   * filter.
   * @return Execution
   * @throws FunctionException if the region passed in is null
   * @since GemFire 6.0
   */
  Execution onRegion(Region region);

  /**
   * Returns the {@link Function} defined by the functionId, returns null if no function is found
   * for the specified functionId
   * @return Function
   * @throws FunctionException if functionID passed is null
   * @since GemFire 6.0
   */
  Function getFunction(String functionId);

  /**
   * Registers the given {@link Function} with the {@link FunctionService} using
   * {@link Function#getId()}.
   * <p>
   * Registering a function allows execution of the function using
   * {@link Execution#execute(String)}. Every member that could execute a function using its
   * {@link Function#getId()} should register the function.
   * </p>
   * @throws FunctionException if function instance passed is null or Function.getId() returns null
   * @since GemFire 6.0
   */
  void registerFunction(Function function);

  /**
   * Unregisters the given {@link Function} with the {@link FunctionService} using
   * {@link Function#getId()}.
   * <p>
   * @throws FunctionException if function instance passed is null or Function.getId() returns null
   * @since GemFire 6.0
   */
  void unregisterFunction(String functionId);

  /**
   * Returns true if the fun ction is registered to FunctionService
   * @throws FunctionException if function instance passed is null or Function.getId() returns null
   * @since GemFire 6.0
   */
  boolean isRegistered(String functionId);

  /**
   * Returns all locally registered functions
   * @return A view of registered functions as a Map of {@link Function#getId()} to {@link Function}
   * @since GemFire 6.0
   */
  Map<String, Function> getRegisteredFunctions();
}
