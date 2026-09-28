package com.smart.university.service;

import com.smart.university.domain.dto.req.StudentPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeacherPageQueryReqDTO;
import com.smart.university.domain.dto.resp.ImportResultRespDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * 批量导入导出服务，覆盖学生、教师的 Excel 导入与导出，以及教师端教学班名单导出与成绩批量导入
 */
public interface ImportExportService {

    /**
     * 批量导入学生，复用学生新增能力创建登录账号
     *
     * @param file 上传的 xlsx 文件
     * @return 导入结果汇总与失败明细
     */
    ImportResultRespDTO importStudent(MultipartFile file);

    /**
     * 下载学生导入模板
     *
     * @return xlsx 文件响应
     */
    ResponseEntity<byte[]> downloadStudentImportTemplate();

    /**
     * 按条件导出学生信息
     *
     * @param requestParam 与学生分页查询一致的筛选条件
     * @return xlsx 文件响应
     */
    ResponseEntity<byte[]> exportStudent(StudentPageQueryReqDTO requestParam);

    /**
     * 批量导入教师，复用教师新增能力创建登录账号
     *
     * @param file 上传的 xlsx 文件
     * @return 导入结果汇总与失败明细
     */
    ImportResultRespDTO importTeacher(MultipartFile file);

    /**
     * 下载教师导入模板
     *
     * @return xlsx 文件响应
     */
    ResponseEntity<byte[]> downloadTeacherImportTemplate();

    /**
     * 按条件导出教师信息
     *
     * @param requestParam 筛选条件，包含关键字与状态
     * @param title        职称，为空表示不过滤
     * @return xlsx 文件响应
     */
    ResponseEntity<byte[]> exportTeacher(TeacherPageQueryReqDTO requestParam, String title);

    /**
     * 导出教学班已选学生名单，仅限教学班归属教师
     *
     * @param teachingClassId 教学班 ID
     * @return xlsx 文件响应
     */
    ResponseEntity<byte[]> exportTeachingClassStudent(Long teachingClassId);

    /**
     * 下载成绩导入模板，预置教学班已选学生的学号与姓名
     *
     * @param teachingClassId 教学班 ID
     * @return xlsx 文件响应
     */
    ResponseEntity<byte[]> downloadScoreImportTemplate(Long teachingClassId);

    /**
     * 批量导入教学班成绩，已发布的成绩不允许覆盖
     *
     * @param teachingClassId 教学班 ID
     * @param file            上传的 xlsx 文件
     * @return 导入结果汇总与失败明细
     */
    ImportResultRespDTO importScore(Long teachingClassId, MultipartFile file);
}
