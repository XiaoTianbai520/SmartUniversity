package com.smart.university.mapper;

import com.smart.university.common.util.EnumParseUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.CoursePageQueryReqDTO;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.enums.CourseTypeEnum;

import java.util.List;

/**
 * 课程持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface CourseMapper extends BaseMapper<CourseDO> {

    /**
     * 根据 ID 查询课程
     *
     * @param courseId 课程 ID
     * @return 课程信息
     */
    default CourseDO getCourseById(Long courseId) {
        return selectById(courseId);
    }

    /**
     * 根据 ID 集合批量查询课程
     *
     * @param courseIds 课程 ID 集合
     * @return 课程信息集合
     */
    default List<CourseDO> listCourseByIds(List<Long> courseIds) {
        return selectList(Wrappers.<CourseDO>lambdaQuery().in(CourseDO::getId, courseIds));
    }

    /**
     * 统计指定课程编号的数量，用于编号查重
     *
     * @param courseCode 课程编号
     * @return 数量
     */
    default long countCourseByCourseCode(String courseCode) {
        return selectCount(Wrappers.<CourseDO>lambdaQuery().eq(CourseDO::getCourseCode, courseCode));
    }

    /**
     * 按条件统计课程数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countCourseByCondition(CoursePageQueryReqDTO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 按条件分页查询课程
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    default IPage<CourseDO> listCourseByCondition(IPage<CourseDO> page, CoursePageQueryReqDTO requestParam) {
        return selectPage(page, buildQueryWrapper(requestParam));
    }

    /**
     * 保存课程
     *
     * @param requestParam 课程数据对象
     * @return 影响行数
     */
    default int saveCourse(CourseDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新课程
     *
     * @param requestParam 课程数据对象
     * @return 影响行数
     */
    default int updateCourse(CourseDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 构建课程查询条件
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<CourseDO> buildQueryWrapper(CoursePageQueryReqDTO requestParam) {
        String keyword = requestParam.getKeyword();
        LambdaQueryWrapper<CourseDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.and(StrUtil.isNotBlank(keyword), each -> each
                .like(CourseDO::getCourseCode, keyword)
                        .or().like(CourseDO::getCourseName, keyword));
        CourseTypeEnum courseType = EnumParseUtil.parseOrNull(CourseTypeEnum.class, requestParam.getCourseType());
        queryWrapper.eq(courseType != null, CourseDO::getCourseType, courseType);
        queryWrapper.eq(requestParam.getStatus() != null, CourseDO::getStatus, requestParam.getStatus());
        queryWrapper.orderByDesc(CourseDO::getId);
        return queryWrapper;
    }
}
