package com.smart.university.mapper;

import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.dto.req.CoursePageQueryReqDTO;

import java.util.List;

/**
 * 课程持久层
 */
public interface CourseMapper {

    /**
     * 根据 ID 查询课程
     *
     * @param courseId 课程 ID
     * @return 课程信息
     */
    CourseDO getCourseById(Long courseId);

    /**
     * 根据 ID 集合批量查询课程
     *
     * @param courseIds 课程 ID 集合
     * @return 课程信息集合
     */
    List<CourseDO> listCourseByIds(List<Long> courseIds);

    /**
     * 统计课程编号占用数量
     *
     * @param courseCode 课程编号
     * @return 数量
     */
    long countCourseByCourseCode(String courseCode);

    /**
     * 按条件统计课程数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countCourseByCondition(CoursePageQueryReqDTO requestParam);

    /**
     * 按条件分页查询课程
     *
     * @param requestParam 查询条件
     * @return 课程信息集合
     */
    List<CourseDO> listCourseByCondition(CoursePageQueryReqDTO requestParam);

    /**
     * 保存课程
     *
     * @param requestParam 课程数据对象
     * @return 影响行数
     */
    int saveCourse(CourseDO requestParam);

    /**
     * 更新课程
     *
     * @param requestParam 课程数据对象
     * @return 影响行数
     */
    int updateCourse(CourseDO requestParam);
}
