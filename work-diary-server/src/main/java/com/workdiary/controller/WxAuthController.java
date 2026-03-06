package com.workdiary.controller;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workdiary.common.api.Result;
import com.workdiary.common.exception.ApiException;
import com.workdiary.dto.WxLoginDTO;
import com.workdiary.entity.User;
import com.workdiary.service.UserService;
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
            // 1. 调用微信接口，用 code 换取 openId 和 session_key
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(loginDTO.getCode());
            String openId = sessionInfo.getOpenid();

            if (StrUtil.isBlank(openId)) {
                return Result.failed("获取微信OpenId失败");
            }

            // 2. 根据 openId 查询我方数据库是否已有该用户
            User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, openId));

            // 3. 如果没注册过，自动执行注册逻辑，下发一个新 ID
            if (user == null) {
                user = new User();
                user.setOpenId(openId);
                user.setNickname("微信用户"); // TODO 后续可接入获取头像昵称的 API
                user.setStatus(1);
                userService.save(user);
            }

            // 4. 调用 Sa-Token，以我方系统生成的 user.getId() 作为登录标识号
            StpUtil.login(user.getId());

            // 5. 获取并返回 Token 信息对象给前端
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
