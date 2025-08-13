package com.core.coreapi.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.URLUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.core.coreapi.constant.CommonConstant;
import com.core.coreapi.exception.BusinessException;
import com.core.coreapi.exception.ThrowUtils;
import com.core.coreapi.mapper.ApiInfoMapper;
import com.core.coreapi.model.dto.apiinfo.ApiInfoQueryRequest;
import com.core.coreapi.model.enums.HttpMethodEnum;
import com.core.coreapi.model.vo.ApiInfoVO;
import com.core.coreapi.service.ApiInfoService;
import com.core.coreapi.service.UserService;
import com.core.coreapi.shared.common.ErrorCode;
import com.core.coreapi.shared.entity.ApiInfo;
import com.core.coreapi.shared.entity.User;
import com.core.coreapi.shared.enums.ApiStatusEnum;
import com.core.coreapi.shared.utils.SignUtil;
import com.core.coreapi.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 接口信息服务实现
 */
@Service
@Slf4j
public class ApiInfoServiceImpl extends ServiceImpl<ApiInfoMapper, ApiInfo>
        implements ApiInfoService {

    @Resource
    private OkHttpClient httpClient;

    @Resource
    private UserService userService;

    @Override
    public void validApiInfo(ApiInfo apiInfo, boolean add) {
        if (apiInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = apiInfo.getUserId();
        String name = apiInfo.getName();
        String description = apiInfo.getDescription();
        String host = apiInfo.getHost();
        String port = apiInfo.getPort();
        String path = apiInfo.getPath();
        String method = apiInfo.getMethod();
        Integer status = apiInfo.getStatus();
        Integer cost = apiInfo.getCost();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(
                    ObjectUtils.anyNull(userId, status, cost) ||
                            StringUtils.isAnyBlank(name, host, port, path, method),
                    ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (userId != null && userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id错误");
        }
        if (StringUtils.isNotBlank(name) && name.length() > 128) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口名过长");
        }
        if (StringUtils.isNotBlank(description) && description.length() > 2048) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "描述过长");
        }
        if (StringUtils.isNotBlank(host) && host.length() > 128) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "域名过长");
        }
        if (StringUtils.isNotBlank(port) && port.length() > 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "端口过大");
        }
        if (StringUtils.isNotBlank(path) && path.length() > 128) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "路径过长");
        }
        if (StringUtils.isNotBlank(method) && !HttpMethodEnum.getValues().contains(method)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不存在此方法");
        }
        if (status != null && !ApiStatusEnum.getValues().contains(status)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不存在此状态");
        }
        if (cost != null && cost < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "价格不小于0");
        }
    }

    @Override
    public QueryWrapper<ApiInfo> getQueryWrapper(ApiInfoQueryRequest apiInfoQueryRequest) {
        QueryWrapper<ApiInfo> queryWrapper = new QueryWrapper<>();
        if (apiInfoQueryRequest == null) {
            return queryWrapper;
        }
        Long id = apiInfoQueryRequest.getId();
        Long userId = apiInfoQueryRequest.getUserId();
        Long docId = apiInfoQueryRequest.getDocId();
        String name = apiInfoQueryRequest.getName();
        String description = apiInfoQueryRequest.getDescription();
        String host = apiInfoQueryRequest.getHost();
        String path = apiInfoQueryRequest.getPath();
        String method = apiInfoQueryRequest.getMethod();
        Integer status = apiInfoQueryRequest.getStatus();
        String sortField = apiInfoQueryRequest.getSortField();
        String sortOrder = apiInfoQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(docId != null, "docId", docId);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.like(StringUtils.isNotBlank(host), "host", host);
        queryWrapper.like(StringUtils.isNotBlank(path), "path", path);
        queryWrapper.eq(StringUtils.isNotBlank(method), "method", method);
        queryWrapper.eq(ObjectUtils.isNotEmpty(status), "status", status);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public ApiInfoVO getApiInfoVO(ApiInfo apiInfo) {
        if (apiInfo == null) {
            return null;
        }
        ApiInfoVO apiInfoVO = new ApiInfoVO();
        BeanUtils.copyProperties(apiInfo, apiInfoVO);
        return apiInfoVO;
    }

    @Override
    public List<ApiInfoVO> getApiInfoVOList(List<ApiInfo> apiInfoList) {
        if (apiInfoList == null) {
            return Collections.emptyList();
        }
        return apiInfoList.stream()
                .map(this::getApiInfoVO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ApiInfoVO> getApiInfoVOPage(Page<ApiInfo> apiInfoPage) {
        List<ApiInfo> apiInfoList = apiInfoPage.getRecords();
        Page<ApiInfoVO> apiInfoVOPage = new Page<>(apiInfoPage.getCurrent(), apiInfoPage.getSize(), apiInfoPage.getTotal());
        if (CollUtil.isEmpty(apiInfoList)) {
            return apiInfoVOPage;
        }
        List<ApiInfoVO> apiInfoVOList = getApiInfoVOList(apiInfoList);
        apiInfoVOPage.setRecords(apiInfoVOList);
        return apiInfoVOPage;

    }

    @Override
    public String invokeApi(String url, String method, String query, String body, HttpServletRequest request) {
        // 1.校验
        ThrowUtils.throwIf(StringUtils.isBlank(url), ErrorCode.PARAMS_ERROR, "缺少url");
        if (query == null) query = "";
        if (body == null) body = "";
        // 2.根据当前用户信息构建请求头
        User loginUser = userService.getLoginUser(request);
        String timestamp = String.valueOf(System.currentTimeMillis());
        Request.Builder requestBuilder = new Request.Builder()
                .header("accessKey", loginUser.getAccessKey())
                .header("nonce", UUID.randomUUID().toString())
                .header("timestamp", timestamp)
                .header("sign", SignUtil.genSign(timestamp, loginUser.getSecretKey()));
        // 3.构建请求
        if (StringUtils.isNotBlank(query)) {
            url = String.format("%s?%s", url, URLUtil.encodeQuery(query));
        }
        if (HttpMethodEnum.GET.getValue().equals(method)) {
            requestBuilder
                    .url(url)
                    .get();
        } else if (HttpMethodEnum.POST.getValue().equals(method)) {
            requestBuilder
                    .url(url)
                    .post(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), body));
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不存在此方法");
        }
        // 4.发起请求
        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (response.body() != null) {
                return response.body().string();
            } else return null;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
        }
    }

}