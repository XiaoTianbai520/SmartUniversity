package com.smart.university.mapper;

import com.smart.university.common.util.EnumParseUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.AcademicClassPageQueryReqDTO;
import com.smart.university.domain.entity.AcademicClassDO;

import java.util.List;

/**
 * 行政班级持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface AcademicClassMapper extends BaseMapper<AcademicClassDO> {

    /**
     * 根据 ID 查询行政班级
     *
     * @param classId 行政班级 ID
     * @return 行政班级信息
     */
    default AcademicClassDO getAcademicClassById(Long classId) {
        return selectById(classId);
    }

    /**
     * 根据 ID 集合批量查询行政班级
     *
     * @param classIds 行政班级 ID 集合
     * @return 行政班级信息集合
     */
    default List<AcademicClassDO> listAcademicClassByIds(List<Long> classIds) {
        return selectList(Wrappers.<AcademicClassDO>lambdaQuery().in(AcademicClassDO::getId, classIds));
    }

    /**
     * 按条件统计行政班级数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countAcademicClassByCondition(AcademicClassPageQueryReqDTO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 按条件分页查询行政班级
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    default IPage<AcademicClassDO> listAcademicClassByCondition(IPage<AcademicClassDO> page, AcademicClassPageQueryReqDTO requestParam) {
        return selectPage(page, buildQueryWrapper(requestParam));
    }

    /**
     * 保存行政班级
     *
     * @param requestParam 行政班级数据对象
     * @return 影响行数
     */
    default int saveAcademicClass(AcademicClassDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新行政班级
     *
     * @param requestParam 行政班级数据对象
     * @return 影响行数
     */
    default int updateAcademicClass(AcademicClassDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 构建行政班级查询条件
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<AcademicClassDO> buildQueryWrapper(AcademicClassPageQueryReqDTO requestParam) {
        String keyword = requestParam.getKeyword();
        LambdaQueryWrapper<AcademicClassDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.and(StrUtil.isNotBlank(keyword), each -> each
                .like(AcademicClassDO::getClassCode, keyword)
                        .or().like(AcademicClassDO::getClassName, keyword));
        queryWrapper.eq(requestParam.getMajorId() != null, AcademicClassDO::getMajorId, requestParam.getMajorId());
        queryWrapper.eq(requestParam.getGradeId() != null, AcademicClassDO::getGradeId, requestParam.getGradeId());
        queryWrapper.eq(requestParam.getStatus() != null, AcademicClassDO::getStatus, requestParam.getStatus());
        queryWrapper.orderByDesc(AcademicClassDO::getId);
        return queryWrapper;
    }
}
