package com.core.coreapi.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Document(indexName = "api_log")
@AllArgsConstructor
@NoArgsConstructor
public class ApiAccessLog {
    private String ak;
    private String uri;
    private String method;
    private int statusCode;
    private long duration;         // 耗时（ms）
    private String ip;
    private String userAgent;
    private String timestamp;      // ISO格式时间，如 2025-06-05T15:00:00Z
}
