package com.workdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "微信小程序登录参数对象")
public class WxLoginDTO {

    @NotBlank(message = "code不能为空")
    @Schema(description = "通过 wx.login 获取到的临时登录凭证code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
