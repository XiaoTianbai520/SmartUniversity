package com.smart.university.mapper;

import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;

import java.util.List;

/**
 * 选课记录持久层
 */
public interface CourseSelectionMapper {

    /**
     * 根据 ID 查询选课记录
     *
     * @param selectionId 选课记录 ID
     * @return 选课记录信息
     */
    CourseSelectionDO getSelectionById(Long selectionId);

    /**
     * 查询指定学生在指定教学班下的选课记录，包含已退课记录
     *
     * @param requestParam 查询条件，包含 studentId 与 teachingClassId
     * @return 选课记录信息
     */
    CourseSelectionDO getSelectionByStudentAndClass(CourseSelectionDO requestParam);

    /**
     * 按条件统计选课记录数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countSelectionByCondition(CourseSelectionDO requestParam);

    /**
     * 按条件查询选课记录
     *
     * @param requestParam 查询条件
     * @return 选课记录信息集合
     */
    List<CourseSelectionDO> listSelectionByCondition(CourseSelectionDO requestParam);

    /**
     * 查询指定学生在指定学期下的选课记录
     *
     * @param requestParam 查询条件，已由后端填充 studentId 与 semesterId
     * @return 选课记录信息集合
     */
    List<CourseSelectionDO> listSelectionByStudent(StudentSelectionQueryReqDTO requestParam);

    /**
     * 统计教学班当前已选人数
     *
     * @param teachingClassId 教学班 ID
     * @return 已选人数
     */
    long countSelectedByTeachingClassId(Long teachingClassId);

    /**
     * 统计学生在指定学期已选课程数量
     *
     * @param requestParam 查询条件，包含 studentId、semesterId 与 status
     * @return 已选数量
     */
    long countSelectedByStudent(StudentSelectionQueryReqDTO requestParam);

    /**
     * 保存选课记录
     *
     * @param requestParam 选课记录数据对象
     * @return 影响行数
     */
    int saveSelection(CourseSelectionDO requestParam);

    /**
     * 更新选课记录
     *
     * @param requestParam 选课记录数据对象
     * @return 影响行数
     */
    int updateSelection(CourseSelectionDO requestParam);
}
