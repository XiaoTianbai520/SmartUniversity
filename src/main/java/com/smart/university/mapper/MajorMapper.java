package com.smart.university.mapper;

import com.smart.university.common.util.EnumParseUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.MajorPageQueryReqDTO;
import com.smart.university.domain.entity.MajorDO;

import java.util.List;

/**
 * 专业持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface MajorMapper extends BaseMapper<MajorDO> {

    /**
     * 根据 ID 查询专业
     *
     * @param majorId 专业 ID
     * @return 专业信息
     */
    default MajorDO getMajorById(Long majorId) {
        return selectById(majorId);
    }

    /**
     * 根据 ID 集合批量查询专业
     *
     * @param majorIds 专业 ID 集合
     * @return 专业信息集合
     */
    default List<MajorDO> listMajorByIds(List<Long> majorIds) {
        return selectList(Wrappers.<MajorDO>lambdaQuery().in(MajorDO::getId, majorIds));
    }

    /**
     * 根据专业编号查询专业
     *
     * @param majorCode 专业编号
     * @return 专业信息
     */
    default MajorDO getMajorByMajorCode(String majorCode) {
        return selectOne(Wrappers.<MajorDO>lambdaQuery()
                .eq(MajorDO::getMajorCode, majorCode)
                .last("LIMIT 1"));
    }

    /**
     * 按条件统计专业数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countMajorByCondition(MajorPageQueryReqDTO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 按条件分页查询专业
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    default IPage<MajorDO> listMajorByCondition(IPage<MajorDO> page, MajorPageQueryReqDTO requestParam) {
        return selectPage(page, buildQueryWrapper(requestParam));
    }

    /**
     * 保存专业
     *
     * @param requestParam 专业数据对象
     * @return 影响行数
     */
    default int saveMajor(MajorDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新专业
     *
     * @param requestParam 专业数据对象
     * @return 影响行数
     */
    default int updateMajor(MajorDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 构建专业查询条件
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<MajorDO> buildQueryWrapper(MajorPageQueryReqDTO requestParam) {
        String keyword = requestParam.getKeyword();
        LambdaQueryWrapper<MajorDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.and(StrUtil.isNotBlank(keyword), each -> each
                .like(MajorDO::getMajorCode, keyword)
                        .or().like(MajorDO::getMajorName, keyword));
        queryWrapper.eq(requestParam.getStatus() != null, MajorDO::getStatus, requestParam.getStatus());
        queryWrapper.orderByDesc(MajorDO::getId);
        return queryWrapper;
    }
}
