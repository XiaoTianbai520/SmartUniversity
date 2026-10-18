package com.smart.university.controller.admin;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.dto.resp.CacheReconcileRespDTO;
import com.smart.university.service.CacheReconcileService;
import com.smart.university.service.SemesterService;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教务端缓存一致性对账接口（34.3）
 */
@Tag(name = "教务端-缓存一致性对账", description = "手动触发容量缓存与教学班 selected_count 的三方对账")
@RestController
@RequestMapping("/api/v1/admin")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminCacheReconcileController {

    private final CacheReconcileService cacheReconcileService;

    private final SemesterService semesterService;

    /**
     * 手动触发缓存一致性对账，以 DB 为准校正 Redis 计数与 selected_count
     *
     * @param semesterId 学期 ID，缺省取当前学期
     * @param limit      单次扫描上限，缺省 500
     * @return 对账结果
     */
    @Operation(summary = "手动触发缓存一致性对账",
            description = "以 DB 为准校正 Redis 计数与 selected_count，对账本身不产生超卖")
    @PostMapping("/cache/reconcile")
    public Result<CacheReconcileRespDTO> reconcile(
            @Parameter(example = "1") @RequestParam(required = false) Long semesterId,
            @Parameter(example = "500") @RequestParam(required = false, defaultValue = "500") Integer limit) {
        try {
            Long targetSemesterId = semesterId == null
                    ? semesterService.getCurrentSemester().getId()
                    : semesterId;
            CacheReconcileRespDTO result = cacheReconcileService.reconcile(targetSemesterId, limit);
            return Result.success("对账完成", result);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ResultCodeEnum.CACHE_RECONCILE_FAILED);
        }
    }
}
