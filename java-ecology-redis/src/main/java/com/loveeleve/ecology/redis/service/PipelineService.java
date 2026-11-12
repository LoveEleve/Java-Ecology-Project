package com.loveeleve.ecology.redis.service;

import com.loveeleve.ecology.redis.config.RedisKeys;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PipelineService {

    private final StringRedisTemplate redisTemplate;

    public PipelineService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void write(String name, Map<String, String> values, Duration ttl) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisStringCommands commands = connection.stringCommands();
            values.forEach((key, value) -> commands.set(
                    RedisKeys.pipeline(name + ":" + key).getBytes(StandardCharsets.UTF_8),
                    value.getBytes(StandardCharsets.UTF_8),
                    Expiration.from(ttl),
                    RedisStringCommands.SetOption.UPSERT));
            return null;
        });
    }

    public Map<String, String> read(String name, List<String> keys) {
        List<Object> values = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisStringCommands commands = connection.stringCommands();
            keys.forEach(key -> commands.get(RedisKeys.pipeline(name + ":" + key).getBytes(StandardCharsets.UTF_8)));
            return null;
        });
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            Object value = values.get(i);
            result.put(keys.get(i), value == null ? null : value.toString());
        }
        return result;
    }
}
