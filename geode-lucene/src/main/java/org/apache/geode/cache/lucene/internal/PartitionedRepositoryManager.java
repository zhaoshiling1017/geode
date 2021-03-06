/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
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
package org.apache.geode.cache.lucene.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.geode.InternalGemFireError;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.RegionFunctionContext;
import org.apache.geode.cache.lucene.LuceneIndexDestroyedException;
import org.apache.geode.cache.lucene.LuceneSerializer;
import org.apache.geode.cache.lucene.internal.repository.IndexRepository;
import org.apache.geode.cache.lucene.internal.repository.RepositoryManager;
import org.apache.geode.internal.cache.BucketNotFoundException;
import org.apache.geode.internal.cache.BucketRegion;
import org.apache.geode.internal.cache.PartitionedRegion;
import org.apache.geode.internal.cache.execute.InternalRegionFunctionContext;

public class PartitionedRepositoryManager implements RepositoryManager {
  public static IndexRepositoryFactory indexRepositoryFactory = new IndexRepositoryFactory();
  /**
   * map of the parent bucket region to the index repository
   *
   * This is based on the BucketRegion in case a bucket is rebalanced, we don't want to return a
   * stale index repository. If a bucket moves off of this node and comes back, it will have a new
   * BucketRegion object.
   *
   * It is weak so that the old BucketRegion will be garbage collected.
   */
  protected final ConcurrentHashMap<Integer, IndexRepository> indexRepositories =
      new ConcurrentHashMap<Integer, IndexRepository>();

  /** The user region for this index */
  protected PartitionedRegion userRegion = null;
  protected final LuceneSerializer serializer;
  protected final InternalLuceneIndex index;
  protected volatile boolean closed;
  private final CountDownLatch isDataRegionReady = new CountDownLatch(1);

  public PartitionedRepositoryManager(InternalLuceneIndex index, LuceneSerializer serializer) {
    this.index = index;
    this.serializer = serializer;
    this.closed = false;
  }

  public void setUserRegionForRepositoryManager(PartitionedRegion userRegion) {
    this.userRegion = userRegion;
  }

  @Override
  public Collection<IndexRepository> getRepositories(RegionFunctionContext ctx)
      throws BucketNotFoundException {
    Region<Object, Object> region = ctx.getDataSet();
    Set<Integer> buckets = ((InternalRegionFunctionContext) ctx).getLocalBucketSet(region);
    ArrayList<IndexRepository> repos = new ArrayList<IndexRepository>(buckets.size());
    for (Integer bucketId : buckets) {
      BucketRegion userBucket = userRegion.getDataStore().getLocalBucketById(bucketId);
      if (userBucket == null) {
        throw new BucketNotFoundException(
            "User bucket was not found for region " + region + "bucket id " + bucketId);
      } else {
        repos.add(getRepository(userBucket.getId()));
      }
    }

    return repos;
  }

  @Override
  public IndexRepository getRepository(Region region, Object key, Object callbackArg)
      throws BucketNotFoundException {
    BucketRegion userBucket = userRegion.getBucketRegion(key, callbackArg);
    if (userBucket == null) {
      throw new BucketNotFoundException("User bucket was not found for region " + region + "key "
          + key + " callbackarg " + callbackArg);
    }

    return getRepository(userBucket.getId());
  }

  /**
   * Return the repository for a given user bucket
   */
  protected IndexRepository getRepository(Integer bucketId) throws BucketNotFoundException {
    IndexRepository repo = indexRepositories.get(bucketId);
    if (repo != null && !repo.isClosed()) {
      return repo;
    }

    repo = computeRepository(bucketId);

    if (repo == null) {
      throw new BucketNotFoundException(
          "Unable to find lucene index because no longer primary for bucket " + bucketId);
    }
    return repo;
  }

  protected IndexRepository computeRepository(Integer bucketId, LuceneSerializer serializer,
      InternalLuceneIndex index, PartitionedRegion userRegion, IndexRepository oldRepository)
      throws IOException {
    return indexRepositoryFactory.computeIndexRepository(bucketId, serializer, index, userRegion,
        oldRepository);
  }


  protected IndexRepository computeRepository(Integer bucketId) {
    try {
      isDataRegionReady.await();
    } catch (InterruptedException e) {
      throw new InternalGemFireError("Unable to create index repository", e);
    }
    IndexRepository repo = indexRepositories.compute(bucketId, (key, oldRepository) -> {
      try {
        if (closed) {
          if (oldRepository != null) {
            oldRepository.cleanup();
          }
          throw new LuceneIndexDestroyedException(index.getName(), index.getRegionPath());
        }
        return computeRepository(bucketId, serializer, index, userRegion, oldRepository);
      } catch (IOException e) {
        throw new InternalGemFireError("Unable to create index repository", e);
      }
    });
    return repo;
  }

  protected void allowRepositoryComputation() {
    isDataRegionReady.countDown();
  }


  @Override
  public void close() {
    this.closed = true;
    for (Integer bucketId : indexRepositories.keySet()) {
      try {
        computeRepository(bucketId);
      } catch (LuceneIndexDestroyedException e) {
        /* expected exception */}
    }
  }
}
