package com.smart.university.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.TrainingPlanPageQueryReqDTO;
import com.smart.university.domain.entity.TrainingPlanDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 培养方案持久层，对外只暴露 default 方法，
 * 方法名遵循 get / list / count / save / update / remove 前缀规范
 */
public interface TrainingPlanMapper extends BaseMapper<TrainingPlanDO> {

    /**
     * 根据 ID 查询培养方案
     *
     * @param planId 培养方案 ID
     * @return 培养方案信息
     */
    default TrainingPlanDO getTrainingPlanById(Long planId) {
        return selectById(planId);
    }

    /**
     * 根据专业与入学年级查询培养方案，唯一键保证至多一条
     *
     * @param majorId 专业 ID
     * @param gradeId 年级 ID
     * @return 培养方案信息，无对应方案时返回 null
     */
    default TrainingPlanDO getTrainingPlanByMajorAndGrade(Long majorId, Long gradeId) {
        return selectOne(Wrappers.<TrainingPlanDO>lambdaQuery()
                .eq(TrainingPlanDO::getMajorId, majorId)
                .eq(TrainingPlanDO::getGradeId, gradeId)
                .last("LIMIT 1"));
    }

    /**
     * 按条件统计培养方案数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countTrainingPlanByCondition(@Param("requestParam") TrainingPlanPageQueryReqDTO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 按条件分页查询培养方案
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    default IPage<TrainingPlanDO> listTrainingPlanByCondition(IPage<TrainingPlanDO> page,
                                                              @Param("requestParam") TrainingPlanPageQueryReqDTO requestParam) {
        return selectPage(page, buildQueryWrapper(requestParam));
    }

    /**
     * 按条件查询培养方案集合，不分页
     *
     * @param requestParam 查询条件
     * @return 培养方案集合
     */
    default List<TrainingPlanDO> listTrainingPlanByCondition(TrainingPlanPageQueryReqDTO requestParam) {
        return selectList(buildQueryWrapper(requestParam));
    }

    /**
     * 保存培养方案
     *
     * @param requestParam 培养方案数据对象
     * @return 影响行数
     */
    default int saveTrainingPlan(TrainingPlanDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新培养方案
     *
     * @param requestParam 培养方案数据对象
     * @return 影响行数
     */
    default int updateTrainingPlan(TrainingPlanDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 删除培养方案
     *
     * @param planId 培养方案 ID
     * @return 影响行数
     */
    default int removeTrainingPlan(Long planId) {
        return deleteById(planId);
    }

    /**
     * 构建培养方案查询条件，关键字按方案名称模糊匹配
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<TrainingPlanDO> buildQueryWrapper(TrainingPlanPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<TrainingPlanDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(requestParam.getMajorId() != null, TrainingPlanDO::getMajorId, requestParam.getMajorId());
        queryWrapper.eq(requestParam.getGradeId() != null, TrainingPlanDO::getGradeId, requestParam.getGradeId());
        queryWrapper.eq(requestParam.getStatus() != null, TrainingPlanDO::getStatus, requestParam.getStatus());
        queryWrapper.like(requestParam.getKeyword() != null && !requestParam.getKeyword().isEmpty(),
                TrainingPlanDO::getPlanName, requestParam.getKeyword());
        queryWrapper.orderByDesc(TrainingPlanDO::getId);
        return queryWrapper;
    }
}
