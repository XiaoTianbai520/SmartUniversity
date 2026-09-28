package com.smart.university.controller.teacher;

import com.smart.university.common.base.Result;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.dto.resp.ImportResultRespDTO;
import com.smart.university.service.ImportExportService;
import com.smart.university.web.annotation.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 教师端批量导入导出接口，教学班维度接口均校验教学班归属
 */
@RestController
@RequestMapping("/api/v1/teacher/teaching-classes")
@RequireRole(RoleEnum.TEACHER)
@RequiredArgsConstructor
public class TeacherExcelController {

    private final ImportExportService importExportService;

    /**
     * 导出教学班已选学生名单
     *
     * @param teachingClassId 教学班 ID
     * @return xlsx 文件响应
     */
    @GetMapping("/{teachingClassId}/students/export")
    public ResponseEntity<byte[]> exportTeachingClassStudent(@PathVariable Long teachingClassId) {
        return importExportService.exportTeachingClassStudent(teachingClassId);
    }

    /**
     * 下载成绩导入模板，模板预置该教学班已选学生的学号与姓名
     *
     * @param teachingClassId 教学班 ID
     * @return xlsx 文件响应
     */
    @GetMapping("/{teachingClassId}/scores/import-template")
    public ResponseEntity<byte[]> downloadScoreImportTemplate(@PathVariable Long teachingClassId) {
        return importExportService.downloadScoreImportTemplate(teachingClassId);
    }

    /**
     * 批量导入教学班成绩
     *
     * @param teachingClassId 教学班 ID
     * @param file            上传的 xlsx 文件
     * @return 导入结果汇总与失败明细
     */
    @PostMapping("/{teachingClassId}/scores/import")
    public Result<ImportResultRespDTO> importScore(@PathVariable Long teachingClassId,
                                                   @RequestParam("file") MultipartFile file) {
        return Result.success(importExportService.importScore(teachingClassId, file));
    }
}
