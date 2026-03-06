package com.workdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "新增商单请求参数")
public class WorkOrderAddDTO {

    @NotBlank(message = "商单名称不能为空")
    @Schema(description = "商单名称/合作项目名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "商单描述/要求备注")
    private String description;

    @Schema(description = "发布平台")
    private String platform;

    @Schema(description = "商单相关截图URL集合")
    private List<String> imageUrls;

    @Schema(description = "需要垫付的金额", defaultValue = "0.00")
    private BigDecimal advanceAmount;

    @Schema(description = "商单收入/酬金", defaultValue = "0.00")
    private BigDecimal incomeAmount;
}
