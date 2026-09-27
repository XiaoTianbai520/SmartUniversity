package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.dto.req.CoursePageQueryReqDTO;
import com.smart.university.domain.dto.req.CourseSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.CourseRespDTO;

import java.util.List;

/**
 * 课程服务
 */
public interface CourseService {

    /**
     * 分页查询课程
     *
     * @param requestParam 查询条件
     * @return 课程分页结果
     */
    PageResult<CourseRespDTO> pageCourse(CoursePageQueryReqDTO requestParam);

    /**
     * 查询课程详情
     *
     * @param courseId 课程 ID
     * @return 课程信息
     */
    CourseRespDTO getCourseDetail(Long courseId);

    /**
     * 保存课程
     *
     * @param requestParam 课程入参
     * @return 课程 ID
     */
    Long saveCourse(CourseSaveReqDTO requestParam);

    /**
     * 修改课程状态
     *
     * @param courseId     课程 ID
     * @param requestParam 状态入参
     */
    void updateCourseStatus(Long courseId, StatusUpdateReqDTO requestParam);

    /**
     * 根据 ID 查询课程
     *
     * @param courseId 课程 ID
     * @return 课程数据对象
     */
    CourseDO getCourseById(Long courseId);

    /**
     * 根据 ID 集合批量查询课程
     *
     * @param courseIds 课程 ID 集合
     * @return 课程数据对象集合
     */
    List<CourseDO> listCourseByIds(List<Long> courseIds);
}
