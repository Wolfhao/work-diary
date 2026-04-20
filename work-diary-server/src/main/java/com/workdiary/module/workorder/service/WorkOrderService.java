package com.workdiary.module.workorder.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.workdiary.module.workorder.dto.WorkOrderAddDTO;
import com.workdiary.module.workorder.dto.WorkOrderQueryDTO;
import com.workdiary.module.workorder.dto.WorkOrderUpdateDTO;
import com.workdiary.module.workorder.entity.WorkOrder;
import com.workdiary.module.workorder.vo.WorkOrderVO;

/**
 * 商单服务接口
 */
public interface WorkOrderService extends IService<WorkOrder> {

    boolean addWorkOrder(WorkOrderAddDTO dto);

    boolean updateWorkOrder(WorkOrderUpdateDTO dto);

    boolean deleteWorkOrder(Long id);

    WorkOrderVO getWorkOrderDetail(Long id);

    Page<WorkOrderVO> pageWorkOrders(WorkOrderQueryDTO queryDTO);
}
