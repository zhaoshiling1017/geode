/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.cache.execute.internal;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionException;

import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.distributed.internal.DistributionConfig;
import org.apache.geode.internal.InternalEntity;
import org.apache.geode.internal.i18n.LocalizedStrings;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides the entry point into execution of user defined {@linkplain Function}s.
 * <p>
 * Function execution provides a means to route application behaviour to {@linkplain Region data} or
 * more generically to peers in a {@link DistributedSystem} or servers in a {@link Pool}.
 * </p>
 *
 * While {@link FunctionService} is a customer facing interface to this functionality, all of the
 * work is done here. In addition, internal only functionality is exposed in this class.
 *
 * @since GemFire 7.0
 */
public class FunctionServiceManager {

  private static final ConcurrentHashMap<String, Function> idToFunctionMap =
      new ConcurrentHashMap<>();

  /**
   * use when the optimization to execute onMember locally is not desired.
   */
  public static final boolean RANDOM_onMember =
      Boolean.getBoolean(DistributionConfig.GEMFIRE_PREFIX + "randomizeOnMember");

  public FunctionServiceManager() {
    // do nothing
  }

  /**
   * Returns the {@link Function} defined by the functionId, returns null if no function is found
   * for the specified functionId
   *
   * @return Function
   * @throws FunctionException if functionID passed is null
   * @since GemFire 6.0
   */
  public Function getFunction(String functionId) {
    if (functionId == null) {
      throw new FunctionException(LocalizedStrings.FunctionService_0_PASSED_IS_NULL
          .toLocalizedString("functionId instance "));
    }
    return idToFunctionMap.get(functionId);
  }

  /**
   * Registers the given {@link Function} with the {@link FunctionService} using
   * {@link Function#getId()}.
   * <p>
   * Registering a function allows execution of the function using
   * {@link Execution#execute(String)}. Every member that could execute a function using its
   * {@link Function#getId()} should register the function.
   * </p>
   *
   * @throws FunctionException if function instance passed is null or Function.getId() returns null
   * @since GemFire 6.0
   */
  public void registerFunction(Function function) {
    if (function == null) {
      throw new FunctionException(LocalizedStrings.FunctionService_0_PASSED_IS_NULL
          .toLocalizedString("function instance "));
    }
    if (function.getId() == null) {
      throw new FunctionException(
          LocalizedStrings.FunctionService_FUNCTION_GET_ID_RETURNED_NULL.toLocalizedString());
    }
    if (function.isHA() && !function.hasResult()) {
      throw new FunctionException(
          LocalizedStrings.FunctionService_FUNCTION_ATTRIBUTE_MISMATCH.toLocalizedString());
    }

    idToFunctionMap.put(function.getId(), function);
  }

  /**
   * Unregisters the given {@link Function} with the {@link FunctionService} using
   * {@link Function#getId()}.
   * <p>
   *
   * @throws FunctionException if function instance passed is null or Function.getId() returns null
   * @since GemFire 6.0
   */
  public void unregisterFunction(String functionId) {
    if (functionId == null) {
      throw new FunctionException(LocalizedStrings.FunctionService_0_PASSED_IS_NULL
          .toLocalizedString("functionId instance "));
    }
    idToFunctionMap.remove(functionId);
  }

  /**
   * Returns true if the function is registered to FunctionService
   *
   * @throws FunctionException if function instance passed is null or Function.getId() returns null
   * @since GemFire 6.0
   */
  public boolean isRegistered(String functionId) {
    if (functionId == null) {
      throw new FunctionException(LocalizedStrings.FunctionService_0_PASSED_IS_NULL
          .toLocalizedString("functionId instance "));
    }
    return idToFunctionMap.containsKey(functionId);
  }

  /**
   * Returns all locally registered functions
   *
   * @return A view of registered functions as a Map of {@link Function#getId()} to {@link Function}
   * @since GemFire 6.0
   */
  public Map<String, Function> getRegisteredFunctions() {
    // We have to remove the internal functions before returning the map to the users
    final Map<String, Function> tempIdToFunctionMap = new HashMap<String, Function>();
    for (Map.Entry<String, Function> entry : idToFunctionMap.entrySet()) {
      if (!(entry.getValue() instanceof InternalEntity)) {
        tempIdToFunctionMap.put(entry.getKey(), entry.getValue());
      }
    }
    return tempIdToFunctionMap;
  }

  public void unregisterAllFunctions() {
    // Unregistering all the functions registered with the FunctionService.
    Map<String, Function> functions = new HashMap<String, Function>(idToFunctionMap);
    for (String functionId : idToFunctionMap.keySet()) {
      unregisterFunction(functionId);
    }
  }
}
