package com.core.coreapi.controller;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreapi.common.DeleteRequest;
import com.core.coreapi.common.IdRequest;
import com.core.coreapi.constant.RedisKey;
import com.core.coreapi.exception.BusinessException;
import com.core.coreapi.exception.ThrowUtils;
import com.core.coreapi.model.dto.apiinfo.ApiInfoAddRequest;
import com.core.coreapi.model.dto.apiinfo.ApiInfoQueryRequest;
import com.core.coreapi.model.dto.apiinfo.ApiInfoUpdateRequest;
import com.core.coreapi.model.dto.apiinfo.InvokeRequest;
import com.core.coreapi.model.dto.doc.Document;
import com.core.coreapi.model.entity.ApiDoc;
import com.core.coreapi.model.vo.ApiInfoVO;
import com.core.coreapi.service.ApiDocService;
import com.core.coreapi.service.ApiInfoService;
import com.core.coreapi.service.UserService;
import com.core.coreapi.shared.common.BaseResponse;
import com.core.coreapi.shared.common.ErrorCode;
import com.core.coreapi.shared.common.ResultUtils;
import com.core.coreapi.shared.entity.ApiInfo;
import com.core.coreapi.shared.entity.User;
import com.core.coreapi.shared.enums.ApiStatusEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * api接口
 */
@RestController
@RequestMapping("/api")
@Slf4j
@CacheConfig(cacheNames = RedisKey.SC_API_INFO)
public class ApiController {

    @Resource
    private ApiInfoService apiInfoService;

    @Resource
    private ApiDocService apiDocService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    private final ScheduledExecutorService DELAY_EXECUTOR = Executors.newScheduledThreadPool(2);

    // region 增删改查

