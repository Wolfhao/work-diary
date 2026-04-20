package com.workdiary.module.workorder.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workdiary.common.exception.ApiException;
import com.workdiary.module.workorder.dto.WorkOrderAddDTO;
import com.workdiary.module.workorder.dto.WorkOrderQueryDTO;
import com.workdiary.module.workorder.dto.WorkOrderUpdateDTO;
import com.workdiary.module.workorder.entity.WorkOrder;
import com.workdiary.module.workorder.mapper.WorkOrderMapper;
import com.workdiary.module.workorder.service.WorkOrderService;
import com.workdiary.module.workorder.vo.WorkOrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商单服务实现类
 */
@Service
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder> implements WorkOrderService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addWorkOrder(WorkOrderAddDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        WorkOrder workOrder = new WorkOrder();
        BeanUtil.copyProperties(dto, workOrder);
        workOrder.setUserId(userId);
        workOrder.setStatus(10);
        workOrder.setIsAdvanceRecovered(0);
        workOrder.setIsIncomeReceived(0);

        return this.save(workOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWorkOrder(WorkOrderUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        WorkOrder existingOrder = this.getById(dto.getId());

        if (existingOrder == null || !existingOrder.getUserId().equals(userId)) {
            throw new ApiException("商单不存在或无权操作");
        }

        WorkOrder updateEntity = new WorkOrder();
        BeanUtil.copyProperties(dto, updateEntity);

        if (dto.getIsAdvanceRecovered() != null && dto.getIsAdvanceRecovered() == 1
                && existingOrder.getIsAdvanceRecovered() == 0) {
            updateEntity.setAdvanceRecoverTime(LocalDateTime.now());
        }

        if (dto.getIsIncomeReceived() != null && dto.getIsIncomeReceived() == 1
                && existingOrder.getIsIncomeReceived() == 0) {
            updateEntity.setIncomeReceiveTime(LocalDateTime.now());
        }

        Integer targetStatus = dto.getStatus() != null ? dto.getStatus() : existingOrder.getStatus();
        if (targetStatus != null && targetStatus == 30) {
            BigDecimal currentAdvance = existingOrder.getAdvanceAmount();
            boolean isAdvanceOk = (currentAdvance == null || currentAdvance.compareTo(BigDecimal.ZERO) <= 0)
                    || ((dto.getIsAdvanceRecovered() != null ? dto.getIsAdvanceRecovered()
                            : existingOrder.getIsAdvanceRecovered()) == 1);

            BigDecimal currentIncome = existingOrder.getIncomeAmount();
            boolean isIncomeOk = (currentIncome == null || currentIncome.compareTo(BigDecimal.ZERO) <= 0)
                    || ((dto.getIsIncomeReceived() != null ? dto.getIsIncomeReceived()
                            : existingOrder.getIsIncomeReceived()) == 1);

            if (isAdvanceOk && isIncomeOk) {
                updateEntity.setStatus(40);
            }
        }

        return this.updateById(updateEntity);
    }

    @Override
    public boolean deleteWorkOrder(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        WorkOrder existingOrder = this.getById(id);

        if (existingOrder == null || !existingOrder.getUserId().equals(userId)) {
            throw new ApiException("商单不存在或无权操作");
        }

        return this.removeById(id);
    }

    @Override
    public WorkOrderVO getWorkOrderDetail(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        WorkOrder existingOrder = this.getById(id);

        if (existingOrder == null || !existingOrder.getUserId().equals(userId)) {
            throw new ApiException("商单不存在或无权操作");
        }

        WorkOrderVO vo = new WorkOrderVO();
        BeanUtil.copyProperties(existingOrder, vo);
        return vo;
    }

    @Override
    public Page<WorkOrderVO> pageWorkOrders(WorkOrderQueryDTO queryDTO) {
        Long userId = StpUtil.getLoginIdAsLong();

        Page<WorkOrder> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<WorkOrder> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(WorkOrder::getUserId, userId)
                .like(StrUtil.isNotBlank(queryDTO.getTitle()), WorkOrder::getTitle, queryDTO.getTitle())
                .eq(queryDTO.getStatus() != null, WorkOrder::getStatus, queryDTO.getStatus())
                .in(queryDTO.getStatuses() != null && !queryDTO.getStatuses().isEmpty(),
                        WorkOrder::getStatus, queryDTO.getStatuses())
                .eq(queryDTO.getIsAdvanceRecovered() != null, WorkOrder::getIsAdvanceRecovered,
                        queryDTO.getIsAdvanceRecovered())
                .eq(queryDTO.getIsIncomeReceived() != null, WorkOrder::getIsIncomeReceived,
                        queryDTO.getIsIncomeReceived())
                .orderByDesc(WorkOrder::getCreateTime);

        Page<WorkOrder> resultPage = this.page(page, queryWrapper);

        Page<WorkOrderVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<WorkOrderVO> voList = resultPage.getRecords().stream().map(order -> {
            WorkOrderVO vo = new WorkOrderVO();
            BeanUtil.copyProperties(order, vo);
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }
}
