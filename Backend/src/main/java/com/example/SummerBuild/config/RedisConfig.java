package com.example.SummerBuild.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

  @Value("${spring.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.redis.port:6379}")
  private int redisPort;

  @Bean
  public RedissonClient redissonClient() {
    Config config = new Config();
    config
        .useSingleServer()
        .setAddress("redis://" + redisHost + ":" + redisPort)
        .setConnectionMinimumIdleSize(1)
        .setConnectionPoolSize(10)
        .setRetryAttempts(3)
        .setRetryInterval(1500);

    return Redisson.create(config);
  }
}
