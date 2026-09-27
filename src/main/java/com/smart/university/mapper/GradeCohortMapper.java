package com.smart.university.mapper;

import com.smart.university.common.util.EnumParseUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.GradePageQueryReqDTO;
import com.smart.university.domain.entity.GradeCohortDO;

import java.util.List;

/**
 * 年级持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface GradeCohortMapper extends BaseMapper<GradeCohortDO> {

    /**
     * 根据 ID 查询年级
     *
     * @param gradeId 年级 ID
     * @return 年级信息
     */
    default GradeCohortDO getGradeCohortById(Long gradeId) {
        return selectById(gradeId);
    }

    /**
     * 根据 ID 集合批量查询年级
     *
     * @param gradeIds 年级 ID 集合
     * @return 年级信息集合
     */
    default List<GradeCohortDO> listGradeCohortByIds(List<Long> gradeIds) {
        return selectList(Wrappers.<GradeCohortDO>lambdaQuery().in(GradeCohortDO::getId, gradeIds));
    }

    /**
     * 按条件统计年级数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countGradeCohortByCondition(GradePageQueryReqDTO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 按条件分页查询年级
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    default IPage<GradeCohortDO> listGradeCohortByCondition(IPage<GradeCohortDO> page, GradePageQueryReqDTO requestParam) {
        return selectPage(page, buildQueryWrapper(requestParam));
    }

    /**
     * 保存年级
     *
     * @param requestParam 年级数据对象
     * @return 影响行数
     */
    default int saveGradeCohort(GradeCohortDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新年级
     *
     * @param requestParam 年级数据对象
     * @return 影响行数
     */
    default int updateGradeCohort(GradeCohortDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 构建年级查询条件
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<GradeCohortDO> buildQueryWrapper(GradePageQueryReqDTO requestParam) {
        String keyword = requestParam.getKeyword();
        LambdaQueryWrapper<GradeCohortDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.and(StrUtil.isNotBlank(keyword), each -> each
                .like(GradeCohortDO::getGradeName, keyword));
        queryWrapper.eq(requestParam.getStatus() != null, GradeCohortDO::getStatus, requestParam.getStatus());
        queryWrapper.orderByDesc(GradeCohortDO::getEntryYear);
        return queryWrapper;
    }
}
