package com.smart.university.controller.admin;

import com.smart.university.common.base.PageResult;
import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.BatchTeachingClassSaveReqDTO;
import com.smart.university.domain.dto.req.SelectionBatchPageQueryReqDTO;
import com.smart.university.domain.dto.req.SelectionBatchSaveReqDTO;
import com.smart.university.domain.dto.req.SelectionBatchStatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.SelectionBatchRespDTO;
import com.smart.university.domain.dto.resp.TeachingClassRespDTO;
import com.smart.university.service.SelectionBatchService;
import com.smart.university.web.annotation.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教务端选课批次管理接口
 */
@RestController
@RequestMapping("/api/v1/admin/selection-batches")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class SelectionBatchController {

    private final SelectionBatchService selectionBatchService;

    /**
     * 分页查询选课批次
     *
     * @param requestParam 查询条件
     * @return 批次分页结果
     */
    @GetMapping
    public Result<PageResult<SelectionBatchRespDTO>> pageSelectionBatch(SelectionBatchPageQueryReqDTO requestParam) {
        return Result.success(selectionBatchService.pageSelectionBatch(requestParam));
    }

    /**
     * 查询批次详情
     *
     * @param batchId 批次 ID
     * @return 批次信息
     */
    @GetMapping("/{batchId}")
    public Result<SelectionBatchRespDTO> getSelectionBatchDetail(@PathVariable Long batchId) {
        return Result.success(selectionBatchService.getSelectionBatchDetail(batchId));
    }

    /**
     * 创建选课批次
     *
     * @param requestParam 批次入参
     * @return 批次 ID
     */
    @PostMapping
    public Result<Long> saveSelectionBatch(@Valid @RequestBody SelectionBatchSaveReqDTO requestParam) {
        return Result.success("保存成功", selectionBatchService.saveSelectionBatch(requestParam));
    }

    /**
     * 修改选课批次
     *
     * @param batchId      批次 ID
     * @param requestParam 批次入参
     * @return 批次 ID
     */
    @PutMapping("/{batchId}")
    public Result<Long> updateSelectionBatch(@PathVariable Long batchId,
                                             @Valid @RequestBody SelectionBatchSaveReqDTO requestParam) {
        requestParam.setId(batchId);
        return Result.success("保存成功", selectionBatchService.saveSelectionBatch(requestParam));
    }

    /**
     * 修改批次状态
     *
     * @param batchId      批次 ID
     * @param requestParam 状态入参
     * @return 空响应
     */
    @PatchMapping("/{batchId}/status")
    public Result<Void> updateSelectionBatchStatus(@PathVariable Long batchId,
                                                   @Valid @RequestBody SelectionBatchStatusUpdateReqDTO requestParam) {
        selectionBatchService.updateSelectionBatchStatus(batchId, requestParam);
        return Result.success("状态修改成功", null);
    }

    /**
     * 添加教学班到批次
     *
     * @param batchId      批次 ID
     * @param requestParam 教学班 ID 集合入参
     * @return 空响应
     */
    @PostMapping("/{batchId}/teaching-classes")
    public Result<Void> saveBatchTeachingClass(@PathVariable Long batchId,
                                               @Valid @RequestBody BatchTeachingClassSaveReqDTO requestParam) {
        selectionBatchService.saveBatchTeachingClass(batchId, requestParam);
        return Result.success("添加成功", null);
    }

    /**
     * 查询批次已开放的教学班
     *
     * @param batchId 批次 ID
     * @return 教学班集合
     */
    @GetMapping("/{batchId}/teaching-classes")
    public Result<List<TeachingClassRespDTO>> listBatchTeachingClass(@PathVariable Long batchId) {
        return Result.success(selectionBatchService.listBatchTeachingClass(batchId));
    }

    /**
     * 从批次移除教学班
     *
     * @param batchId         批次 ID
     * @param teachingClassId 教学班 ID
     * @return 空响应
     */
    @DeleteMapping("/{batchId}/teaching-classes/{teachingClassId}")
    public Result<Void> removeBatchTeachingClass(@PathVariable Long batchId, @PathVariable Long teachingClassId) {
        selectionBatchService.removeBatchTeachingClass(batchId, teachingClassId);
        return Result.success("移除成功", null);
    }
}
