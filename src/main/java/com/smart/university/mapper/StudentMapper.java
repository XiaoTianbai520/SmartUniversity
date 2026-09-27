package com.smart.university.mapper;

import com.smart.university.common.util.EnumParseUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.StudentPageQueryReqDTO;
import com.smart.university.domain.entity.StudentDO;

import java.util.List;

/**
 * 学生持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface StudentMapper extends BaseMapper<StudentDO> {

    /**
     * 根据 ID 查询学生
     *
     * @param studentId 学生 ID
     * @return 学生信息
     */
    default StudentDO getStudentById(Long studentId) {
        return selectById(studentId);
    }

    /**
     * 根据 ID 集合批量查询学生
     *
     * @param studentIds 学生 ID 集合
     * @return 学生信息集合
     */
    default List<StudentDO> listStudentByIds(List<Long> studentIds) {
        return selectList(Wrappers.<StudentDO>lambdaQuery().in(StudentDO::getId, studentIds));
    }

    /**
     * 根据用户 ID 查询学生
     *
     * @param userId 用户 ID
     * @return 学生信息
     */
    default StudentDO getStudentByUserId(Long userId) {
        return selectOne(Wrappers.<StudentDO>lambdaQuery()
                .eq(StudentDO::getUserId, userId)
                .last("LIMIT 1"));
    }

    /**
     * 根据学号查询学生
     *
     * @param studentNo 学号
     * @return 学生信息
     */
    default StudentDO getStudentByStudentNo(String studentNo) {
        return selectOne(Wrappers.<StudentDO>lambdaQuery()
                .eq(StudentDO::getStudentNo, studentNo)
                .last("LIMIT 1"));
    }

    /**
     * 统计指定学号的数量，用于学号查重
     *
     * @param studentNo 学号
     * @return 数量
     */
    default long countStudentByStudentNo(String studentNo) {
        return selectCount(Wrappers.<StudentDO>lambdaQuery().eq(StudentDO::getStudentNo, studentNo));
    }

    /**
     * 按条件统计学生数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countStudentByCondition(StudentPageQueryReqDTO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 按条件分页查询学生
     *
     * @param page         分页参数
     * @param requestParam 查询条件
     * @return 分页结果
     */
    default IPage<StudentDO> listStudentByCondition(IPage<StudentDO> page, StudentPageQueryReqDTO requestParam) {
        return selectPage(page, buildQueryWrapper(requestParam));
    }

    /**
     * 保存学生
     *
     * @param requestParam 学生数据对象
     * @return 影响行数
     */
    default int saveStudent(StudentDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新学生
     *
     * @param requestParam 学生数据对象
     * @return 影响行数
     */
    default int updateStudent(StudentDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 构建学生查询条件
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<StudentDO> buildQueryWrapper(StudentPageQueryReqDTO requestParam) {
        String keyword = requestParam.getKeyword();
        LambdaQueryWrapper<StudentDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.and(StrUtil.isNotBlank(keyword), each -> each
                .like(StudentDO::getStudentNo, keyword)
                        .or().like(StudentDO::getStudentName, keyword));
        queryWrapper.eq(requestParam.getMajorId() != null, StudentDO::getMajorId, requestParam.getMajorId());
        queryWrapper.eq(requestParam.getGradeId() != null, StudentDO::getGradeId, requestParam.getGradeId());
        queryWrapper.eq(requestParam.getClassId() != null, StudentDO::getClassId, requestParam.getClassId());
        queryWrapper.eq(requestParam.getStatus() != null, StudentDO::getStatus, requestParam.getStatus());
        queryWrapper.orderByDesc(StudentDO::getId);
        return queryWrapper;
    }
}
