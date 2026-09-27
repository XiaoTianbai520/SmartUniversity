package com.smart.university.mapper;

import com.smart.university.common.util.EnumParseUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.ClassroomPageQueryReqDTO;
import com.smart.university.domain.entity.ClassroomDO;

import java.util.List;

/**
 * 教室持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface ClassroomMapper extends BaseMapper<ClassroomDO> {

    /**
     * 根据 ID 查询教室
     *
     * @param classroomId 教室 ID
     * @return 教室信息
     */
    default ClassroomDO getClassroomById(Long classroomId) {
        return selectById(classroomId);
    }

    /**
     * 根据 ID 集合批量查询教室
     *
     * @param classroomIds 教室 ID 集合
     * @return 教室信息集合
     */
    default List<ClassroomDO> listClassroomByIds(List<Long> classroomIds) {
        return selectList(Wrappers.<ClassroomDO>lambdaQuery().in(ClassroomDO::getId, classroomIds));
    }

    /**
     * 按条件统计教室数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countClassroomByCondition(ClassroomPageQueryReqDTO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 按条件分页查询教室
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    default IPage<ClassroomDO> listClassroomByCondition(IPage<ClassroomDO> page, ClassroomPageQueryReqDTO requestParam) {
        return selectPage(page, buildQueryWrapper(requestParam));
    }

    /**
     * 保存教室
     *
     * @param requestParam 教室数据对象
     * @return 影响行数
     */
    default int saveClassroom(ClassroomDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新教室
     *
     * @param requestParam 教室数据对象
     * @return 影响行数
     */
    default int updateClassroom(ClassroomDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 构建教室查询条件
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<ClassroomDO> buildQueryWrapper(ClassroomPageQueryReqDTO requestParam) {
        String keyword = requestParam.getKeyword();
        LambdaQueryWrapper<ClassroomDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.and(StrUtil.isNotBlank(keyword), each -> each
                .like(ClassroomDO::getBuildingName, keyword)
                        .or().like(ClassroomDO::getRoomNo, keyword));
        queryWrapper.eq(requestParam.getStatus() != null, ClassroomDO::getStatus, requestParam.getStatus());
        queryWrapper.orderByDesc(ClassroomDO::getId);
        return queryWrapper;
    }
}
