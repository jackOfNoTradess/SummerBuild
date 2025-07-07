package com.example.SummerBuild.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class DistributedLockService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedLockService.class);

    @Autowired
    private RedissonClient redissonClient;

    private static final long LOCK_WAIT_TIME = 10; // seconds
    private static final long LOCK_LEASE_TIME = 30; // seconds

    public <T> T executeWithLock(String lockKey, Supplier<T> operation) {
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

            if (acquired) {
                logger.info("Lock acquired for key: {}", lockKey);
                return operation.get();
            } else {
                logger.warn("Failed to acquire lock for key: {}", lockKey);
                throw new RuntimeException("Could not acquire lock for: " + lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted while waiting for lock: {}", lockKey, e);
            throw new RuntimeException("Lock operation interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.info("Lock released for key: {}", lockKey);
            }
        }
    }
}