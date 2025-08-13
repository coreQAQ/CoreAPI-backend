package com.core.coreapi.model.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "用户实体类")
@Data
public class User {

    @Schema(description = "用户名", example = "core")
    private String name;

    @Schema(description = "账号", example = "a22307118")
    private String account;

    @Schema(description = "地址")
    private Address address;

    // 数组类型的示例值需要为标准json格式
    @Schema(description = "兴趣", example = "[\"打游戏\", \"看书\", \"旅游\"]")
    private List<String> hobbies;

}
