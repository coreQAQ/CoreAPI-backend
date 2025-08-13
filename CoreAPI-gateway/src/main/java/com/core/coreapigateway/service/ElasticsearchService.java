package com.core.coreapigateway.service;

import com.core.coreapigateway.entity.ApiAccessLog;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ElasticsearchService {

    @Resource
    private ElasticsearchRestTemplate template;

    public void saveLog(ApiAccessLog log) {
        template.save(log);
    }
}
