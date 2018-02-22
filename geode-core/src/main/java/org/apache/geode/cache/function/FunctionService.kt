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
package org.apache.geode.cache.function

import org.apache.geode.cache.common.function.FunctionServiceBase
import org.apache.geode.cache.execute.Execution
import org.apache.geode.cache.execute.Function
import org.apache.geode.cache.execute.FunctionException
import org.apache.geode.distributed.DistributedMember

interface FunctionService : FunctionServiceBase {
    /**
     * Returns an [Execution] object that can be used to execute a data independent function on
     * a [DistributedMember]. If the member is not found, executing the function will throw an
     * Exception. If the member goes down while dispatching or executing the function on the member,
     * an Exception will be thrown.
     * @param distributedMember defines a member in the distributed system
     * @return Execution
     * @throws FunctionException if distributedMember is null
     * @since GemFire 7.0
     */
    fun onMember(distributedMember: DistributedMember?): Execution<*, *, *>

    /**
     * Returns an [Execution] object that can be used to execute a data independent function on
     * all peer members. If the optional groups parameter is provided, function is executed on all
     * members that belong to the provided groups.
     *
     *
     * If one of the members goes down while dispatching or executing the function on the member, an
     * Exception will be thrown.
     * @param groups optional list of GemFire configuration property "groups" (see
     * [ `groups`](../../distributed/DistributedSystem.html#groups)) on
     * which to execute the function. Function will be executed on all members of each group
     * @return Execution
     * @throws FunctionException if no members are found belonging to the provided groups
     * @since GemFire 7.0
     */
    fun onMembers(vararg groups: String?): Execution<*, *, *>

    /**
     * Returns an [Execution] object that can be used to execute a data independent function on
     * the set of [DistributedMember]s. If one of the members goes down while dispatching or
     * executing the function, an Exception will be thrown.
     * @param distributedMembers set of distributed members on which [Function] to be executed
     * @throws FunctionException if distributedMembers is null
     * @since GemFire 7.0
     */
    fun onMembers(distributedMembers: Set<DistributedMember>?): Execution<*, *, *>

    /**
     * Returns an [Execution] object that can be used to execute a data independent function on
     * one member of each group provided.
     * @param groups list of GemFire configuration property "groups" (see
     * [ `groups`](../../distributed/DistributedSystem.html#groups)) on
     * which to execute the function. Function will be executed on one member of each group
     * @return Execution
     * @throws FunctionException if no members are found belonging to the provided groups
     * @since GemFire 7.0
     */
    fun onMember(vararg groups: String?): Execution<*, *, *>
}
