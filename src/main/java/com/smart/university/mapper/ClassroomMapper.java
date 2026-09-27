package com.smart.university.mapper;

import com.smart.university.domain.entity.ClassroomDO;
import com.smart.university.domain.dto.req.ClassroomPageQueryReqDTO;

import java.util.List;

/**
 * 教室持久层
 */
public interface ClassroomMapper {

    /**
     * 根据 ID 查询教室
     *
     * @param classroomId 教室 ID
     * @return 教室信息
     */
    ClassroomDO getClassroomById(Long classroomId);

    /**
     * 根据 ID 集合批量查询教室
     *
     * @param classroomIds 教室 ID 集合
     * @return 教室信息集合
     */
    List<ClassroomDO> listClassroomByIds(List<Long> classroomIds);

    /**
     * 按条件统计教室数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countClassroomByCondition(ClassroomPageQueryReqDTO requestParam);

    /**
     * 按条件分页查询教室
     *
     * @param requestParam 查询条件
     * @return 教室信息集合
     */
    List<ClassroomDO> listClassroomByCondition(ClassroomPageQueryReqDTO requestParam);

    /**
     * 保存教室
     *
     * @param requestParam 教室数据对象
     * @return 影响行数
     */
    int saveClassroom(ClassroomDO requestParam);

    /**
     * 更新教室
     *
     * @param requestParam 教室数据对象
     * @return 影响行数
     */
    int updateClassroom(ClassroomDO requestParam);
}
