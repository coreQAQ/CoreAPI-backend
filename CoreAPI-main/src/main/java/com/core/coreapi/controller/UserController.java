package com.core.coreapi.controller;

import java.time.Duration;
import java.util.Date;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreapi.annotation.AuthCheck;
import com.core.coreapi.shared.common.BaseResponse;
import com.core.coreapi.common.DeleteRequest;
import com.core.coreapi.shared.common.ErrorCode;
import com.core.coreapi.shared.common.ResultUtils;
import com.core.coreapi.constant.RedisKey;
import com.core.coreapi.constant.UserConstant;
import com.core.coreapi.exception.BusinessException;
import com.core.coreapi.exception.ThrowUtils;
import com.core.coreapi.manager.EmailManager;
import com.core.coreapi.model.dto.user.*;
import com.core.coreapi.shared.entity.User;
import com.core.coreapi.model.vo.LoginUserVO;
import com.core.coreapi.model.vo.UserVO;
import com.core.coreapi.service.UserService;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import static com.core.coreapi.service.impl.UserServiceImpl.SALT;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
@Slf4j
@CacheConfig(cacheNames = RedisKey.SC_USER_INFO)
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private EmailManager emailManager;

    @Resource
    private RedissonClient redissonClient;

    private final ScheduledExecutorService DELAY_EXECUTOR = Executors.newScheduledThreadPool(2);

    // region 登录相关

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    @CacheEvict(key = "#result.data")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String email = userLoginRequest.getEmail();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(email, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLogin(email, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    // endregion

    // region 增删改查

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @CacheEvict(key = "#result.data")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        String defaultPassword = "12345678";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + defaultPassword).getBytes());
        user.setUserPassword(encryptPassword);
        userService.genKeys(user);
        try {
            boolean result = userService.save(user);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            return ResultUtils.success(user.getId());
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被注册");
        }
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @DeleteMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @CacheEvict(key = "#deleteRequest.id")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @PutMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @CacheEvict(key = "#userUpdateRequest.id")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
                                            HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        User oldUser = userService.getById(user);
        ThrowUtils.throwIf(oldUser == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        if (result) {
            RBucket<Object> bucket = redissonClient.getBucket(RedisKey.SC_USER_INFO + oldUser.getAccessKey());
            bucket.delete();

            // 延迟删除
            DELAY_EXECUTOR.schedule(() -> {
                bucket.delete();
            }, 300, TimeUnit.MILLISECONDS);
        }

        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        BaseResponse<User> response = getUserById(id, request);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                   HttpServletRequest request) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * 分页获取用户封装列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                       HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return ResultUtils.success(userVOPage);
    }

    // endregion

    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest
     * @param request
     * @return
     */
    @PutMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
                                              HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        user.setId(loginUser.getId());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 发送邮箱验证码
     *
     * @param email
     * @param request
     * @return
     */
    @GetMapping("/send/captcha")
    public BaseResponse<String> sendCaptcha(@RequestParam(required = false) String email, HttpServletRequest request) {
        ThrowUtils.throwIf(!Validator.isEmail(email), ErrorCode.PARAMS_ERROR, "邮箱地址无效");
        email = email.toLowerCase();
        // 频控
        RBucket<Integer> limitBucket = redissonClient.getBucket(String.format(RedisKey.SEND_LIMIT, email));
        boolean b = limitBucket.setIfAbsent(1, Duration.ofMinutes(1));
        if (!b)
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请勿频繁发送");
        // 4位数验证码，五分钟过期
        String captcha = RandomUtil.randomNumbers(4);
        RBucket<String> captchaBucket = redissonClient.getBucket(String.format(RedisKey.CAPTCHA, email));
        captchaBucket.set(captcha, 5, TimeUnit.MINUTES);
        String messageId = emailManager.sendCaptcha(email, captcha, 5);
        return ResultUtils.success(messageId);
    }

    /**
     * 生成密钥
     *
     * @param captcha
     * @param request
     * @return
     */
    @GetMapping("/gen/keys")
    @CacheEvict(key = "#result.data")
    public BaseResponse<Long> genKeys(@RequestParam String captcha, HttpServletRequest request) {
        if (StringUtils.isBlank(captcha))
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Supplier<Boolean> action = () -> {
            User user = new User();
            user.setId(loginUser.getId());
            // 生成密钥需要
            user.setEmail(loginUser.getEmail());
            user.setUserPassword(loginUser.getUserPassword());

            userService.genKeys(user);
            boolean result = userService.updateById(user);

            if (result) {
                // 另外删除旧的 accessKey -> id 的缓存
                RBucket<Object> bucket = redissonClient.getBucket(RedisKey.AK2ID + loginUser.getAccessKey());
                bucket.delete();
            }

            return result;
        };
        userService.verifyCaptcha(loginUser.getEmail(), captcha, action);
        return ResultUtils.success(loginUser.getId());
    }

    /**
     * 绑定邮箱 一般是换绑
     *
     * @param emailBindRequest
     * @param request
     * @return
     */
    @PostMapping("/bind/email")
    public BaseResponse<Boolean> bindEmail(@RequestBody EmailBindRequest emailBindRequest, HttpServletRequest request) {
        if (emailBindRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String email = emailBindRequest.getEmail().toLowerCase();
        String captcha = emailBindRequest.getCaptcha();
        if (StringUtils.isAnyBlank(email, captcha)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少参数");
        }
        User loginUser = userService.getLoginUser(request);
        Supplier<Boolean> action = () -> {
            try {
                // 更新用户信息
                User user = new User();
                user.setId(loginUser.getId());
                user.setEmail(email);
                boolean result = userService.updateById(user);
                return result;
            } catch (DuplicateKeyException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被绑定");
            }
        };
        userService.verifyCaptcha(email, captcha, action);
        return ResultUtils.success(true);
    }

    /**
     * 修改新密码
     *
     * @param changePassRequest
     * @param request
     * @return
     */
    @PostMapping("/change/pass")
    public BaseResponse<Boolean> changePass(@RequestBody ChangePassRequest changePassRequest, HttpServletRequest request) {
        String captcha = changePassRequest.getCaptcha();
        String email = changePassRequest.getEmail();
        String newPassword = changePassRequest.getNewPassword();
        ThrowUtils.throwIf(StringUtils.isAnyBlank(captcha, email, newPassword), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(newPassword.length() < 4, ErrorCode.PARAMS_ERROR, "密码不小于4位");

        Supplier<Boolean> action = () -> {
            User user = userService.getOne(new QueryWrapper<User>()
                    .eq("email", email));
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "该邮箱未注册");
            user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes()));
            user.setUpdateTime(new Date());
            return userService.updateById(user);
        };
        userService.verifyCaptcha(email, captcha, action);
        return ResultUtils.success(true);
    }


}
