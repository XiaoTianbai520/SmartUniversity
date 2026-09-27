package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.dto.req.StudentTeachingClassPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeachingClassPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeachingClassSaveReqDTO;
import com.smart.university.domain.dto.req.TeacherStudentPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeachingClassStatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.StudentTeachingClassDetailRespDTO;
import com.smart.university.domain.dto.resp.StudentTeachingClassRespDTO;
import com.smart.university.domain.dto.resp.TeacherClassStudentRespDTO;
import com.smart.university.domain.dto.resp.TeacherTeachingClassRespDTO;
import com.smart.university.domain.dto.resp.TeachingClassRespDTO;

import java.util.List;

/**
 * 教学班服务
 */
public interface TeachingClassService {

    /**
     * 分页查询教学班
     *
     * @param requestParam 查询条件
     * @return 教学班分页结果
     */
    PageResult<TeachingClassRespDTO> pageTeachingClass(TeachingClassPageQueryReqDTO requestParam);

    /**
     * 查询教学班详情
     *
     * @param teachingClassId 教学班 ID
     * @return 教学班信息
     */
    TeachingClassRespDTO getTeachingClassDetail(Long teachingClassId);

    /**
     * 保存教学班，同步维护开放专业与开放年级
     *
     * @param requestParam 教学班入参
     * @return 教学班 ID
     */
    Long saveTeachingClass(TeachingClassSaveReqDTO requestParam);

    /**
     * 修改教学班状态
     *
     * @param teachingClassId 教学班 ID
     * @param requestParam    状态入参
     */
    void updateTeachingClassStatus(Long teachingClassId, TeachingClassStatusUpdateReqDTO requestParam);

    /**
     * 分页查询当前学生可选课程，专业、年级、学期、批次均由后端推导
     *
     * @param requestParam 查询条件
     * @return 可选课程分页结果
     */
    PageResult<StudentTeachingClassRespDTO> pageStudentVisibleTeachingClass(
            StudentTeachingClassPageQueryReqDTO requestParam);

    /**
     * 查询学生视角的教学班详情，并再次校验查看权限
     *
     * @param studentId       学生 ID
     * @param teachingClassId 教学班 ID
     * @return 课程详情
     */
    StudentTeachingClassDetailRespDTO getStudentTeachingClassDetail(Long studentId, Long teachingClassId);

    /**
     * 根据 ID 查询教学班
     *
     * @param teachingClassId 教学班 ID
     * @return 教学班数据对象
     */
    TeachingClassDO getTeachingClassById(Long teachingClassId);

    /**
     * 查询当前登录教师的教学班
     *
     * @param semesterId 学期 ID，为空时使用当前学期
     * @return 教学班集合
     */
    List<TeacherTeachingClassRespDTO> listTeacherTeachingClass(Long semesterId);

    /**
     * 分页查询教学班学生名单，只统计已选状态的学生
     *
     * @param teachingClassId 教学班 ID
     * @param requestParam    查询条件
     * @return 学生名单分页结果
     */
    PageResult<TeacherClassStudentRespDTO> pageTeacherClassStudent(Long teachingClassId,
                                                                   TeacherStudentPageQueryReqDTO requestParam);
}
