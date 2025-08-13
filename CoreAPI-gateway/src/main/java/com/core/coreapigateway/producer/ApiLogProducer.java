package com.core.coreapigateway.producer;

import com.core.coreapigateway.entity.ApiAccessLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class ApiLogProducer {

    private final String topic = "api_log";

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private KafkaTemplate<String, String> template;

    public void send(ApiAccessLog apiLog) {
        try {
            String json = objectMapper.writeValueAsString(apiLog);
            template.send(topic, json);
        } catch (JsonProcessingException e) {
            log.warn("写入es失败：{}", apiLog);
        }
    }

}
