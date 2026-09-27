package com.smart.university.mapper;

import com.smart.university.common.util.EnumParseUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.TeacherPageQueryReqDTO;
import com.smart.university.domain.entity.TeacherDO;

import java.util.List;

/**
 * 教师持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface TeacherMapper extends BaseMapper<TeacherDO> {

    /**
     * 根据 ID 查询教师
     *
     * @param teacherId 教师 ID
     * @return 教师信息
     */
    default TeacherDO getTeacherById(Long teacherId) {
        return selectById(teacherId);
    }

    /**
     * 根据 ID 集合批量查询教师
     *
     * @param teacherIds 教师 ID 集合
     * @return 教师信息集合
     */
    default List<TeacherDO> listTeacherByIds(List<Long> teacherIds) {
        return selectList(Wrappers.<TeacherDO>lambdaQuery().in(TeacherDO::getId, teacherIds));
    }

    /**
     * 根据用户 ID 查询教师
     *
     * @param userId 用户 ID
     * @return 教师信息
     */
    default TeacherDO getTeacherByUserId(Long userId) {
        return selectOne(Wrappers.<TeacherDO>lambdaQuery()
                .eq(TeacherDO::getUserId, userId)
                .last("LIMIT 1"));
    }

    /**
     * 根据教师编号查询教师
     *
     * @param teacherNo 教师编号
     * @return 教师信息
     */
    default TeacherDO getTeacherByTeacherNo(String teacherNo) {
        return selectOne(Wrappers.<TeacherDO>lambdaQuery()
                .eq(TeacherDO::getTeacherNo, teacherNo)
                .last("LIMIT 1"));
    }

    /**
     * 统计指定教师编号的数量，用于编号查重
     *
     * @param teacherNo 教师编号
     * @return 数量
     */
    default long countTeacherByTeacherNo(String teacherNo) {
        return selectCount(Wrappers.<TeacherDO>lambdaQuery().eq(TeacherDO::getTeacherNo, teacherNo));
    }

    /**
     * 按条件统计教师数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countTeacherByCondition(TeacherPageQueryReqDTO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 按条件分页查询教师
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    default IPage<TeacherDO> listTeacherByCondition(IPage<TeacherDO> page, TeacherPageQueryReqDTO requestParam) {
        return selectPage(page, buildQueryWrapper(requestParam));
    }

    /**
     * 保存教师
     *
     * @param requestParam 教师数据对象
     * @return 影响行数
     */
    default int saveTeacher(TeacherDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新教师
     *
     * @param requestParam 教师数据对象
     * @return 影响行数
     */
    default int updateTeacher(TeacherDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 构建教师查询条件
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<TeacherDO> buildQueryWrapper(TeacherPageQueryReqDTO requestParam) {
        String keyword = requestParam.getKeyword();
        LambdaQueryWrapper<TeacherDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.and(StrUtil.isNotBlank(keyword), each -> each
                .like(TeacherDO::getTeacherNo, keyword)
                        .or().like(TeacherDO::getTeacherName, keyword));
        queryWrapper.eq(requestParam.getStatus() != null, TeacherDO::getStatus, requestParam.getStatus());
        queryWrapper.orderByDesc(TeacherDO::getId);
        return queryWrapper;
    }
}
