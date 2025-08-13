package com.core.coreapigateway.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedisConfig {

    private Integer database;
    private String host;
    private String port;
    private Integer timeout;
    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
//        config.setCodec(new JsonJacksonCodec());
        config.useSingleServer()
                .setDatabase(database)
                .setAddress("redis://" + host + ":" + port)
                .setTimeout(timeout)
                .setPassword(password);
        return Redisson.create(config);
    }

}