package com.core.coreapi.model.entity;

import com.maxmind.geoip2.model.CityResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Geo-IP 精简信息
 */
@Data
@Schema(description = "IP归属地精简信息")
public class IpDetail implements Serializable {

    @Schema(description = "客户端 IP", example = "112.21.20.238")
    private String ip;

    @Schema(description = "国家 ISO 代码，如 CN、US", example = "CN")
    private String countryCode;

    @Schema(description = "国家中文名", example = "中国")
    private String countryName;

    @Schema(description = "省份/州代码，例如 JS（江苏）", example = "JS")
    private String provinceCode;

    @Schema(description = "省份中文名", example = "江苏")
    private String provinceName;

    @Schema(description = "城市中文名", example = "无锡市")
    private String cityName;

    @Schema(description = "纬度", example = "31.5618")
    private Double latitude;

    @Schema(description = "经度", example = "120.2864")
    private Double longitude;

    @Schema(description = "时区标识，例如 Asia/Shanghai", example = "Asia/Shanghai")
    private String timeZone;

    private static final long serialVersionUID = 1L;

    /**
     * 从 MaxMind GeoIP 的 CityResponse 映射生成简要信息
     */
    public static IpDetail parse(CityResponse response) {
        IpDetail info = new IpDetail();
        if (response == null) {
            return info;
        }

        // IP
        if (response.getTraits() != null) {
            info.setIp(response.getTraits().getIpAddress());
        }

        // Country
        if (response.getCountry() != null) {
            info.setCountryCode(response.getCountry().getIsoCode());
            if (response.getCountry().getNames() != null) {
                info.setCountryName(response.getCountry().getNames().get("zh-CN"));
            }
        }

        // Province
        if (response.getSubdivisions() != null && !response.getSubdivisions().isEmpty()) {
            if (response.getSubdivisions().get(0) != null) {
                info.setProvinceCode(response.getSubdivisions().get(0).getIsoCode());
                if (response.getSubdivisions().get(0).getNames() != null) {
                    info.setProvinceName(response.getSubdivisions().get(0).getNames().get("zh-CN"));
                }
            }
        }

        // City
        if (response.getCity() != null && response.getCity().getNames() != null) {
            info.setCityName(response.getCity().getNames().get("zh-CN"));
        }

        // Location
        if (response.getLocation() != null) {
            info.setLatitude(response.getLocation().getLatitude());
            info.setLongitude(response.getLocation().getLongitude());
            info.setTimeZone(response.getLocation().getTimeZone());
        }

        return info;
    }
}
