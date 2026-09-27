package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.SysUserDO;
import com.smart.university.domain.entity.TeacherDO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.req.TeacherPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeacherSaveReqDTO;
import com.smart.university.domain.dto.resp.TeacherRespDTO;
import com.smart.university.mapper.SysUserMapper;
import com.smart.university.mapper.TeacherMapper;
import com.smart.university.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 教师服务实现
 */
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherMapper teacherMapper;

    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<TeacherRespDTO> pageTeacher(TeacherPageQueryReqDTO requestParam) {
        long total = teacherMapper.countTeacherByCondition(requestParam);
        if (total <= 0) {
            return PageResult.empty(requestParam.getCurrentPage(), requestParam.getLimit());
        }
        List<TeacherDO> teacherDOList = teacherMapper.listTeacherByCondition(requestParam);
        List<TeacherRespDTO> records = teacherDOList.stream().map(each -> {
            TeacherRespDTO result = BeanUtil.copyProperties(each, TeacherRespDTO.class);
            result.setTeacherId(each.getId());
            return result;
        }).toList();
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), total);
    }

    @Override
    public TeacherRespDTO getTeacherDetail(Long teacherId) {
        TeacherDO teacherDO = getTeacherById(teacherId);
        TeacherRespDTO result = BeanUtil.copyProperties(teacherDO, TeacherRespDTO.class);
        result.setTeacherId(teacherDO.getId());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveTeacher(TeacherSaveReqDTO requestParam) {
        if (sysUserMapper.countUserByUsername(requestParam.getUsername()) > 0) {
            throw new BizException(ResultCodeEnum.USERNAME_EXIST);
        }
        if (teacherMapper.countTeacherByTeacherNo(requestParam.getTeacherNo()) > 0) {
            throw new BizException(ResultCodeEnum.TEACHER_NO_EXIST);
        }
        SysUserDO sysUserDO = new SysUserDO();
        sysUserDO.setUsername(requestParam.getUsername());
        sysUserDO.setPasswordHash(BCrypt.hashpw(requestParam.getPassword(), BCrypt.gensalt()));
        sysUserDO.setRole(RoleEnum.TEACHER);
        sysUserDO.setStatus(1);
        sysUserMapper.saveUser(sysUserDO);

        TeacherDO teacherDO = new TeacherDO();
        teacherDO.setUserId(sysUserDO.getId());
        teacherDO.setTeacherNo(requestParam.getTeacherNo());
        teacherDO.setTeacherName(requestParam.getTeacherName());
        teacherDO.setTitle(requestParam.getTitle());
        teacherDO.setStatus(1);
        teacherMapper.saveTeacher(teacherDO);
        return teacherDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTeacher(TeacherSaveReqDTO requestParam) {
        TeacherDO teacherDO = getTeacherById(requestParam.getId());
        if (!teacherDO.getTeacherNo().equals(requestParam.getTeacherNo())
                && teacherMapper.countTeacherByTeacherNo(requestParam.getTeacherNo()) > 0) {
            throw new BizException(ResultCodeEnum.TEACHER_NO_EXIST);
        }
        TeacherDO updateTeacherDO = new TeacherDO();
        updateTeacherDO.setId(requestParam.getId());
        updateTeacherDO.setTeacherNo(requestParam.getTeacherNo());
        updateTeacherDO.setTeacherName(requestParam.getTeacherName());
        updateTeacherDO.setTitle(requestParam.getTitle());
        teacherMapper.updateTeacher(updateTeacherDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTeacherStatus(Long teacherId, StatusUpdateReqDTO requestParam) {
        TeacherDO teacherDO = getTeacherById(teacherId);
        TeacherDO updateTeacherDO = new TeacherDO();
        updateTeacherDO.setId(teacherId);
        updateTeacherDO.setStatus(requestParam.getStatus());
        teacherMapper.updateTeacher(updateTeacherDO);

        SysUserDO updateUserDO = new SysUserDO();
        updateUserDO.setId(teacherDO.getUserId());
        updateUserDO.setStatus(requestParam.getStatus());
        sysUserMapper.updateUser(updateUserDO);
    }

    @Override
    public TeacherDO getTeacherById(Long teacherId) {
        TeacherDO result = teacherMapper.getTeacherById(teacherId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.TEACHER_NOT_EXIST);
        }
        return result;
    }
}
