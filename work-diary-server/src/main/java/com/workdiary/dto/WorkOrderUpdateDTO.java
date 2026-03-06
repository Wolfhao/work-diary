package com.workdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "修改商单请求参数")
public class WorkOrderUpdateDTO extends WorkOrderAddDTO {

    @NotNull(message = "商单ID不能为空")
    @Schema(description = "商单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "垫付是否已收回(0:未收回, 1:已收回)")
    private Integer isAdvanceRecovered;

    @Schema(description = "收入是否已到账(0:未到账, 1:已到账)")
    private Integer isIncomeReceived;

    @Schema(description = "商单状态(10:待开工, 20:制作中, 30:待结款, 40:已完成, 90:已取消)")
    private Integer status;
}
