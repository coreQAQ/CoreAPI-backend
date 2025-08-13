package com.core.coreapi.controller;

import cn.hutool.core.lang.Validator;
import com.core.coreapi.exception.BusinessException;
import com.core.coreapi.model.dto.ValidateRequest;
import com.core.coreapi.model.entity.IpDetail;
import com.core.coreapi.model.entity.PhoneLocation;
import com.core.coreapi.model.entity.ValidateResult;
import com.core.coreapi.model.entity.User;
import com.core.coreapi.service.PhoneLocationService;
import com.core.coreapi.shared.common.BaseResponse;
import com.core.coreapi.shared.common.ErrorCode;
import com.core.coreapi.shared.common.ResultUtils;
import com.core.coreapi.util.CommonUtils;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1")
@Tag(name = "v1接口")
@Slf4j
public class V1Controller {

    @Resource
    private DatabaseReader reader;

    @Resource
    private PhoneLocationService phoneLocationService;

    /**
     * 获取ip详细信息
     *
     * @param ip
     * @return
     */
    @GetMapping("/ip")
    @Parameter(name = "ip", description = "IP 地址", example = "112.21.20.238")
    public BaseResponse<IpDetail> getIpDetail(@RequestParam String ip) {
        if (StringUtils.isBlank(ip))
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse cityResponse = reader.city(ipAddress);
//            log.info(cityResponse.toJson());
            return ResultUtils.success(IpDetail.parse(cityResponse));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        } catch (GeoIp2Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    /**
     * 查询手机号归属地
     * @param phone
     * @return
     */
    @GetMapping("/phone")
    @Parameter(name = "phone", description = "手机号(前7位或更多)", example = "1381995")
    public BaseResponse<PhoneLocation> getPhoneLocation(@RequestParam String phone) {
        if (StringUtils.isBlank(phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!Validator.isNumber(phone) || phone.length() < 7) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "格式错误");
        }
        String segment = phone.substring(0, 7);
        PhoneLocation phoneLocation = phoneLocationService.lambdaQuery()
                .like(PhoneLocation::getPhone, segment)
                .one();
        if (phoneLocation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未知号码");
        }
//        log.info(phoneLocation.toString());
        return ResultUtils.success(phoneLocation);
    }

    /**
     * 验证数据格式
     * @param validateRequest
     * @return
     */
    @PostMapping("/validate")
    public BaseResponse<ValidateResult> regexValidate(@RequestBody ValidateRequest validateRequest) {
        String type = validateRequest.getType();
        List<String> values = validateRequest.getValues();

        if (!CommonUtils.isSupportedType(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的校验类型: " + type);
        }

        List<Boolean> results = new ArrayList<>();
        for (String value : values) {
            boolean valid = switch (type) {
                case "email"     -> Validator.isEmail(value);
                case "mobile"    -> Validator.isMobile(value);
                case "chinese"   -> Validator.isChinese(value);
                case "idcard"    -> Validator.isCitizenId(value);
                case "url"       -> Validator.isUrl(value);
                case "mac"       -> Validator.isMac(value);
                case "ipv4"      -> Validator.isIpv4(value);
                case "ipv6"      -> Validator.isIpv6(value);
                case "zipcode"   -> Validator.isZipCode(value);
                case "birthday"  -> Validator.isBirthday(value);
                case "plate"     -> Validator.isPlateNumber(value);
                default          -> false; // 理论不会走到
            };
            results.add(valid);
        }
        ValidateResult resp = new ValidateResult();
        resp.setValues(values);
        resp.setType(type);
        resp.setIsValid(results);
        return ResultUtils.success(resp);
    }

    // region 不同参数类型的接口

    /**
     * 获取用户信息
     * get 基础类型参数
     *
     * @return
     */
    @GetMapping("/test/basic")
    @Parameter(name = "id", description = "用户id", example = "123")
    public BaseResponse<User> getUserInfo(@RequestParam String id) {
        User user = new User();
        user.setAccount("1234");
        user.setName("core");
        return ResultUtils.success(user);
    }

    /**
     * 获取用户信息
     * get 包装类型参数
     *
     * @return
     */
    @GetMapping("/test/wrapper")
    @Parameter(name = "id", description = "用户id", example = "123")
    public BaseResponse<User> getUserInfo(@RequestParam Long id) {
        User user = new User();
        user.setAccount("1234");
        user.setName("core");
        return ResultUtils.success(user);
    }

    /**
     * 获取用户信息
     * get 数组类型参数
     *
     * @return
     */
    @GetMapping("/test/array")
    @Parameter(name = "ids", description = "用户id数组", example = "[1,2,3]")
    public BaseResponse<User> getUserInfo(@RequestParam List<Long> ids) {
        User user = new User();
        user.setAccount("1234");
        user.setName("core");
        return ResultUtils.success(user);
    }

    /**
     * 提交用户信息
     * post 基础类型参数
     * @return
     */
    @PostMapping("/test/basic")
    public BaseResponse<Long> postUserInfo(@RequestBody long userId) {
        return ResultUtils.success(1L);
    }

    /**
     * 提交用户信息
     * post 包装类型参数
     * @return
     */
    @PostMapping("/test/wrapper")
    public BaseResponse<Long> postUserInfo(@RequestBody Long userId) {
        return ResultUtils.success(1L);
    }

    /**
     * 提交用户信息
     * post 数组类型参数
     * @return
     */
    @PostMapping("/test/array")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "用户ID数组",
            content = @Content(
                    examples = @ExampleObject("[1, 2, 3]")
            )
    )
    public BaseResponse<List<Long>> postUserInfo(@RequestBody List<Long> userId) {
        return ResultUtils.success(new ArrayList<>(List.of(1L, 2L, 3L)));
    }

    /**
     * 提交用户信息
     * post 数组对象参数
     * @return
     */
    @PostMapping("/test/array/object")
    public BaseResponse<List<User>> postUserInfoList(@RequestBody List<User> users) {
        return ResultUtils.success(List.of(new User()));
    }

    /**
     * 提交用户信息
     * post 对象类型参数
     * @return
     */
    @PostMapping("/test/object")
    public BaseResponse<List<User>> postUserInfo(@RequestBody User user) {
        return ResultUtils.success(new ArrayList<>(List.of(user, user)));
    }

    /**
     * 什么都没有的接口
     */
    @GetMapping("/test/nothing")
    public void doNothing() {}

}
