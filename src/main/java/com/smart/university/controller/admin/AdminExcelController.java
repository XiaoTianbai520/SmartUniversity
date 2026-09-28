package com.smart.university.controller.admin;
import io.swagger.v3.oas.annotations.Parameter;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.req.StudentPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeacherPageQueryReqDTO;
import com.smart.university.domain.dto.resp.ImportResultRespDTO;
import com.smart.university.service.ImportExportService;
import com.smart.university.web.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 教务端批量导入导出接口
 */
@Tag(name = "教务端-批量导入导出", description = "学生与教师的 Excel 导入、模板下载与条件导出")
@RestController
@RequestMapping("/api/v1/admin")
@RequireRole(RoleEnum.ADMIN)
@RequiredArgsConstructor
public class AdminExcelController {

    private final ImportExportService importExportService;

    /**
     * 批量导入学生
     *
     * @param file 上传的 xlsx 文件
     * @return 导入结果汇总与失败明细
     */
    @Operation(summary = "批量导入学生", description = "上传 xlsx，逐行独立事务导入，返回汇总与失败明细")
    @PostMapping("/students/import")
    public Result<ImportResultRespDTO> importStudent(@RequestParam("file") MultipartFile file) {
        return Result.success(importExportService.importStudent(file));
    }

    /**
     * 下载学生导入模板
     *
     * @return xlsx 文件响应
     */
    @Operation(summary = "下载学生导入模板")
    @GetMapping("/students/import-template")
    public ResponseEntity<byte[]> downloadStudentImportTemplate() {
        return importExportService.downloadStudentImportTemplate();
    }

    /**
     * 按条件导出学生信息
     *
     * @param requestParam 与学生分页查询一致的筛选条件
     * @return xlsx 文件响应
     */
    @Operation(summary = "导出学生信息", description = "按筛选条件导出学生 xlsx")
    @GetMapping("/students/export")
    public ResponseEntity<byte[]> exportStudent(StudentPageQueryReqDTO requestParam) {
        return importExportService.exportStudent(requestParam);
    }

    /**
     * 批量导入教师
     *
     * @param file 上传的 xlsx 文件
     * @return 导入结果汇总与失败明细
     */
    @Operation(summary = "批量导入教师", description = "上传 xlsx，逐行独立事务导入，返回汇总与失败明细")
    @PostMapping("/teachers/import")
    public Result<ImportResultRespDTO> importTeacher(@RequestParam("file") MultipartFile file) {
        return Result.success(importExportService.importTeacher(file));
    }

    /**
     * 下载教师导入模板
     *
     * @return xlsx 文件响应
     */
    @Operation(summary = "下载教师导入模板")
    @GetMapping("/teachers/import-template")
    public ResponseEntity<byte[]> downloadTeacherImportTemplate() {
        return importExportService.downloadTeacherImportTemplate();
    }

    /**
     * 按条件导出教师信息
     *
     * @param requestParam 筛选条件，包含关键字与状态
     * @param title        职称，为空表示不过滤
     * @return xlsx 文件响应
     */
    @Operation(summary = "导出教师信息", description = "按筛选条件与职称导出教师 xlsx")
    @GetMapping("/teachers/export")
    public ResponseEntity<byte[]> exportTeacher(TeacherPageQueryReqDTO requestParam,
                                                @Parameter(example = "测试导出") @RequestParam(required = false) String title) {
        return importExportService.exportTeacher(requestParam, title);
    }
}
