package com.smart.university.mapper;

import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.dto.req.StudentPageQueryReqDTO;

import java.util.List;

/**
 * 学生持久层
 */
public interface StudentMapper {

    /**
     * 根据 ID 查询学生
     *
     * @param studentId 学生 ID
     * @return 学生信息
     */
    StudentDO getStudentById(Long studentId);

    /**
     * 根据用户 ID 查询学生
     *
     * @param userId 用户 ID
     * @return 学生信息
     */
    StudentDO getStudentByUserId(Long userId);

    /**
     * 根据学号查询学生
     *
     * @param studentNo 学号
     * @return 学生信息
     */
    StudentDO getStudentByStudentNo(String studentNo);

    /**
     * 根据 ID 集合批量查询学生
     *
     * @param studentIds 学生 ID 集合
     * @return 学生信息集合
     */
    List<StudentDO> listStudentByIds(List<Long> studentIds);

    /**
     * 统计学号占用数量
     *
     * @param studentNo 学号
     * @return 数量
     */
    long countStudentByStudentNo(String studentNo);

    /**
     * 按条件统计学生数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    long countStudentByCondition(StudentPageQueryReqDTO requestParam);

    /**
     * 按条件分页查询学生
     *
     * @param requestParam 查询条件
     * @return 学生信息集合
     */
    List<StudentDO> listStudentByCondition(StudentPageQueryReqDTO requestParam);

    /**
     * 保存学生
     *
     * @param requestParam 学生数据对象
     * @return 影响行数
     */
    int saveStudent(StudentDO requestParam);

    /**
     * 更新学生
     *
     * @param requestParam 学生数据对象
     * @return 影响行数
     */
    int updateStudent(StudentDO requestParam);
}
