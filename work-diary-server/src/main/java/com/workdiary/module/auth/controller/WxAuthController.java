package com.workdiary.module.auth.controller;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workdiary.common.api.Result;
import com.workdiary.common.exception.ApiException;
import com.workdiary.module.auth.dto.WxLoginDTO;
import com.workdiary.module.auth.service.UserService;
import com.workdiary.shared.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/wx")
@RequiredArgsConstructor
@Tag(name = "Wechat Auth", description = "微信小程序授权登录接口")
public class WxAuthController {

    private final WxMaService wxMaService;
    private final UserService userService;

    @Operation(summary = "微信快捷登录", description = "传入 wx.login 的 code，换取本系统的 Sa-Token")
    @PostMapping("/login")
    public Result<SaTokenInfo> login(@Validated @RequestBody WxLoginDTO loginDTO) {
        try {
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(loginDTO.getCode());
            String openId = sessionInfo.getOpenid();

            if (StrUtil.isBlank(openId)) {
                return Result.failed("获取微信OpenId失败");
            }

            User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, openId));

            if (user == null) {
                user = new User();
                user.setOpenId(openId);
                user.setNickname("微信用户");
                user.setStatus(1);
                userService.save(user);
            }

            StpUtil.login(user.getId());
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            return Result.success(tokenInfo);

        } catch (Exception e) {
            log.error("微信登录授权异常: ", e);
            throw new ApiException("微信登录失败，请稍后重试: " + e.getMessage());
        }
    }

    @Operation(summary = "退出登录", description = "注销当前登录状态")
    @PostMapping("/logout")
    public Result<Boolean> logout() {
        StpUtil.logout();
        return Result.success(true);
    }
}
