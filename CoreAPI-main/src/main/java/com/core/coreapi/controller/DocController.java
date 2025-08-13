package com.core.coreapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreapi.annotation.AuthCheck;
import com.core.coreapi.common.DeleteRequest;
import com.core.coreapi.constant.UserConstant;
import com.core.coreapi.esdao.ApiDocEsDao;
import com.core.coreapi.exception.BusinessException;
import com.core.coreapi.exception.ThrowUtils;
import com.core.coreapi.model.dto.apidoc.ApiDocAddRequest;
import com.core.coreapi.model.dto.apidoc.ApiDocQueryRequest;
import com.core.coreapi.model.dto.apidoc.ApiDocUpdateRequest;
import com.core.coreapi.model.dto.apiinfo.ApiInfoQueryRequest;
import com.core.coreapi.model.entity.ApiDoc;
import com.core.coreapi.model.vo.ApiInfoVO;
import com.core.coreapi.service.ApiDocService;
import com.core.coreapi.service.UserService;
import com.core.coreapi.shared.common.BaseResponse;
import com.core.coreapi.shared.common.ErrorCode;
import com.core.coreapi.shared.common.ResultUtils;
import com.core.coreapi.shared.entity.ApiInfo;
import com.core.coreapi.shared.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 接口文档接口
 *
 */
@RestController
@RequestMapping("/apiDoc")
@Slf4j
public class DocController {

    @Resource
    private ApiDocService apiDocService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建接口文档
     *
     * @param apiDocAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addApiDoc(@RequestBody ApiDocAddRequest apiDocAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(apiDocAddRequest == null, ErrorCode.PARAMS_ERROR);
        ApiDoc apiDoc = new ApiDoc();
        BeanUtils.copyProperties(apiDocAddRequest, apiDoc);
        // 1.填充数据
        User user = userService.getLoginUser(request);
        apiDoc.setUserId(user.getId());
        // 优先级默认为 0
        if (apiDoc.getPriority() == null)
            apiDoc.setPriority(0);
        // 2.数据校验
        apiDocService.validApiDoc(apiDoc, true);
        // 3.操作ES
        return ResultUtils.success(apiDocService.add(apiDoc));
    }

    /**
     * 删除接口文档
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteApiDoc(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(apiDocService.deleteById(deleteRequest.getId()));
    }

    /**
     * 更新接口文档
     *
     * @param apiDocUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateApiDoc(@RequestBody ApiDocUpdateRequest apiDocUpdateRequest) {
        if (apiDocUpdateRequest == null || apiDocUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 复制修改的字段值
        ApiDoc apiDoc = new ApiDoc();
        BeanUtils.copyProperties(apiDocUpdateRequest, apiDoc);
        return ResultUtils.success(apiDocService.updateById(apiDoc));
    }

    /**
     * 根据 id 获取接口文档
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<ApiDoc> getApiDocById(@RequestParam long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        ApiDoc apiDoc = apiDocService.getById(id);
        ThrowUtils.throwIf(apiDoc == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(apiDoc);
    }

    /**
     * 获取接口文档概要列表
     * 去除了content字段
     *
     * @param request
     * @return
     */
    @PostMapping("/list/brief")
    public BaseResponse<List<ApiDoc>> listApiDocBrief(HttpServletRequest request) {
        return ResultUtils.success(apiDocService.listApiDocBrief());
    }

    /**
     * 分页获取列表
     *
     * @param apiDocQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<ApiDoc>> listApiDocByPage(@RequestBody ApiDocQueryRequest apiDocQueryRequest) {
        long current = apiDocQueryRequest.getCurrent();
        long size = apiDocQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(apiDocService.searchFromEs(apiDocQueryRequest));
    }

    // endregion
}
