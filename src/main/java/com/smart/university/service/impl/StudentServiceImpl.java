package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.AcademicClassDO;
import com.smart.university.domain.entity.MajorDO;
import com.smart.university.domain.entity.GradeCohortDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.SysUserDO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.req.StudentPageQueryReqDTO;
import com.smart.university.domain.dto.req.StudentSaveReqDTO;
import com.smart.university.domain.dto.resp.StudentRespDTO;
import com.smart.university.mapper.AcademicClassMapper;
import com.smart.university.mapper.GradeCohortMapper;
import com.smart.university.mapper.MajorMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.mapper.SysUserMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.university.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学生服务实现
 */
@Service
@RequiredArgsConstructor
public class StudentServiceImpl extends ServiceImpl<StudentMapper, StudentDO> implements StudentService {

    private final StudentMapper studentMapper;

    private final SysUserMapper sysUserMapper;

    private final MajorMapper majorMapper;

    private final GradeCohortMapper gradeCohortMapper;

    private final AcademicClassMapper academicClassMapper;

    @Override
    public PageResult<StudentRespDTO> pageStudent(StudentPageQueryReqDTO requestParam) {
        Page<StudentDO> page = Page.of(requestParam.getCurrentPage(), requestParam.getLimit());
        IPage<StudentDO> pageResult = studentMapper.listStudentByCondition(page, requestParam);
        List<StudentDO> studentDOList = pageResult.getRecords();
        List<StudentRespDTO> records = convertToRespDTO(studentDOList);
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), pageResult.getTotal());
    }

    @Override
    public StudentRespDTO getStudentDetail(Long studentId) {
        StudentDO studentDO = getStudentById(studentId);
        return convertToRespDTO(Collections.singletonList(studentDO)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveStudent(StudentSaveReqDTO requestParam) {
        if (sysUserMapper.countUserByUsername(requestParam.getUsername()) > 0) {
            throw new BizException(ResultCodeEnum.USERNAME_EXIST);
        }
        if (studentMapper.countStudentByStudentNo(requestParam.getStudentNo()) > 0) {
            throw new BizException(ResultCodeEnum.STUDENT_NO_EXIST);
        }
        checkMajorAndGradeAndClass(requestParam);
        SysUserDO sysUserDO = new SysUserDO();
        sysUserDO.setUsername(requestParam.getUsername());
        sysUserDO.setPasswordHash(BCrypt.hashpw(requestParam.getPassword(), BCrypt.gensalt()));
        sysUserDO.setRole(RoleEnum.STUDENT);
        sysUserDO.setStatus(1);
        sysUserMapper.saveUser(sysUserDO);

        StudentDO studentDO = new StudentDO();
        studentDO.setUserId(sysUserDO.getId());
        studentDO.setStudentNo(requestParam.getStudentNo());
        studentDO.setStudentName(requestParam.getStudentName());
        studentDO.setMajorId(requestParam.getMajorId());
        studentDO.setGradeId(requestParam.getGradeId());
        studentDO.setClassId(requestParam.getClassId());
        studentDO.setStatus(1);
        studentMapper.saveStudent(studentDO);
        return studentDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStudent(StudentSaveReqDTO requestParam) {
        StudentDO studentDO = getStudentById(requestParam.getId());
        if (!studentDO.getStudentNo().equals(requestParam.getStudentNo())
                && studentMapper.countStudentByStudentNo(requestParam.getStudentNo()) > 0) {
            throw new BizException(ResultCodeEnum.STUDENT_NO_EXIST);
        }
        checkMajorAndGradeAndClass(requestParam);
        StudentDO updateStudentDO = new StudentDO();
        updateStudentDO.setId(requestParam.getId());
        updateStudentDO.setStudentNo(requestParam.getStudentNo());
        updateStudentDO.setStudentName(requestParam.getStudentName());
        updateStudentDO.setMajorId(requestParam.getMajorId());
        updateStudentDO.setGradeId(requestParam.getGradeId());
        updateStudentDO.setClassId(requestParam.getClassId());
        studentMapper.updateStudent(updateStudentDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStudentStatus(Long studentId, StatusUpdateReqDTO requestParam) {
        StudentDO studentDO = getStudentById(studentId);
        StudentDO updateStudentDO = new StudentDO();
        updateStudentDO.setId(studentId);
        updateStudentDO.setStatus(requestParam.getStatus());
        studentMapper.updateStudent(updateStudentDO);

        SysUserDO updateUserDO = new SysUserDO();
        updateUserDO.setId(studentDO.getUserId());
        updateUserDO.setStatus(requestParam.getStatus());
        sysUserMapper.updateUser(updateUserDO);
    }

    @Override
    public StudentDO getStudentByUserId(Long userId) {
        return studentMapper.getStudentByUserId(userId);
    }

    @Override
    public StudentDO getStudentById(Long studentId) {
        StudentDO result = studentMapper.getStudentById(studentId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        return result;
    }

    /**
     * 校验专业、年级、行政班存在且互相一致
     *
     * @param requestParam 学生入参
     */
    private void checkMajorAndGradeAndClass(StudentSaveReqDTO requestParam) {
        MajorDO majorDO = majorMapper.getMajorById(requestParam.getMajorId());
        if (majorDO == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "专业不存在");
        }
        GradeCohortDO gradeCohortDO = gradeCohortMapper.getGradeCohortById(requestParam.getGradeId());
        if (gradeCohortDO == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "年级不存在");
        }
        AcademicClassDO academicClassDO = academicClassMapper.getAcademicClassById(requestParam.getClassId());
        if (academicClassDO == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "行政班级不存在");
        }
        if (!academicClassDO.getMajorId().equals(requestParam.getMajorId())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "班级专业与学生专业不一致");
        }
        if (!academicClassDO.getGradeId().equals(requestParam.getGradeId())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "班级年级与学生年级不一致");
        }
    }

    /**
     * 批量转换为出参，并补齐专业、年级、班级名称
     *
     * @param studentDOList 学生数据对象集合
     * @return 学生出参集合
     */
    private List<StudentRespDTO> convertToRespDTO(List<StudentDO> studentDOList) {
        Map<Long, MajorDO> majorMap = majorMapper
                .listMajorByIds(studentDOList.stream().map(StudentDO::getMajorId).distinct().toList())
                .stream().collect(Collectors.toMap(MajorDO::getId, Function.identity()));
        Map<Long, GradeCohortDO> gradeMap = gradeCohortMapper
                .listGradeCohortByIds(studentDOList.stream().map(StudentDO::getGradeId).distinct().toList())
                .stream().collect(Collectors.toMap(GradeCohortDO::getId, Function.identity()));
        Map<Long, AcademicClassDO> classMap = academicClassMapper
                .listAcademicClassByIds(studentDOList.stream().map(StudentDO::getClassId).distinct().toList())
                .stream().collect(Collectors.toMap(AcademicClassDO::getId, Function.identity()));
        return studentDOList.stream().map(each -> {
            StudentRespDTO result = BeanUtil.copyProperties(each, StudentRespDTO.class);
            result.setStudentId(each.getId());
            MajorDO majorDO = majorMap.get(each.getMajorId());
            GradeCohortDO gradeCohortDO = gradeMap.get(each.getGradeId());
            AcademicClassDO academicClassDO = classMap.get(each.getClassId());
            result.setMajorName(majorDO == null ? null : majorDO.getMajorName());
            result.setGradeName(gradeCohortDO == null ? null : gradeCohortDO.getGradeName());
            result.setClassName(academicClassDO == null ? null : academicClassDO.getClassName());
            return result;
        }).toList();
    }
}
