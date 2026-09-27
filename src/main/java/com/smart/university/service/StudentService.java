package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.req.StudentPageQueryReqDTO;
import com.smart.university.domain.dto.req.StudentSaveReqDTO;
import com.smart.university.domain.dto.resp.StudentRespDTO;

/**
 * 学生服务
 */
public interface StudentService {

    /**
     * 分页查询学生
     *
     * @param requestParam 查询条件
     * @return 学生分页结果
     */
    PageResult<StudentRespDTO> pageStudent(StudentPageQueryReqDTO requestParam);

    /**
     * 查询学生详情
     *
     * @param studentId 学生 ID
     * @return 学生信息
     */
    StudentRespDTO getStudentDetail(Long studentId);

    /**
     * 新增学生，同时创建登录账号
     *
     * @param requestParam 学生入参
     * @return 学生 ID
     */
    Long saveStudent(StudentSaveReqDTO requestParam);

    /**
     * 修改学生
     *
     * @param requestParam 学生入参
     */
    void updateStudent(StudentSaveReqDTO requestParam);

    /**
     * 修改学生状态
     *
     * @param studentId    学生 ID
     * @param requestParam 状态入参
     */
    void updateStudentStatus(Long studentId, StatusUpdateReqDTO requestParam);

    /**
     * 根据用户 ID 查询学生，供登录与学生端接口复用
     *
     * @param userId 用户 ID
     * @return 学生数据对象
     */
    StudentDO getStudentByUserId(Long userId);

    /**
     * 根据 ID 查询学生
     *
     * @param studentId 学生 ID
     * @return 学生数据对象
     */
    StudentDO getStudentById(Long studentId);
}
