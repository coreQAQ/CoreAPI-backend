package com.core.coreapi.consumer;

import com.core.coreapi.model.entity.ApiAccessLog;
import com.core.coreapi.model.entity.ApiDoc;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ApiLogConsumer {

    private final String topic = "api_log";

    @Resource
    private KafkaTemplate<String, String> template;

    @Resource
    private ElasticsearchRestTemplate esTemplate;

    @Resource
    private ObjectMapper mapper;

    @KafkaListener(topics = "api_log", groupId = "main")
    public void batchConsume(List<String> messages) {
        // 反序列化
        List<ApiAccessLog> logs = messages.stream()
                .map(message -> {
                    try {
                        return mapper.readValue(message, ApiAccessLog.class);
                    } catch (JsonProcessingException e) {
                        log.error(e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        esTemplate.save(logs);
    }

}
