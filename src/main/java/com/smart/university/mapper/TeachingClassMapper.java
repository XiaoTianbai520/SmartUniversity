package com.smart.university.mapper;

import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.dto.req.StudentTeachingClassPageQueryReqDTO;
import com.smart.university.domain.dto.req.TeachingClassPageQueryReqDTO;

import java.util.List;

/**
 * 教学班持久层
 */
public interface TeachingClassMapper {

    /**
     * 根据 ID 查询教学班
     *
     * @param teachingClassId 教学班 ID
     * @return 教学班信息
     */
    TeachingClassDO getTeachingClassById(Long teachingClassId);

    /**
     * 根据教学班编号查询教学班
     *
     * @param classCode 教学班编号
     * @return 教学班信息
     */
    TeachingClassDO getTeachingClassByClassCode(String classCode);

    /**
     * 根据 ID 集合批量查询教学班
     *
     * @param teachingClassIds 教学班 ID 集合
     * @return 教学班信息集合
     */
    List<TeachingClassDO> listTeachingClassByIds(List<Long> teachingClassIds);

    /**
     * 根据教师与学期查询教学班
     *
     * @param teachingClassDO 查询条件，包含 teacherId 与 semesterId
     * @return 教学班信息集合
     */
    List<TeachingClassDO> listTeachingClassByTeacher(TeachingClassDO teachingClassDO);

    /**
     * 按条件统计教学班数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countTeachingClassByCondition(TeachingClassPageQueryReqDTO requestParam);

    /**
     * 按条件分页查询教学班
     *
     * @param requestParam 查询条件
     * @return 教学班信息集合
     */
    List<TeachingClassDO> listTeachingClassByCondition(TeachingClassPageQueryReqDTO requestParam);

    /**
     * 统计当前学生有权限查看的教学班数量
     *
     * @param requestParam 查询条件，已由后端填充学生专业、年级、学期与批次
     * @return 数量
     */
    long countStudentVisibleTeachingClass(StudentTeachingClassPageQueryReqDTO requestParam);

    /**
     * 分页查询当前学生有权限查看的教学班
     *
     * @param requestParam 查询条件，已由后端填充学生专业、年级、学期与批次
     * @return 教学班信息集合
     */
    List<TeachingClassDO> listStudentVisibleTeachingClass(StudentTeachingClassPageQueryReqDTO requestParam);

    /**
     * 保存教学班
     *
     * @param requestParam 教学班数据对象
     * @return 影响行数
     */
    int saveTeachingClass(TeachingClassDO requestParam);

    /**
     * 更新教学班
     *
     * @param requestParam 教学班数据对象
     * @return 影响行数
     */
    int updateTeachingClass(TeachingClassDO requestParam);
}
