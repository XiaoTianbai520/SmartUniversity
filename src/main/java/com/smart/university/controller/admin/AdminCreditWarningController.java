package com.smart.university.controller.admin;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.GraduationRiskQueryReqDTO;
import com.smart.university.domain.dto.resp.GraduationRiskPageRespDTO;
import com.smart.university.service.GraduationRiskService;
import com.smart.university.service.SemesterService;
import com.smart.university.web.annotation.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教务端学分预警 / 毕业风险名单接口（42.1）
 *
 * <p>数据口径复用 40.1 的 {@link GraduationRiskService#pageGraduationRisk}，为跨学期累计（C8），
 * 与 36.1 / 36.2 同源同值。T9 裁定：统一使用 {@code GraduationRiskPageRespDTO} 作为出参，
 * 删除原 §11.5 登记的冗余 {@code CreditWarningRespDTO}，保证接口可用、口径一致。
 */
@Tag(name = "教务端-学分预警", description = "学分不足 / 毕业风险名单，管理员视角查询，数据口径与 40.1 一致")
@RestController
@RequestMapping("/api/v1/admin")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminCreditWarningController {

    private final GraduationRiskService graduationRiskService;

    private final SemesterService semesterService;

    /**
     * 分页查询学分预警 / 毕业风险名单
     *
     * @param requestParam 查询条件（学期 / 专业 / 年级 / 风险等级 / 分页）
     * @return 毕业风险名单分页结果与口径回显
     */
    @Operation(summary = "学分预警 / 毕业风险名单", description = "学期缺省取当前学期，其余筛选条件可选")
    @GetMapping("/warnings/credit")
    public Result<GraduationRiskPageRespDTO> pageCreditWarning(GraduationRiskQueryReqDTO requestParam) {
        if (requestParam.getSemesterId() == null) {
            requestParam.setSemesterId(semesterService.getCurrentSemester().getId());
        }
        return Result.success(graduationRiskService.pageGraduationRisk(requestParam));
    }
}
