package com.smart.university.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.json.JSONUtil;
import com.smart.university.common.context.UserContext;
import com.smart.university.common.context.UserContextHolder;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.common.util.JwtUtil;
import com.smart.university.common.util.RedisKeyUtil;
import com.smart.university.common.constant.RedisCommonConstant;
import com.smart.university.domain.entity.AcademicClassDO;
import com.smart.university.domain.entity.GradeCohortDO;
import com.smart.university.domain.entity.MajorDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.SysUserDO;
import com.smart.university.domain.entity.TeacherDO;
import com.smart.university.domain.dto.req.LoginReqDTO;
import com.smart.university.domain.dto.resp.CurrentUserRespDTO;
import com.smart.university.domain.dto.resp.LoginRespDTO;
import com.smart.university.mapper.AcademicClassMapper;
import com.smart.university.mapper.GradeCohortMapper;
import com.smart.university.mapper.MajorMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.mapper.SysUserMapper;
import com.smart.university.mapper.TeacherMapper;
import com.smart.university.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 登录认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;

    private final StudentMapper studentMapper;

    private final TeacherMapper teacherMapper;

    private final MajorMapper majorMapper;

    private final GradeCohortMapper gradeCohortMapper;

    private final AcademicClassMapper academicClassMapper;

    private final JwtUtil jwtUtil;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginRespDTO login(LoginReqDTO requestParam) {
        SysUserDO sysUserDO = sysUserMapper.getUserByUsername(requestParam.getUsername());
        if (sysUserDO == null) {
            throw new BizException(ResultCodeEnum.USER_NOT_EXIST);
        }
        if (ObjectUtil.notEqual(sysUserDO.getRole(), requestParam.getLoginType())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "登录入口与用户角色不匹配");
        }
        if (ObjectUtil.equal(0, sysUserDO.getStatus())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "账号已停用");
        }
        if (!BCrypt.checkpw(requestParam.getPassword(), sysUserDO.getPasswordHash())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "账号或密码错误");
        }
        UserContext userContext = buildUserContext(sysUserDO);
        String token = jwtUtil.generateToken(sysUserDO.getId(), sysUserDO.getUsername(), sysUserDO.getRole());
        cacheLoginState(userContext, token);
        SysUserDO updateUserDO = new SysUserDO();
        updateUserDO.setId(sysUserDO.getId());
        updateUserDO.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateUser(updateUserDO);

        LoginRespDTO result = new LoginRespDTO();
        result.setToken(token);
        result.setUserId(sysUserDO.getId());
        result.setRole(sysUserDO.getRole().name());
        result.setUsername(sysUserDO.getUsername());
        result.setDisplayName(userContext.getDisplayName());
        return result;
    }

    @Override
    public CurrentUserRespDTO getCurrentUser() {
        UserContext userContext = UserContextHolder.getUserContext();
        if (userContext == null) {
            throw new BizException(ResultCodeEnum.NOT_LOGIN);
        }
        CurrentUserRespDTO result = new CurrentUserRespDTO();
        result.setUserId(userContext.getUserId());
        result.setRole(userContext.getRole() == null ? null : userContext.getRole().name());
        result.setName(userContext.getDisplayName());
        if (!userContext.isStudent()) {
            return result;
        }
        StudentDO studentDO = studentMapper.getStudentById(userContext.getStudentId());
        if (studentDO == null) {
            throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
        }
        result.setStudentId(studentDO.getId());
        result.setStudentNo(studentDO.getStudentNo());
        result.setName(studentDO.getStudentName());
        result.setMajorId(studentDO.getMajorId());
        result.setGradeId(studentDO.getGradeId());
        result.setClassId(studentDO.getClassId());
        MajorDO majorDO = majorMapper.getMajorById(studentDO.getMajorId());
        GradeCohortDO gradeCohortDO = gradeCohortMapper.getGradeCohortById(studentDO.getGradeId());
        AcademicClassDO academicClassDO = academicClassMapper.getAcademicClassById(studentDO.getClassId());
        result.setMajorName(majorDO == null ? null : majorDO.getMajorName());
        result.setGradeName(gradeCohortDO == null ? null : gradeCohortDO.getGradeName());
        result.setClassName(academicClassDO == null ? null : academicClassDO.getClassName());
        return result;
    }

    @Override
    public void logout() {
        UserContext userContext = UserContextHolder.getUserContext();
        if (userContext == null || userContext.getUserId() == null) {
            return;
        }
        stringRedisTemplate.delete(RedisKeyUtil.buildLoginTokenKey(userContext.getUserId()));
        stringRedisTemplate.delete(RedisKeyUtil.buildLoginUserKey(userContext.getUserId()));
    }

    /**
     * 根据系统用户构建登录上下文，补充学生或教师业务身份
     *
     * @param sysUserDO 系统用户信息
     * @return 登录上下文
     */
    private UserContext buildUserContext(SysUserDO sysUserDO) {
        UserContext.UserContextBuilder builder = UserContext.builder()
                .userId(sysUserDO.getId())
                .username(sysUserDO.getUsername())
                .role(sysUserDO.getRole())
                .displayName(sysUserDO.getUsername());
        if (RoleEnum.STUDENT == sysUserDO.getRole()) {
            StudentDO studentDO = studentMapper.getStudentByUserId(sysUserDO.getId());
            if (studentDO == null) {
                throw new BizException(ResultCodeEnum.STUDENT_NOT_EXIST);
            }
            builder.studentId(studentDO.getId()).displayName(studentDO.getStudentName());
        }
        if (RoleEnum.TEACHER == sysUserDO.getRole()) {
            TeacherDO teacherDO = teacherMapper.getTeacherByUserId(sysUserDO.getId());
            if (teacherDO == null) {
                throw new BizException(ResultCodeEnum.TEACHER_NOT_EXIST);
            }
            builder.teacherId(teacherDO.getId()).displayName(teacherDO.getTeacherName());
        }
        return builder.build();
    }

    /**
     * 缓存登录态，支持退出登录与单设备互踢
     *
     * @param userContext 登录上下文
     * @param token       访问令牌
     */
    private void cacheLoginState(UserContext userContext, String token) {
        Duration timeout = Duration.ofSeconds(RedisCommonConstant.LOGIN_TOKEN_TTL_SECONDS);
        stringRedisTemplate.opsForValue().set(RedisKeyUtil.buildLoginTokenKey(userContext.getUserId()), token, timeout);
        stringRedisTemplate.opsForValue()
                .set(RedisKeyUtil.buildLoginUserKey(userContext.getUserId()), JSONUtil.toJsonStr(userContext), timeout);
    }
}
