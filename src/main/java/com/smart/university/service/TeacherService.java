package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.entity.TeacherDO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.req.TeacherPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeacherSaveReqDTO;
import com.smart.university.domain.dto.resp.TeacherRespDTO;

/**
 * 教师服务
 */
public interface TeacherService {

    /**
     * 分页查询教师
     *
     * @param requestParam 查询条件
     * @return 教师分页结果
     */
    PageResult<TeacherRespDTO> pageTeacher(TeacherPageQueryReqDTO requestParam);

    /**
     * 查询教师详情
     *
     * @param teacherId 教师 ID
     * @return 教师信息
     */
    TeacherRespDTO getTeacherDetail(Long teacherId);

    /**
     * 新增教师，同时创建登录账号
     *
     * @param requestParam 教师入参
     * @return 教师 ID
     */
    Long saveTeacher(TeacherSaveReqDTO requestParam);

    /**
     * 修改教师
     *
     * @param requestParam 教师入参
     */
    void updateTeacher(TeacherSaveReqDTO requestParam);

    /**
     * 修改教师状态
     *
     * @param teacherId    教师 ID
     * @param requestParam 状态入参
     */
    void updateTeacherStatus(Long teacherId, StatusUpdateReqDTO requestParam);

    /**
     * 根据 ID 查询教师
     *
     * @param teacherId 教师 ID
     * @return 教师数据对象
     */
    TeacherDO getTeacherById(Long teacherId);
}
