package com.core.coreapi.config;

import cn.hutool.core.io.resource.ResourceUtil;
import com.maxmind.geoip2.DatabaseReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class MMDBConfig {

    @Bean
    public DatabaseReader databaseReader() {
        try (InputStream database = ResourceUtil.getStream("GeoLite2-City.mmdb")) {
            return new DatabaseReader.Builder(database).build();
        } catch (IOException e) {
            log.error("读取IP数据库失败");
            throw new RuntimeException(e);
        }
    }

}
