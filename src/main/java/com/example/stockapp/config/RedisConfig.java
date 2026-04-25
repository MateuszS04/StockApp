package com.example.stockapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisConfig {

    // Auto creation of redisConnectionFactory, convenient way to read and write string based values
    //as stock quantities are numeric and serialize cleanly

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory){
        return new StringRedisTemplate(connectionFactory);
    }
    /**
     * Loads the trade.lua once at startup. Spring Data Redi will script lload it into Redis
     * on first execution and reuse the hash for subsequent evalsha calls
     */

     @Bean
    public RedisScript<Long> tradeScript(){
         DefaultRedisScript<Long> script= new DefaultRedisScript<>();
         script.setLocation(new ClassPathResource("scripts/trade.lua"));
         script.setResultType(Long.class);
         return script;
     }
}
