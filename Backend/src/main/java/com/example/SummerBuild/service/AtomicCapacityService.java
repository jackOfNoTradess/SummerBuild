package com.example.SummerBuild.service;

import java.util.UUID;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AtomicCapacityService {

  private static final Logger logger = LoggerFactory.getLogger(AtomicCapacityService.class);

  @Autowired private RedissonClient redissonClient;

  private static final String CAPACITY_KEY_PREFIX = "event:capacity:";

  /** Initialize capacity for an event in Redis */
  public void initializeCapacity(UUID eventId, Integer initialCapacity) {
    String key = CAPACITY_KEY_PREFIX + eventId.toString();
    RAtomicLong atomicCapacity = redissonClient.getAtomicLong(key);
    atomicCapacity.set(initialCapacity != null ? initialCapacity : 0);
    logger.info("Initialized capacity for event {}: {}", eventId, initialCapacity);
  }

  /** Get current capacity for an event */
  public long getCurrentCapacity(UUID eventId) {
    String key = CAPACITY_KEY_PREFIX + eventId.toString();
    RAtomicLong atomicCapacity = redissonClient.getAtomicLong(key);
    return atomicCapacity.get();
  }

  /** Atomically increment capacity (when someone leaves an event) */
  public long incrementCapacity(UUID eventId) {
    String key = CAPACITY_KEY_PREFIX + eventId.toString();
    RAtomicLong atomicCapacity = redissonClient.getAtomicLong(key);
    long newValue = atomicCapacity.incrementAndGet();
    logger.info("Incremented capacity for event {}: {}", eventId, newValue);
    return newValue;
  }

  /**
   * Atomically decrement capacity (when someone joins an event) Returns true if successful, false
   * if capacity would go below 0
   */
  public boolean decrementCapacity(UUID eventId) {
    String key = CAPACITY_KEY_PREFIX + eventId.toString();
    RAtomicLong atomicCapacity = redissonClient.getAtomicLong(key);

    long currentValue = atomicCapacity.get();
    if (currentValue <= 0) {
      logger.warn("Cannot decrement capacity for event {} - already at 0", eventId);
      return false;
    }

    long newValue = atomicCapacity.decrementAndGet();
    if (newValue < 0) {
      // Rollback if we went negative
      atomicCapacity.incrementAndGet();
      logger.warn("Capacity decrement would result in negative value for event {}", eventId);
      return false;
    }

    logger.info("Decremented capacity for event {}: {}", eventId, newValue);
    return true;
  }

  /** Update capacity to a specific value */
  public void updateCapacity(UUID eventId, Integer newCapacity) {
    String key = CAPACITY_KEY_PREFIX + eventId.toString();
    RAtomicLong atomicCapacity = redissonClient.getAtomicLong(key);
    atomicCapacity.set(newCapacity != null ? newCapacity : 0);
    logger.info("Updated capacity for event {}: {}", eventId, newCapacity);
  }

  /** Delete capacity entry for an event */
  public void deleteCapacity(UUID eventId) {
    String key = CAPACITY_KEY_PREFIX + eventId.toString();
    RAtomicLong atomicCapacity = redissonClient.getAtomicLong(key);
    atomicCapacity.delete();
    logger.info("Deleted capacity entry for event {}", eventId);
  }
}
