package com.smart.university.controller.admin;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.dto.resp.CacheStatusRespDTO;
import com.smart.university.domain.dto.resp.CacheWarmupRespDTO;
import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.mapper.SelectionBatchMapper;
import com.smart.university.service.SelectionCapacityService;
import com.smart.university.service.SemesterService;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教务端选课容量缓存接口（34.1 / 34.2）
 */
@Tag(name = "教务端-选课容量缓存", description = "容量计数预热与缓存状态查询")
@RestController
@RequestMapping("/api/v1/admin")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminSelectionCacheController {

    private final SelectionCapacityService selectionCapacityService;

    private final SelectionBatchMapper selectionBatchMapper;

    private final SemesterService semesterService;

    /**
     * 预热指定批次的容量计数
     *
     * @param batchId 选课批次 ID
     * @return 预热结果
     */
    @Operation(summary = "预热教学班容量计数", description = "以 DB 权威已选人数覆盖写容量 Key，重复执行幂等")
    @PostMapping("/selection-batches/{batchId}/cache/warmup")
    public Result<CacheWarmupRespDTO> warmupCache(@Parameter(example = "1") @PathVariable Long batchId) {
        SelectionBatchDO batchDO = selectionBatchMapper.getSelectionBatchById(batchId);
        if (batchDO == null) {
            throw new BizException(ResultCodeEnum.SELECTION_BATCH_NOT_EXIST);
        }
        return Result.success("预热完成", selectionCapacityService.warmupByBatchId(batchId));
    }

    /**
     * 查询容量缓存状态
     *
     * @param semesterId 学期 ID，缺省当前学期
     * @return 缓存状态
     */
    @Operation(summary = "查询容量缓存状态", description = "含 Redis 降级标记、已缓存 Key 数与抽样权威人数")
    @GetMapping("/cache/status")
    public Result<CacheStatusRespDTO> getCacheStatus(
            @Parameter(example = "1") @RequestParam(required = false) Long semesterId) {
        Long targetSemesterId = semesterId == null
                ? semesterService.getCurrentSemester().getId()
                : semesterId;
        return Result.success(selectionCapacityService.getCacheStatus(targetSemesterId));
    }
}
