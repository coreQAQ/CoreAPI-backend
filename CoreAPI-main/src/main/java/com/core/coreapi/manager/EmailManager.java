package com.core.coreapi.manager;

import cn.hutool.extra.mail.MailUtil;
import com.core.coreapi.shared.common.ErrorCode;
import com.core.coreapi.exception.BusinessException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

/**
 * 邮件发送管理
 *
 */
@Component
@Slf4j
public class EmailManager {

    @Resource
    private Configuration freemarkerConfig;

    /**
     * 发送验证码
     * @param email
     * @param captcha
     * @param expireTime 过期时间 单位：min
     * @return
     */
    public String sendCaptcha(String email, String captcha, Integer expireTime) {
        HashMap<String, Object> dataModel = new HashMap<>();
        dataModel.put("captcha", captcha);
        dataModel.put("expireTime", expireTime);
        // 生成邮件内容
        StringWriter content = new StringWriter();
        try {
            Template template = freemarkerConfig.getTemplate("CaptchaEmailTemplate.ftl");
            template.process(dataModel, content);
        } catch (IOException | TemplateException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成邮件失败");
        }
        // 发送邮件
        try {
            String messageId = MailUtil.send(email, "CoreAPI-验证码", content.toString(), true);
            log.info("发送邮件到 {} 成功，messageId: {}", email, messageId);
            return messageId;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送邮件失败");
        }
    }

}
