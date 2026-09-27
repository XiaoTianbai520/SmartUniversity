package com.smart.university.mapper;

import com.smart.university.domain.entity.MajorDO;
import com.smart.university.domain.dto.req.MajorPageQueryReqDTO;

import java.util.List;

/**
 * 专业持久层
 */
public interface MajorMapper {

    /**
     * 根据 ID 查询专业
     *
     * @param majorId 专业 ID
     * @return 专业信息
     */
    MajorDO getMajorById(Long majorId);

    /**
     * 根据 ID 集合批量查询专业
     *
     * @param majorIds 专业 ID 集合
     * @return 专业信息集合
     */
    List<MajorDO> listMajorByIds(List<Long> majorIds);

    /**
     * 根据专业编号查询专业
     *
     * @param majorCode 专业编号
     * @return 专业信息
     */
    MajorDO getMajorByMajorCode(String majorCode);

    /**
     * 按条件统计专业数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countMajorByCondition(MajorPageQueryReqDTO requestParam);

    /**
     * 按条件分页查询专业
     *
     * @param requestParam 查询条件
     * @return 专业信息集合
     */
    List<MajorDO> listMajorByCondition(MajorPageQueryReqDTO requestParam);

    /**
     * 保存专业
     *
     * @param requestParam 专业数据对象
     * @return 影响行数
     */
    int saveMajor(MajorDO requestParam);

    /**
     * 更新专业
     *
     * @param requestParam 专业数据对象
     * @return 影响行数
     */
    int updateMajor(MajorDO requestParam);
}
