package com.workdiary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "商单侧分页查询参数")
public class WorkOrderQueryDTO {

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;

    @Schema(description = "每页大小", defaultValue = "10")
    private Integer size = 10;

    @Schema(description = "商单名称(模糊查询)")
    private String title;

    @Schema(description = "单一商单状态")
    private Integer status;

    @Schema(description = "多种商单状态列表(用于'制作中'Tab 匹配 10/20)")
    private List<Integer> statuses;

    @Schema(description = "垫付是否已收回(0:未收回, 1:已收回)")
    private Integer isAdvanceRecovered;

    @Schema(description = "收入是否已到账(0:未到账, 1:已到账)")
    private Integer isIncomeReceived;
}
