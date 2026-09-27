package com.smart.university.mapper;

import com.smart.university.domain.entity.TeacherDO;
import com.smart.university.domain.dto.req.TeacherPageQueryReqDTO;

import java.util.List;

/**
 * 教师持久层
 */
public interface TeacherMapper {

    /**
     * 根据 ID 查询教师
     *
     * @param teacherId 教师 ID
     * @return 教师信息
     */
    TeacherDO getTeacherById(Long teacherId);

    /**
     * 根据用户 ID 查询教师
     *
     * @param userId 用户 ID
     * @return 教师信息
     */
    TeacherDO getTeacherByUserId(Long userId);

    /**
     * 根据教师编号查询教师
     *
     * @param teacherNo 教师编号
     * @return 教师信息
     */
    TeacherDO getTeacherByTeacherNo(String teacherNo);

    /**
     * 根据 ID 集合批量查询教师
     *
     * @param teacherIds 教师 ID 集合
     * @return 教师信息集合
     */
    List<TeacherDO> listTeacherByIds(List<Long> teacherIds);

    /**
     * 统计教师编号占用数量
     *
     * @param teacherNo 教师编号
     * @return 数量
     */
    long countTeacherByTeacherNo(String teacherNo);

    /**
     * 按条件统计教师数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countTeacherByCondition(TeacherPageQueryReqDTO requestParam);

    /**
     * 按条件分页查询教师
     *
     * @param requestParam 查询条件
     * @return 教师信息集合
     */
    List<TeacherDO> listTeacherByCondition(TeacherPageQueryReqDTO requestParam);

    /**
     * 保存教师
     *
     * @param requestParam 教师数据对象
     * @return 影响行数
     */
    int saveTeacher(TeacherDO requestParam);

    /**
     * 更新教师
     *
     * @param requestParam 教师数据对象
     * @return 影响行数
     */
    int updateTeacher(TeacherDO requestParam);
}
