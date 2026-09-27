package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.dto.req.ClassroomPageQueryReqDTO;
import com.smart.university.domain.dto.req.ClassroomSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.ClassroomRespDTO;

/**
 * 教室服务
 */
public interface ClassroomService {

    /**
     * 分页查询教室
     *
     * @param requestParam 查询条件
     * @return 教室分页结果
     */
    PageResult<ClassroomRespDTO> pageClassroom(ClassroomPageQueryReqDTO requestParam);

    /**
     * 查询教室详情
     *
     * @param classroomId 教室 ID
     * @return 教室信息
     */
    ClassroomRespDTO getClassroomDetail(Long classroomId);

    /**
     * 保存教室
     *
     * @param requestParam 教室入参
     * @return 教室 ID
     */
    Long saveClassroom(ClassroomSaveReqDTO requestParam);

    /**
     * 修改教室状态
     *
     * @param classroomId  教室 ID
     * @param requestParam 状态入参
     */
    void updateClassroomStatus(Long classroomId, StatusUpdateReqDTO requestParam);
}
