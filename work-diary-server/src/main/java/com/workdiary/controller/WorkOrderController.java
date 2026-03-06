package com.workdiary.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.workdiary.common.api.Result;
import com.workdiary.dto.WorkOrderAddDTO;
import com.workdiary.dto.WorkOrderQueryDTO;
import com.workdiary.dto.WorkOrderUpdateDTO;
import com.workdiary.service.WorkOrderService;
import com.workdiary.vo.WorkOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/work-order")
@RequiredArgsConstructor
@Tag(name = "Work Order", description = "商单管理接口")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @Operation(summary = "新增商单", description = "录入一条新的商务合作商单")
    @PostMapping
    public Result<Boolean> addWorkOrder(@Validated @RequestBody WorkOrderAddDTO dto) {
        boolean success = workOrderService.addWorkOrder(dto);
        return success ? Result.success(true) : Result.failed("新增失败");
    }

    @Operation(summary = "修改商单", description = "更新商单核心状态、资金回款状态等")
    @PutMapping
    public Result<Boolean> updateWorkOrder(@Validated @RequestBody WorkOrderUpdateDTO dto) {
        boolean success = workOrderService.updateWorkOrder(dto);
        return success ? Result.success(true) : Result.failed("修改失败");
    }

    @Operation(summary = "删除商单", description = "逻辑删除指定商单")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteWorkOrder(@Parameter(description = "商单ID") @PathVariable Long id) {
        boolean success = workOrderService.deleteWorkOrder(id);
        return success ? Result.success(true) : Result.failed("删除失败");
    }

    @Operation(summary = "查询商单详情", description = "获取单个商单详细信息")
    @GetMapping("/{id}")
    public Result<WorkOrderVO> getWorkOrderDetail(@Parameter(description = "商单ID") @PathVariable Long id) {
        WorkOrderVO vo = workOrderService.getWorkOrderDetail(id);
        return Result.success(vo);
    }

    @Operation(summary = "分页查询商单列表", description = "按条件分页搜索当前用户的商单列表")
    @PostMapping("/page")
    public Result<Page<WorkOrderVO>> pageWorkOrders(@RequestBody WorkOrderQueryDTO queryDTO) {
        Page<WorkOrderVO> page = workOrderService.pageWorkOrders(queryDTO);
        return Result.success(page);
    }
}
