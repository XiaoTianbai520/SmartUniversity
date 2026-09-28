package com.smart.university.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.common.util.ExcelParseUtil;
import com.smart.university.common.util.ExcelWriteUtil;
import com.smart.university.domain.dto.excel.ScoreExcelRowDTO;
import com.smart.university.domain.dto.excel.StudentExcelRowDTO;
import com.smart.university.domain.dto.excel.TeacherExcelRowDTO;
import com.smart.university.domain.dto.req.StudentPageQueryReqDTO;
import com.smart.university.domain.dto.req.StudentSaveReqDTO;
import com.smart.university.domain.dto.req.TeacherPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeacherSaveReqDTO;
import com.smart.university.domain.dto.resp.ImportFailItemDTO;
import com.smart.university.domain.dto.resp.ImportResultRespDTO;
import com.smart.university.domain.entity.AcademicClassDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.GradeCohortDO;
import com.smart.university.domain.entity.MajorDO;
import com.smart.university.domain.entity.ScoreDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.SysUserDO;
import com.smart.university.domain.entity.TeacherDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.enums.ScoreStatusEnum;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.mapper.AcademicClassMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.GradeCohortMapper;
import com.smart.university.mapper.MajorMapper;
import com.smart.university.mapper.ScoreMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.mapper.SysUserMapper;
import com.smart.university.mapper.TeacherMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.service.ImportExportService;
import com.smart.university.service.StudentService;
import com.smart.university.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 批量导入导出服务实现。
 * 导入一律采用逐行独立处理：外层不加事务，单行失败只记录原因并继续，
 * 避免单行脏数据回滚整批数据，也避免被调用方的内部事务把外层标记为 rollback-only
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExportServiceImpl implements ImportExportService {

    /**
     * 导入未填写初始密码时使用的系统默认密码
     */
    private static final String DEFAULT_PASSWORD = "123456";

    /**
     * xlsx 内容类型
     */
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * 单次导出最大行数，避免大结果集拖垮内存
     */
    private static final long EXPORT_MAX_ROWS = 10000L;

    /**
     * 成绩下限
     */
    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;

    /**
     * 成绩上限
     */
    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    /**
     * 启用状态文本
     */
    private static final String ENABLED_TEXT = "正常";

    /**
     * 停用状态文本
     */
    private static final String DISABLED_TEXT = "停用";

    /**
     * 非业务异常时的兜底失败原因，避免把技术堆栈透给用户
     */
    private static final String UNEXPECTED_FAIL_REASON = "数据异常，请检查该行填写内容";

    private final StudentService studentService;

    private final TeacherService teacherService;

    private final StudentMapper studentMapper;

    private final TeacherMapper teacherMapper;

    private final SysUserMapper sysUserMapper;

    private final MajorMapper majorMapper;

    private final GradeCohortMapper gradeCohortMapper;

    private final AcademicClassMapper academicClassMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final CourseSelectionMapper courseSelectionMapper;

    private final ScoreMapper scoreMapper;

    @Override
    public ImportResultRespDTO importStudent(MultipartFile file) {
        List<ExcelParseUtil.ExcelRow> rowList = ExcelParseUtil.readDataRows(file, ExcelWriteUtil.STUDENT_IMPORT_HEADERS);
        if (CollUtil.isEmpty(rowList)) {
            throw new BizException(ResultCodeEnum.IMPORT_DATA_EMPTY);
        }
        List<ImportFailItemDTO> failList = new ArrayList<>();
        int successCount = 0;
        for (ExcelParseUtil.ExcelRow each : rowList) {
            String identifier = each.getCell(ExcelWriteUtil.STUDENT_NO_COLUMN_INDEX);
            try {
                studentService.saveStudent(buildStudentSaveParam(each));
                successCount++;
            } catch (BizException ex) {
                failList.add(buildFailItem(each.getRowIndex(), identifier, ex.getMessage()));
            } catch (Exception ex) {
                log.warn("导入学生第 {} 行处理异常", each.getRowIndex(), ex);
                failList.add(buildFailItem(each.getRowIndex(), identifier, UNEXPECTED_FAIL_REASON));
            }
        }
        return buildImportResult(rowList.size(), successCount, failList);
    }

    @Override
    public ResponseEntity<byte[]> downloadStudentImportTemplate() {
        byte[] content = ExcelWriteUtil.writeSheet("学生导入模板", ExcelWriteUtil.STUDENT_IMPORT_HEADERS, List.of());
        return buildExcelResponse(content, "学生导入模板.xlsx", "student-import-template.xlsx");
    }

    @Override
    public ResponseEntity<byte[]> exportStudent(StudentPageQueryReqDTO requestParam) {
        Page<StudentDO> page = Page.of(1L, EXPORT_MAX_ROWS);
        List<StudentDO> studentDOList = studentMapper.listStudentByCondition(page, requestParam).getRecords();
        if (CollUtil.isEmpty(studentDOList)) {
            throw new BizException(ResultCodeEnum.EXPORT_DATA_EMPTY);
        }
        Map<Long, MajorDO> majorMap = listMajorMap(studentDOList.stream().map(StudentDO::getMajorId).toList());
        Map<Long, GradeCohortDO> gradeMap = listGradeMap(studentDOList.stream().map(StudentDO::getGradeId).toList());
        Map<Long, AcademicClassDO> classMap = listClassMap(studentDOList.stream().map(StudentDO::getClassId).toList());
        Map<Long, SysUserDO> userMap = listUserMap(studentDOList.stream().map(StudentDO::getUserId).toList());
        List<List<String>> rows = studentDOList.stream().map(each -> {
            List<String> result = new ArrayList<>();
            result.add(each.getStudentNo());
            result.add(each.getStudentName());
            result.add(majorMap.get(each.getMajorId()) == null ? StrUtil.EMPTY
                    : majorMap.get(each.getMajorId()).getMajorName());
            result.add(gradeMap.get(each.getGradeId()) == null ? StrUtil.EMPTY
                    : gradeMap.get(each.getGradeId()).getGradeName());
            result.add(classMap.get(each.getClassId()) == null ? StrUtil.EMPTY
                    : classMap.get(each.getClassId()).getClassName());
            result.add(buildStatusText(each.getStatus()));
            SysUserDO sysUserDO = userMap.get(each.getUserId());
            result.add(sysUserDO == null ? StrUtil.EMPTY : buildStatusText(sysUserDO.getStatus()));
            return result;
        }).toList();
        byte[] content = ExcelWriteUtil.writeSheet("学生信息", ExcelWriteUtil.STUDENT_EXPORT_HEADERS, rows);
        return buildExcelResponse(content, "学生信息导出.xlsx", "students.xlsx");
    }

    @Override
    public ImportResultRespDTO importTeacher(MultipartFile file) {
        List<ExcelParseUtil.ExcelRow> rowList = ExcelParseUtil.readDataRows(file, ExcelWriteUtil.TEACHER_IMPORT_HEADERS);
        if (CollUtil.isEmpty(rowList)) {
            throw new BizException(ResultCodeEnum.IMPORT_DATA_EMPTY);
        }
        List<ImportFailItemDTO> failList = new ArrayList<>();
        int successCount = 0;
        for (ExcelParseUtil.ExcelRow each : rowList) {
            String identifier = each.getCell(ExcelWriteUtil.TEACHER_NO_COLUMN_INDEX);
            try {
                teacherService.saveTeacher(buildTeacherSaveParam(each));
                successCount++;
            } catch (BizException ex) {
                failList.add(buildFailItem(each.getRowIndex(), identifier, ex.getMessage()));
            } catch (Exception ex) {
                log.warn("导入教师第 {} 行处理异常", each.getRowIndex(), ex);
                failList.add(buildFailItem(each.getRowIndex(), identifier, UNEXPECTED_FAIL_REASON));
            }
        }
        return buildImportResult(rowList.size(), successCount, failList);
    }

    @Override
    public ResponseEntity<byte[]> downloadTeacherImportTemplate() {
        byte[] content = ExcelWriteUtil.writeSheet("教师导入模板", ExcelWriteUtil.TEACHER_IMPORT_HEADERS, List.of());
        return buildExcelResponse(content, "教师导入模板.xlsx", "teacher-import-template.xlsx");
    }

    @Override
    public ResponseEntity<byte[]> exportTeacher(TeacherPageQueryReqDTO requestParam, String title) {
        LambdaQueryWrapper<TeacherDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.and(StrUtil.isNotBlank(requestParam.getKeyword()), each -> each
                .like(TeacherDO::getTeacherNo, requestParam.getKeyword())
                .or().like(TeacherDO::getTeacherName, requestParam.getKeyword()));
        queryWrapper.eq(StrUtil.isNotBlank(title), TeacherDO::getTitle, title);
        queryWrapper.eq(requestParam.getStatus() != null, TeacherDO::getStatus, requestParam.getStatus());
        queryWrapper.orderByDesc(TeacherDO::getId);
        Page<TeacherDO> page = Page.of(1L, EXPORT_MAX_ROWS);
        List<TeacherDO> teacherDOList = teacherMapper.selectPage(page, queryWrapper).getRecords();
        if (CollUtil.isEmpty(teacherDOList)) {
            throw new BizException(ResultCodeEnum.EXPORT_DATA_EMPTY);
        }
        Map<Long, SysUserDO> userMap = listUserMap(teacherDOList.stream().map(TeacherDO::getUserId).toList());
        List<List<String>> rows = teacherDOList.stream().map(each -> {
            List<String> result = new ArrayList<>();
            result.add(each.getTeacherNo());
            result.add(each.getTeacherName());
            result.add(StrUtil.emptyToDefault(each.getTitle(), StrUtil.EMPTY));
            result.add(buildStatusText(each.getStatus()));
            SysUserDO sysUserDO = userMap.get(each.getUserId());
            result.add(sysUserDO == null ? StrUtil.EMPTY : buildStatusText(sysUserDO.getStatus()));
            return result;
        }).toList();
        byte[] content = ExcelWriteUtil.writeSheet("教师信息", ExcelWriteUtil.TEACHER_EXPORT_HEADERS, rows);
        return buildExcelResponse(content, "教师信息导出.xlsx", "teachers.xlsx");
    }

    @Override
    public ResponseEntity<byte[]> exportTeachingClassStudent(Long teachingClassId) {
        getOwnedTeachingClass(teachingClassId);
        List<CourseSelectionDO> selectionDOList = listSelectedSelection(teachingClassId);
        if (CollUtil.isEmpty(selectionDOList)) {
            throw new BizException(ResultCodeEnum.EXPORT_DATA_EMPTY);
        }
        Map<Long, StudentDO> studentMap = listStudentMap(selectionDOList);
        Map<Long, MajorDO> majorMap = listMajorMap(studentMap.values().stream().map(StudentDO::getMajorId).toList());
        Map<Long, AcademicClassDO> classMap = listClassMap(studentMap.values().stream().map(StudentDO::getClassId).toList());
        List<List<String>> rows = new ArrayList<>();
        for (CourseSelectionDO each : selectionDOList) {
            StudentDO studentDO = studentMap.get(each.getStudentId());
            if (studentDO == null) {
                continue;
            }
            List<String> rowData = new ArrayList<>();
            rowData.add(studentDO.getStudentNo());
            rowData.add(studentDO.getStudentName());
            rowData.add(majorMap.get(studentDO.getMajorId()) == null ? StrUtil.EMPTY
                    : majorMap.get(studentDO.getMajorId()).getMajorName());
            rowData.add(classMap.get(studentDO.getClassId()) == null ? StrUtil.EMPTY
                    : classMap.get(studentDO.getClassId()).getClassName());
            rows.add(rowData);
        }
        byte[] content = ExcelWriteUtil.writeSheet("教学班学生名单", ExcelWriteUtil.TEACHING_CLASS_STUDENT_HEADERS, rows);
        return buildExcelResponse(content, "教学班学生名单.xlsx", "teaching-class-students.xlsx");
    }

    @Override
    public ResponseEntity<byte[]> downloadScoreImportTemplate(Long teachingClassId) {
        getOwnedTeachingClass(teachingClassId);
        List<CourseSelectionDO> selectionDOList = listSelectedSelection(teachingClassId);
        Map<Long, StudentDO> studentMap = listStudentMap(selectionDOList);
        List<List<String>> rows = new ArrayList<>();
        for (CourseSelectionDO each : selectionDOList) {
            StudentDO studentDO = studentMap.get(each.getStudentId());
            if (studentDO == null) {
                continue;
            }
            List<String> rowData = new ArrayList<>();
            rowData.add(studentDO.getStudentNo());
            rowData.add(studentDO.getStudentName());
            rowData.add(StrUtil.EMPTY);
            rows.add(rowData);
        }
        byte[] content = ExcelWriteUtil.writeSheet("成绩导入模板", ExcelWriteUtil.SCORE_IMPORT_HEADERS, rows);
        return buildExcelResponse(content, "成绩导入模板.xlsx", "score-import-template.xlsx");
    }

    @Override
    public ImportResultRespDTO importScore(Long teachingClassId, MultipartFile file) {
        getOwnedTeachingClass(teachingClassId);
        List<ExcelParseUtil.ExcelRow> rowList = ExcelParseUtil.readDataRows(file, ExcelWriteUtil.SCORE_IMPORT_HEADERS);
        if (CollUtil.isEmpty(rowList)) {
            throw new BizException(ResultCodeEnum.IMPORT_DATA_EMPTY);
        }
        List<CourseSelectionDO> selectionDOList = listSelectedSelection(teachingClassId);
        Map<Long, StudentDO> studentMap = listStudentMap(selectionDOList);
        Map<String, CourseSelectionDO> selectionMap = new HashMap<>();
        for (CourseSelectionDO each : selectionDOList) {
            StudentDO studentDO = studentMap.get(each.getStudentId());
            if (studentDO != null) {
                selectionMap.put(studentDO.getStudentNo(), each);
            }
        }
        Map<Long, ScoreDO> scoreMap = listScoreMap(selectionDOList.stream().map(CourseSelectionDO::getId).toList());
        List<ImportFailItemDTO> failList = new ArrayList<>();
        int successCount = 0;
        for (ExcelParseUtil.ExcelRow each : rowList) {
            String identifier = each.getCell(ExcelWriteUtil.SCORE_STUDENT_NO_COLUMN_INDEX);
            try {
                saveRowScore(each, selectionMap, studentMap, scoreMap);
                successCount++;
            } catch (BizException ex) {
                failList.add(buildFailItem(each.getRowIndex(), identifier, ex.getMessage()));
            } catch (Exception ex) {
                log.warn("导入成绩第 {} 行处理异常", each.getRowIndex(), ex);
                failList.add(buildFailItem(each.getRowIndex(), identifier, UNEXPECTED_FAIL_REASON));
            }
        }
        return buildImportResult(rowList.size(), successCount, failList);
    }

    /**
     * 解析学生行并转换为学生新增入参，行内完成必填项与专业年级班级校验
     *
     * @param row Excel 数据行
     * @return 学生新增入参
     */
    private StudentSaveReqDTO buildStudentSaveParam(ExcelParseUtil.ExcelRow row) {
        StudentExcelRowDTO rowDTO = new StudentExcelRowDTO();
        rowDTO.setRowIndex(row.getRowIndex());
        rowDTO.setStudentNo(row.getCell(ExcelWriteUtil.STUDENT_NO_COLUMN_INDEX));
        rowDTO.setStudentName(row.getCell(ExcelWriteUtil.STUDENT_NAME_COLUMN_INDEX));
        rowDTO.setMajorCode(row.getCell(ExcelWriteUtil.MAJOR_CODE_COLUMN_INDEX));
        rowDTO.setGradeName(row.getCell(ExcelWriteUtil.GRADE_NAME_COLUMN_INDEX));
        rowDTO.setClassCode(row.getCell(ExcelWriteUtil.CLASS_CODE_COLUMN_INDEX));
        rowDTO.setPassword(row.getCell(ExcelWriteUtil.STUDENT_PASSWORD_COLUMN_INDEX));
        checkStudentRowRequired(rowDTO);
        MajorDO majorDO = getMajorByMajorCode(rowDTO.getMajorCode());
        GradeCohortDO gradeCohortDO = getGradeByGradeName(rowDTO.getGradeName());
        AcademicClassDO academicClassDO = getAcademicClassByClassCode(rowDTO.getClassCode());
        checkAcademicClassMatch(majorDO, gradeCohortDO, academicClassDO);
        StudentSaveReqDTO result = new StudentSaveReqDTO();
        result.setUsername(rowDTO.getStudentNo());
        result.setPassword(StrUtil.isBlank(rowDTO.getPassword()) ? DEFAULT_PASSWORD : rowDTO.getPassword());
        result.setStudentNo(rowDTO.getStudentNo());
        result.setStudentName(rowDTO.getStudentName());
        result.setMajorId(majorDO.getId());
        result.setGradeId(gradeCohortDO.getId());
        result.setClassId(academicClassDO.getId());
        return result;
    }

    /**
     * 校验学生导入行的必填列
     *
     * @param rowDTO 学生行模型
     */
    private void checkStudentRowRequired(StudentExcelRowDTO rowDTO) {
        if (StrUtil.isBlank(rowDTO.getStudentNo())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, buildNotBlankMessage(ExcelWriteUtil.STUDENT_NO_COLUMN_INDEX));
        }
        if (StrUtil.isBlank(rowDTO.getStudentName())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, buildNotBlankMessage(ExcelWriteUtil.STUDENT_NAME_COLUMN_INDEX));
        }
        if (StrUtil.isBlank(rowDTO.getMajorCode())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, buildNotBlankMessage(ExcelWriteUtil.MAJOR_CODE_COLUMN_INDEX));
        }
        if (StrUtil.isBlank(rowDTO.getGradeName())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, buildNotBlankMessage(ExcelWriteUtil.GRADE_NAME_COLUMN_INDEX));
        }
        if (StrUtil.isBlank(rowDTO.getClassCode())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, buildNotBlankMessage(ExcelWriteUtil.CLASS_CODE_COLUMN_INDEX));
        }
    }

    /**
     * 解析教师行并转换为教师新增入参
     *
     * @param row Excel 数据行
     * @return 教师新增入参
     */
    private TeacherSaveReqDTO buildTeacherSaveParam(ExcelParseUtil.ExcelRow row) {
        TeacherExcelRowDTO rowDTO = new TeacherExcelRowDTO();
        rowDTO.setRowIndex(row.getRowIndex());
        rowDTO.setTeacherNo(row.getCell(ExcelWriteUtil.TEACHER_NO_COLUMN_INDEX));
        rowDTO.setTeacherName(row.getCell(ExcelWriteUtil.TEACHER_NAME_COLUMN_INDEX));
        rowDTO.setTitle(row.getCell(ExcelWriteUtil.TEACHER_TITLE_COLUMN_INDEX));
        rowDTO.setPassword(row.getCell(ExcelWriteUtil.TEACHER_PASSWORD_COLUMN_INDEX));
        if (StrUtil.isBlank(rowDTO.getTeacherNo())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, buildNotBlankMessage(ExcelWriteUtil.TEACHER_NO_COLUMN_INDEX));
        }
        if (StrUtil.isBlank(rowDTO.getTeacherName())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, buildNotBlankMessage(ExcelWriteUtil.TEACHER_NAME_COLUMN_INDEX));
        }
        TeacherSaveReqDTO result = new TeacherSaveReqDTO();
        result.setUsername(rowDTO.getTeacherNo());
        result.setPassword(StrUtil.isBlank(rowDTO.getPassword()) ? DEFAULT_PASSWORD : rowDTO.getPassword());
        result.setTeacherNo(rowDTO.getTeacherNo());
        result.setTeacherName(rowDTO.getTeacherName());
        result.setTitle(rowDTO.getTitle());
        return result;
    }

    /**
     * 按行落库成绩，已有记录更新、无记录插入
     *
     * @param row           Excel 数据行
     * @param selectionMap  学号与已选记录的映射
     * @param studentMap    学生 ID 与学生信息的映射
     * @param scoreMap      选课记录 ID 与已有成绩的映射
     */
    private void saveRowScore(ExcelParseUtil.ExcelRow row, Map<String, CourseSelectionDO> selectionMap,
                              Map<Long, StudentDO> studentMap, Map<Long, ScoreDO> scoreMap) {
        ScoreExcelRowDTO rowDTO = new ScoreExcelRowDTO();
        rowDTO.setRowIndex(row.getRowIndex());
        rowDTO.setStudentNo(row.getCell(ExcelWriteUtil.SCORE_STUDENT_NO_COLUMN_INDEX));
        rowDTO.setStudentName(row.getCell(ExcelWriteUtil.SCORE_STUDENT_NAME_COLUMN_INDEX));
        rowDTO.setScore(row.getCell(ExcelWriteUtil.SCORE_VALUE_COLUMN_INDEX));
        if (StrUtil.isBlank(rowDTO.getStudentNo())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, buildNotBlankMessage(ExcelWriteUtil.SCORE_STUDENT_NO_COLUMN_INDEX));
        }
        CourseSelectionDO selectionDO = selectionMap.get(rowDTO.getStudentNo());
        if (selectionDO == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_IN_CLASS);
        }
        StudentDO studentDO = studentMap.get(selectionDO.getStudentId());
        if (studentDO != null && StrUtil.isNotBlank(rowDTO.getStudentName())
                && !rowDTO.getStudentName().equals(studentDO.getStudentName())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, StrUtil.format("第 {} 列姓名与学号 {} 对应的学生不一致",
                    ExcelWriteUtil.SCORE_STUDENT_NAME_COLUMN_INDEX + 1, rowDTO.getStudentNo()));
        }
        BigDecimal scoreValue = parseScoreValue(rowDTO.getScore());
        ScoreDO existScoreDO = scoreMap.get(selectionDO.getId());
        if (existScoreDO != null && ScoreStatusEnum.PUBLISHED == existScoreDO.getStatus()) {
            throw new BizException(ResultCodeEnum.SCORE_PUBLISHED);
        }
        if (existScoreDO == null) {
            ScoreDO scoreDO = new ScoreDO();
            scoreDO.setCourseSelectionId(selectionDO.getId());
            scoreDO.setScoreValue(scoreValue);
            scoreDO.setStatus(ScoreStatusEnum.UNPUBLISHED);
            scoreMapper.saveScore(scoreDO);
            scoreMap.put(selectionDO.getId(), scoreDO);
            return;
        }
        ScoreDO updateScoreDO = new ScoreDO();
        updateScoreDO.setId(existScoreDO.getId());
        updateScoreDO.setScoreValue(scoreValue);
        scoreMapper.updateScore(updateScoreDO);
    }

    /**
     * 解析成绩文本并校验取值范围
     *
     * @param scoreText 成绩文本
     * @return 成绩数值
     */
    private BigDecimal parseScoreValue(String scoreText) {
        if (!NumberUtil.isNumber(scoreText)) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, StrUtil.format("第 {} 列成绩格式错误，必须为数字",
                    ExcelWriteUtil.SCORE_VALUE_COLUMN_INDEX + 1));
        }
        BigDecimal result = new BigDecimal(scoreText);
        if (result.compareTo(MIN_SCORE) < 0 || result.compareTo(MAX_SCORE) > 0) {
            throw new BizException(ResultCodeEnum.SCORE_IMPORT_OUT_OF_RANGE);
        }
        return result;
    }

    /**
     * 校验行政班级与专业、年级互相匹配
     *
     * @param majorDO          专业
     * @param gradeCohortDO    年级
     * @param academicClassDO  行政班级
     */
    private void checkAcademicClassMatch(MajorDO majorDO, GradeCohortDO gradeCohortDO, AcademicClassDO academicClassDO) {
        if (!academicClassDO.getMajorId().equals(majorDO.getId())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, StrUtil.format("第 {} 列班级编号 {} 与第 {} 列专业编号 {} 不匹配",
                    ExcelWriteUtil.CLASS_CODE_COLUMN_INDEX + 1, academicClassDO.getClassCode(),
                    ExcelWriteUtil.MAJOR_CODE_COLUMN_INDEX + 1, majorDO.getMajorCode()));
        }
        if (!academicClassDO.getGradeId().equals(gradeCohortDO.getId())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, StrUtil.format("第 {} 列班级编号 {} 与第 {} 列年级名称 {} 不匹配",
                    ExcelWriteUtil.CLASS_CODE_COLUMN_INDEX + 1, academicClassDO.getClassCode(),
                    ExcelWriteUtil.GRADE_NAME_COLUMN_INDEX + 1, gradeCohortDO.getGradeName()));
        }
    }

    /**
     * 按专业编号查询专业
     *
     * @param majorCode 专业编号
     * @return 专业信息
     */
    private MajorDO getMajorByMajorCode(String majorCode) {
        MajorDO result = majorMapper.getMajorByMajorCode(majorCode);
        if (result == null) {
            throw new BizException(ResultCodeEnum.MAJOR_NOT_EXIST, StrUtil.format("第 {} 列专业编号 {} 不存在",
                    ExcelWriteUtil.MAJOR_CODE_COLUMN_INDEX + 1, majorCode));
        }
        return result;
    }

    /**
     * 按年级名称查询年级
     *
     * @param gradeName 年级名称
     * @return 年级信息
     */
    private GradeCohortDO getGradeByGradeName(String gradeName) {
        GradeCohortDO result = gradeCohortMapper.selectOne(Wrappers.<GradeCohortDO>lambdaQuery()
                .eq(GradeCohortDO::getGradeName, gradeName)
                .last("LIMIT 1"));
        if (result == null) {
            throw new BizException(ResultCodeEnum.GRADE_NOT_EXIST, StrUtil.format("第 {} 列年级名称 {} 不存在",
                    ExcelWriteUtil.GRADE_NAME_COLUMN_INDEX + 1, gradeName));
        }
        return result;
    }

    /**
     * 按班级编号查询行政班级
     *
     * @param classCode 班级编号
     * @return 行政班级信息
     */
    private AcademicClassDO getAcademicClassByClassCode(String classCode) {
        AcademicClassDO result = academicClassMapper.selectOne(Wrappers.<AcademicClassDO>lambdaQuery()
                .eq(AcademicClassDO::getClassCode, classCode)
                .last("LIMIT 1"));
        if (result == null) {
            throw new BizException(ResultCodeEnum.ACADEMIC_CLASS_NOT_EXIST, StrUtil.format("第 {} 列班级编号 {} 不存在",
                    ExcelWriteUtil.CLASS_CODE_COLUMN_INDEX + 1, classCode));
        }
        return result;
    }

    /**
     * 校验教学班存在且归属当前登录教师
     *
     * @param teachingClassId 教学班 ID
     * @return 教学班信息
     */
    private TeachingClassDO getOwnedTeachingClass(Long teachingClassId) {
        Long teacherId = UserContextHolder.getTeacherId();
        if (teacherId == null) {
            throw new BizException(ResultCodeEnum.NO_TEACHING_CLASS_PERMISSION);
        }
        TeachingClassDO result = teachingClassMapper.getTeachingClassById(teachingClassId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.TEACHING_CLASS_NOT_EXIST);
        }
        if (!teacherId.equals(result.getTeacherId())) {
            throw new BizException(ResultCodeEnum.NO_TEACHING_CLASS_PERMISSION);
        }
        return result;
    }

    /**
     * 查询教学班内已选状态的选课记录
     *
     * @param teachingClassId 教学班 ID
     * @return 选课记录集合
     */
    private List<CourseSelectionDO> listSelectedSelection(Long teachingClassId) {
        CourseSelectionDO requestParam = new CourseSelectionDO();
        requestParam.setTeachingClassId(teachingClassId);
        requestParam.setStatus(SelectionStatusEnum.SELECTED);
        return courseSelectionMapper.listSelectionByCondition(requestParam);
    }

    /**
     * 批量构建学生映射
     *
     * @param selectionDOList 选课记录集合
     * @return 学生 ID 与学生信息的映射
     */
    private Map<Long, StudentDO> listStudentMap(List<CourseSelectionDO> selectionDOList) {
        List<Long> studentIds = selectionDOList.stream().map(CourseSelectionDO::getStudentId).distinct().toList();
        if (CollUtil.isEmpty(studentIds)) {
            return Map.of();
        }
        return studentMapper.listStudentByIds(studentIds).stream()
                .collect(Collectors.toMap(StudentDO::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 批量构建专业映射
     *
     * @param majorIds 专业 ID 集合
     * @return 专业 ID 与专业信息的映射
     */
    private Map<Long, MajorDO> listMajorMap(List<Long> majorIds) {
        List<Long> distinctIds = majorIds.stream().filter(each -> each != null).distinct().toList();
        if (CollUtil.isEmpty(distinctIds)) {
            return Map.of();
        }
        return majorMapper.listMajorByIds(distinctIds).stream()
                .collect(Collectors.toMap(MajorDO::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 批量构建年级映射
     *
     * @param gradeIds 年级 ID 集合
     * @return 年级 ID 与年级信息的映射
     */
    private Map<Long, GradeCohortDO> listGradeMap(List<Long> gradeIds) {
        List<Long> distinctIds = gradeIds.stream().filter(each -> each != null).distinct().toList();
        if (CollUtil.isEmpty(distinctIds)) {
            return Map.of();
        }
        return gradeCohortMapper.listGradeCohortByIds(distinctIds).stream()
                .collect(Collectors.toMap(GradeCohortDO::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 批量构建行政班级映射
     *
     * @param classIds 行政班级 ID 集合
     * @return 行政班级 ID 与班级信息的映射
     */
    private Map<Long, AcademicClassDO> listClassMap(List<Long> classIds) {
        List<Long> distinctIds = classIds.stream().filter(each -> each != null).distinct().toList();
        if (CollUtil.isEmpty(distinctIds)) {
            return Map.of();
        }
        return academicClassMapper.listAcademicClassByIds(distinctIds).stream()
                .collect(Collectors.toMap(AcademicClassDO::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 批量构建系统用户映射，用于导出账号状态
     *
     * @param userIds 用户 ID 集合
     * @return 用户 ID 与用户信息的映射
     */
    private Map<Long, SysUserDO> listUserMap(List<Long> userIds) {
        List<Long> distinctIds = userIds.stream().filter(each -> each != null).distinct().toList();
        if (CollUtil.isEmpty(distinctIds)) {
            return Map.of();
        }
        return sysUserMapper.selectList(Wrappers.<SysUserDO>lambdaQuery().in(SysUserDO::getId, distinctIds)).stream()
                .collect(Collectors.toMap(SysUserDO::getId, Function.identity(), (first, second) -> first));
    }

    /**
     * 批量构建已有成绩映射
     *
     * @param selectionIds 选课记录 ID 集合
     * @return 选课记录 ID 与成绩的映射
     */
    private Map<Long, ScoreDO> listScoreMap(List<Long> selectionIds) {
        List<Long> distinctIds = selectionIds.stream().filter(each -> each != null).distinct().toList();
        if (CollUtil.isEmpty(distinctIds)) {
            return Map.of();
        }
        return scoreMapper.listScoreBySelectionIds(distinctIds).stream()
                .collect(Collectors.toMap(ScoreDO::getCourseSelectionId, Function.identity(), (first, second) -> first));
    }

    /**
     * 构造导入结果
     *
     * @param totalCount 总行数
     * @param successCount 成功行数
     * @param failList   失败明细
     * @return 导入结果
     */
    private ImportResultRespDTO buildImportResult(int totalCount, int successCount, List<ImportFailItemDTO> failList) {
        ImportResultRespDTO result = new ImportResultRespDTO();
        result.setTotalCount(totalCount);
        result.setSuccessCount(successCount);
        result.setFailCount(failList.size());
        result.setFailList(failList);
        return result;
    }

    /**
     * 构造导入失败明细
     *
     * @param rowIndex   行号
     * @param identifier 业务标识
     * @param reason     失败原因
     * @return 失败明细
     */
    private ImportFailItemDTO buildFailItem(int rowIndex, String identifier, String reason) {
        ImportFailItemDTO result = new ImportFailItemDTO();
        result.setRowIndex(rowIndex);
        result.setIdentifier(identifier);
        result.setReason(reason);
        return result;
    }

    /**
     * 构造必填列的失败提示
     *
     * @param columnIndex 列下标
     * @return 失败提示
     */
    private String buildNotBlankMessage(int columnIndex) {
        return StrUtil.format("第 {} 列不能为空", columnIndex + 1);
    }

    /**
     * 状态文本转换，1 为正常，其余为停用
     *
     * @param status 状态值
     * @return 状态文本
     */
    private String buildStatusText(Integer status) {
        return status != null && status == 1 ? ENABLED_TEXT : DISABLED_TEXT;
    }

    /**
     * 构造 xlsx 附件下载响应，中文名走 RFC 5987 的 filename* 扩展
     *
     * @param content        xlsx 字节数组
     * @param fileName       中文文件名
     * @param asciiFileName  纯 ASCII 兜底文件名
     * @return 文件下载响应
     */
    private ResponseEntity<byte[]> buildExcelResponse(byte[] content, String fileName, String asciiFileName) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, StrUtil.format("attachment; filename=\"{}\"; filename*=UTF-8''{}",
                asciiFileName, encodedFileName));
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
}
