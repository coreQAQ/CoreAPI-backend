package com.core.coreapi.model.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "地址实体类")
public class Address {

    @Schema(description = "省份", example = "江苏省")
    private String province;

    @Schema(description = "城市", example = "无锡")
    private String city;

}