    /**
     * 创建
     *
     * @param apiInfoAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @CacheEvict(key = "#result.data")
    public BaseResponse<Long> addApiInfo(@RequestBody ApiInfoAddRequest apiInfoAddRequest, HttpServletRequest request) {
        // 1.校验
        if (apiInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.复制数据到entity
        ApiInfo apiInfo = new ApiInfo();
        BeanUtils.copyProperties(apiInfoAddRequest, apiInfo);
        // 3.填充其他数据
        User loginUser = userService.getLoginUser(request);
        apiInfo.setUserId(loginUser.getId());
        // 默认下线
        apiInfo.setStatus(ApiStatusEnum.OFFLINE.getValue());
        if (apiInfo.getDocId() == null)
            apiInfo.setDocId(-1L);
        // 4.参数校验
        apiInfoService.validApiInfo(apiInfo, true);
        // 5.写入数据库
        boolean result = apiInfoService.save(apiInfo);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 6.返回id
        long newApiInfoId = apiInfo.getId();
        return ResultUtils.success(newApiInfoId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @return
     */
    @DeleteMapping("/delete")
    @CacheEvict(key = "#deleteRequest.id")
    public BaseResponse<Boolean> deleteApiInfo(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        ApiInfo oldApiInfo = apiInfoService.getById(id);
        ThrowUtils.throwIf(oldApiInfo == null, ErrorCode.NOT_FOUND_ERROR);
        boolean b = apiInfoService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param apiInfoUpdateRequest
     * @return
     */
    @PutMapping("/update")
    @CacheEvict(key = "#apiInfoUpdateRequest.id")
    public BaseResponse<Boolean> updateApiInfo(@RequestBody ApiInfoUpdateRequest apiInfoUpdateRequest) {
        // 1.校验
        if (apiInfoUpdateRequest == null || apiInfoUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.复制数据到entity
        ApiInfo apiInfo = new ApiInfo();
        BeanUtils.copyProperties(apiInfoUpdateRequest, apiInfo);
        if (apiInfo.getDocId() == null)
            apiInfo.setDocId(-1L); // 表示未绑定
        // 3.参数校验
        apiInfoService.validApiInfo(apiInfo, false);
        // 4.判断是否存在
        long id = apiInfoUpdateRequest.getId();
        ApiInfo oldApiInfo = apiInfoService.getById(id);
        ThrowUtils.throwIf(oldApiInfo == null, ErrorCode.NOT_FOUND_ERROR);
        // 5.更新数据
        boolean result = apiInfoService.updateById(apiInfo);

        if (result) {
            RBucket<Object> bucket = redissonClient.getBucket(RedisKey.SC_API_INFO + oldApiInfo.getPath() + oldApiInfo.getMethod());
            bucket.delete();

            // 延迟删除
            DELAY_EXECUTOR.schedule(() -> {
                bucket.delete();
            }, 300, TimeUnit.MILLISECONDS);
        }

        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<ApiInfoVO> getApiInfoVOById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ApiInfo apiInfo = apiInfoService.getById(id);
        ThrowUtils.throwIf(apiInfo == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(apiInfoService.getApiInfoVO(apiInfo));
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param apiInfoQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<ApiInfoVO>> listApiInfoVOByPage(@RequestBody ApiInfoQueryRequest apiInfoQueryRequest) {
        long current = apiInfoQueryRequest.getCurrent();
        long size = apiInfoQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<ApiInfo> apiInfoPage = apiInfoService.page(new Page<>(current, size),
                apiInfoService.getQueryWrapper(apiInfoQueryRequest));
        return ResultUtils.success(apiInfoService.getApiInfoVOPage(apiInfoPage));
    }

    // endregion

    /**
     * 强制上线接口
     * 用于请求示例不完整或错误的情况
     *
     * @param idRequest
     * @param request
     * @return
     */
    @PutMapping("/online/force")
    @CacheEvict(key = "#idRequest.id")
    public BaseResponse<Boolean> apiOnlineForce(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        // 1.校验
        if (idRequest == null || idRequest.getId() == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.设置参数
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setId(idRequest.getId());
        apiInfo.setStatus(ApiStatusEnum.ONLINE.getValue());
        // 3.判断是否存在
        ApiInfo oldApiInfo = apiInfoService.getById(idRequest.getId());
        ThrowUtils.throwIf(oldApiInfo == null, ErrorCode.NOT_FOUND_ERROR);
        // 4.写入
        boolean b = apiInfoService.updateById(apiInfo);
        return ResultUtils.success(b);
    }


    /**
     * 上线接口
     * 根据queryExample和reqBodyExample调用一次接口保证可用
     *
     * @param idRequest
     * @return
     */
    @PutMapping("/online")
    @CacheEvict(key = "#idRequest.id")
    public BaseResponse<Boolean> apiOnline(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        // 1.校验
        if (idRequest == null || idRequest.getId() == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.设置参数
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setId(idRequest.getId());
        apiInfo.setStatus(ApiStatusEnum.ONLINE.getValue());
        // 3.判断是否存在
        ApiInfo oldApiInfo = apiInfoService.getById(idRequest.getId());
        ThrowUtils.throwIf(oldApiInfo == null, ErrorCode.NOT_FOUND_ERROR);
        // 4.上线前调用一次接口保证可用
        String url = String.format("http://%s:%s%s", oldApiInfo.getHost(), oldApiInfo.getPort(), oldApiInfo.getPath());
        String respBody = apiInfoService.invokeApi(url, oldApiInfo.getMethod(),
                oldApiInfo.getQueryExample(), oldApiInfo.getReqBodyExample(), request);
        if (StringUtils.isBlank(respBody)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口未响应");
        }
        try {
            JsonNode root = new ObjectMapper().readTree(respBody);
            // 确认code为0
            if (root.path("code").isMissingNode()) {
                String message = "HTTP 状态码：" + root.path("status").asInt() + root.path("error").asText();
                throw new BusinessException(ErrorCode.OPERATION_ERROR, message);
            } else if (ErrorCode.SUCCESS.getCode() != root.path("code").asInt()) {
                String message = "业务码：" + root.path("code").asInt() + root.path("message").asText();
                throw new BusinessException(ErrorCode.OPERATION_ERROR, message);
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口响应体解析失败");
        }
        // 5.写入
        boolean b = apiInfoService.updateById(apiInfo);
        return ResultUtils.success(b);
    }

    /**
     * 下线接口
     *
     * @param idRequest
     * @return
     */
    @PutMapping("/offline")
    @CacheEvict(key = "#idRequest.id")
    public BaseResponse<Boolean> apiOffline(@RequestBody IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 参数校验
        ApiInfo apiInfo = new ApiInfo();
        apiInfo.setId(idRequest.getId());
        apiInfo.setStatus(ApiStatusEnum.OFFLINE.getValue());
        long id = idRequest.getId();
        // 判断是否存在
        ApiInfo oldApiInfo = apiInfoService.getById(id);
        ThrowUtils.throwIf(oldApiInfo == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = apiInfoService.updateById(apiInfo);
        return ResultUtils.success(result);
    }

    /**
     * 在线调用接口
     *
     * @param invokeRequest
     * @param request
     * @return
     */
    @PostMapping("/invoke")
    public BaseResponse<String> invokeApi(@RequestBody InvokeRequest invokeRequest, HttpServletRequest request) {
        if (invokeRequest == null || invokeRequest.getId() == null || invokeRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = invokeRequest.getId();
        // 判断是否存在
        ApiInfo apiInfo = apiInfoService.getById(id);
        ThrowUtils.throwIf(apiInfo == null, ErrorCode.NOT_FOUND_ERROR);
        // 调用接口
        String url = String.format("http://%s:%s%s", apiInfo.getHost(), apiInfo.getPort(), apiInfo.getPath());
        String result = apiInfoService.invokeApi(url, apiInfo.getMethod(),
                invokeRequest.getQuery(), invokeRequest.getBody(), request);
        if (StringUtils.isBlank(result))
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口未响应");
        // pretty
        return ResultUtils.success(JSONUtil.toJsonPrettyStr(result));
    }

    /**
     * 生成文档
     * 会生成一个新的文档与指定的接口绑定，并且覆盖接口的请求
     *
     * @param idRequest
     * @return
     */
    @PostMapping("/genDoc")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> genDoc(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        if (idRequest == null || idRequest.getId() == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少id");
        }
        Long id = idRequest.getId();
        // 1.判断是否存在
        ApiInfo apiInfo = apiInfoService.getById(id);
        ThrowUtils.throwIf(apiInfo == null, ErrorCode.NOT_FOUND_ERROR, "接口不存在");
        ThrowUtils.throwIf(StringUtils.isBlank(apiInfo.getOpenapiUrl()), ErrorCode.OPERATION_ERROR, "缺少openapi文档");
        // 2.生成文档和请求示例
        Document doc = apiDocService.genDoc(apiInfo.getOpenapiUrl(), apiInfo.getName(), apiInfo.getPath(), apiInfo.getMethod());
        if (doc == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文档生成失败");
        }
        long apiDocId = IdUtil.getSnowflakeNextId();
        apiInfo.setDocId(apiDocId);
        apiInfo.setQueryExample(doc.getQueryExample());
        apiInfo.setReqBodyExample(doc.getReqBodyExample());
        // 3.更新接口信息
        boolean b = apiInfoService.updateById(apiInfo);
        // 4.新文档写入es
        ApiDoc apiDoc = new ApiDoc();
        apiDoc.setId(apiDocId);
        apiDoc.setUserId(userService.getLoginUser(request).getId());
        apiDoc.setTitle(apiInfo.getName());
        apiDoc.setDescription(apiInfo.getDescription());
        apiDoc.setContent(doc.getContent());
        apiDoc.setPriority(0);
        apiDocService.add(apiDoc);

        return ResultUtils.success(true);
    }

}