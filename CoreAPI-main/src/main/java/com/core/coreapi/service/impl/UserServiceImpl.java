package com.core.coreapi.service.impl;

import static com.core.coreapi.constant.UserConstant.USER_LOGIN_STATE;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.core.coreapi.constant.RedisKey;
import com.core.coreapi.model.dto.user.UserRegisterRequest;
import com.core.coreapi.shared.common.ErrorCode;
import com.core.coreapi.constant.CommonConstant;
import com.core.coreapi.exception.BusinessException;
import com.core.coreapi.mapper.UserMapper;
import com.core.coreapi.model.dto.user.UserQueryRequest;
import com.core.coreapi.shared.entity.User;
import com.core.coreapi.model.enums.UserRoleEnum;
import com.core.coreapi.model.vo.LoginUserVO;
import com.core.coreapi.model.vo.UserVO;
import com.core.coreapi.service.UserService;
import com.core.coreapi.utils.SqlUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * 用户服务实现
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "core";

    @Resource
    private RedissonClient redissonClient;

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String email = userRegisterRequest.getEmail().toLowerCase();
        String captcha = userRegisterRequest.getCaptcha();
        // 1. 校验
        if (StringUtils.isAnyBlank(userPassword, checkPassword, email, captcha)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (!Validator.isEmail(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        if (userPassword.length() < 4 || checkPassword.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        User user = new User();
        Supplier<Boolean> action = () -> {
            try {
                // 1. 加密
                String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
                // 2. 插入数据
                user.setUserPassword(encryptPassword);
                user.setEmail(email);
                // 注册时分配密钥
                genKeys(user);
                return save(user);
            } catch (DuplicateKeyException e) {
                e.printStackTrace();
                Throwable rootCause = e.getCause();
                if (rootCause != null) {
                    String message = rootCause.getMessage();
                    Pattern p = Pattern.compile("for key '([^']+)'");
                    Matcher m = p.matcher(message);
                    if (m.find()) {
                        String uniqueKey = m.group(1); // 唯一索引名字
                        // 你可以根据索引名自己做字段映射
                        if (uniqueKey.contains("userAccount")) {
                            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
                        } else if (uniqueKey.contains("email")) {
                            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱重复");
                        }
                    }
                }
                // 默认异常处理
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        };
        verifyCaptcha(email, captcha, action);
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String email, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(email, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (!Validator.isEmail(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        if (userPassword.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email.toLowerCase());
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String email = userQueryRequest.getEmail();
        String userName = userQueryRequest.getUserName();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StringUtils.isNotBlank(email), "email", email);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public void genKeys(User user) {
        String email = user.getEmail();
        String userPassword = user.getUserPassword();
        String accessKey = DigestUtils.md5DigestAsHex((SALT + email + RandomUtil.randomNumbers(5)).getBytes());
        String secretKey = DigestUtils.md5DigestAsHex((SALT + userPassword + RandomUtil.randomNumbers(5)).getBytes());
        // 更新用户信息
        user.setAccessKey(accessKey);
        user.setSecretKey(secretKey);
    }

    @Override
    public void verifyCaptcha(String email, String captcha, Supplier<Boolean> action) {
        if (StringUtils.isAnyBlank(email, captcha)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少参数");
        }
        email = email.toLowerCase();
        RLock lock = redissonClient.getLock(String.format(RedisKey.CAPTCHA_LOCK, email));
        try {
            if (lock.tryLock(2, 8, TimeUnit.SECONDS)) {
                RBucket<String> bucket = redissonClient.getBucket(String.format(RedisKey.CAPTCHA, email));
                String realCaptcha = bucket.get();
                if (!captcha.equals(realCaptcha)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
                }

                Boolean result = action.get();

                if (result) {
                    bucket.delete();
                } else {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据库异常");
                }
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
            }
        } catch (InterruptedException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


}
