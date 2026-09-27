package com.smart.university.mapper;

import com.smart.university.domain.entity.AcademicClassDO;
import com.smart.university.domain.dto.req.AcademicClassPageQueryReqDTO;

import java.util.List;

/**
 * 行政班级持久层
 */
public interface AcademicClassMapper {

    /**
     * 根据 ID 查询行政班级
     *
     * @param classId 班级 ID
     * @return 行政班级信息
     */
    AcademicClassDO getAcademicClassById(Long classId);

    /**
     * 根据 ID 集合批量查询行政班级
     *
     * @param classIds 班级 ID 集合
     * @return 行政班级信息集合
     */
    List<AcademicClassDO> listAcademicClassByIds(List<Long> classIds);

    /**
     * 按条件统计行政班级数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countAcademicClassByCondition(AcademicClassPageQueryReqDTO requestParam);

    /**
     * 按条件分页查询行政班级
     *
     * @param requestParam 查询条件
     * @return 行政班级信息集合
     */
    List<AcademicClassDO> listAcademicClassByCondition(AcademicClassPageQueryReqDTO requestParam);

    /**
     * 保存行政班级
     *
     * @param requestParam 行政班级数据对象
     * @return 影响行数
     */
    int saveAcademicClass(AcademicClassDO requestParam);

    /**
     * 更新行政班级
     *
     * @param requestParam 行政班级数据对象
     * @return 影响行数
     */
    int updateAcademicClass(AcademicClassDO requestParam);
}
