package com.smart.university.mapper;

import com.smart.university.common.util.EnumParseUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.enums.SelectionStatusEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 选课记录持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface CourseSelectionMapper extends BaseMapper<CourseSelectionDO> {

    /**
     * 根据 ID 查询选课记录
     *
     * @param selectionId 选课记录 ID
     * @return 选课记录信息
     */
    default CourseSelectionDO getSelectionById(Long selectionId) {
        return selectById(selectionId);
    }

    /**
     * 根据学生与教学班查询选课记录，用于重复选课校验
     *
     * @param requestParam 选课记录数据对象
     * @return 选课记录信息
     */
    default CourseSelectionDO getSelectionByStudentAndClass(CourseSelectionDO requestParam) {
        return selectOne(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getStudentId, requestParam.getStudentId())
                .eq(CourseSelectionDO::getTeachingClassId, requestParam.getTeachingClassId())
                .last("LIMIT 1"));
    }

    /**
     * 按条件统计选课记录数量
     *
     * @param requestParam 查询条件
     * @return 数量
     */
    default long countSelectionByCondition(CourseSelectionDO requestParam) {
        return selectCount(buildQueryWrapper(requestParam));
    }

    /**
     * 按条件查询选课记录
     *
     * @param requestParam 查询条件
     * @return 选课记录集合
     */
    default List<CourseSelectionDO> listSelectionByCondition(CourseSelectionDO requestParam) {
        return selectList(buildQueryWrapper(requestParam));
    }

    /**
     * 查询学生在指定学期下的选课记录
     *
     * @param requestParam 查询条件
     * @return 选课记录集合
     */
    default List<CourseSelectionDO> listSelectionByStudent(StudentSelectionQueryReqDTO requestParam) {
        return selectList(buildStudentQueryWrapper(requestParam));
    }

    /**
     * 统计教学班已选人数
     *
     * @param teachingClassId 教学班 ID
     * @return 已选人数
     */
    default long countSelectedByTeachingClassId(Long teachingClassId) {
        return selectCount(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getTeachingClassId, teachingClassId)
                .eq(CourseSelectionDO::getStatus, SelectionStatusEnum.SELECTED));
    }

    /**
     * 统计教学班候补人数
     *
     * @param teachingClassId 教学班 ID
     * @return 候补人数
     */
    default long countWaitingByTeachingClassId(Long teachingClassId) {
        return selectCount(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getTeachingClassId, teachingClassId)
                .eq(CourseSelectionDO::getStatus, SelectionStatusEnum.WAITING));
    }

    /**
     * 统计该教学班内排在本条候补记录之前的候补人数，用于实时计算位次
     *
     * @param teachingClassId 教学班 ID
     * @param waitlistNo      当前候补序号
     * @return 排在之前的候补人数
     */
    default long countWaitingAhead(Long teachingClassId, Integer waitlistNo) {
        if (waitlistNo == null) {
            return 0L;
        }
        return selectCount(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getTeachingClassId, teachingClassId)
                .eq(CourseSelectionDO::getStatus, SelectionStatusEnum.WAITING)
                .lt(CourseSelectionDO::getWaitlistNo, waitlistNo));
    }

    /**
     * 查询该教学班当前最大候补序号，无候补时返回 null
     *
     * @param teachingClassId 教学班 ID
     * @return 最大候补序号
     */
    default Integer getMaxWaitlistNo(Long teachingClassId) {
        CourseSelectionDO result = selectOne(Wrappers.<CourseSelectionDO>lambdaQuery()
                .select(CourseSelectionDO::getWaitlistNo)
                .eq(CourseSelectionDO::getTeachingClassId, teachingClassId)
                .isNotNull(CourseSelectionDO::getWaitlistNo)
                .orderByDesc(CourseSelectionDO::getWaitlistNo)
                .last("LIMIT 1"));
        return result == null ? null : result.getWaitlistNo();
    }

    /**
     * 查询教学班候补队列，按候补序号升序排列
     *
     * @param teachingClassId 教学班 ID
     * @return 候补队列
     */
    default List<CourseSelectionDO> listWaitingByTeachingClassId(Long teachingClassId) {
        return selectList(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getTeachingClassId, teachingClassId)
                .eq(CourseSelectionDO::getStatus, SelectionStatusEnum.WAITING)
                .orderByAsc(CourseSelectionDO::getWaitlistNo)
                .orderByAsc(CourseSelectionDO::getId));
    }

    /**
     * 分页查询学生候补中的选课记录
     *
     * @param page         分页参数
     * @param requestParam 查询条件，包含学生 ID 与学期 ID
     * @return 分页结果
     */
    default IPage<CourseSelectionDO> listWaitlistByStudent(IPage<CourseSelectionDO> page,
                                                           StudentSelectionQueryReqDTO requestParam) {
        return selectPage(page, buildWaitlistQueryWrapper(requestParam));
    }
    default long countSelectedByStudent(StudentSelectionQueryReqDTO requestParam) {
        return selectCount(buildStudentQueryWrapper(requestParam));
    }

    /**
     * 统计指定批次下已选记录数量
     *
     * @param batchId 选课批次 ID
     * @return 已选记录数量
     */
    default long countSelectedByBatchId(Long batchId) {
        return selectCount(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getBatchId, batchId)
                .eq(CourseSelectionDO::getStatus, SelectionStatusEnum.SELECTED));
    }

    /**
     * 统计指定批次下指定状态的记录数量
     *
     * @param batchId 选课批次 ID
     * @param status  选课状态，为空时只按批次过滤
     * @return 记录数量
     */
    default long countByBatchIdAndStatus(Long batchId, SelectionStatusEnum status) {
        return selectCount(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getBatchId, batchId)
                .eq(status != null, CourseSelectionDO::getStatus, status));
    }

    /**
     * 统计教学班已退课记录数量，用于退课率分析
     *
     * @param teachingClassId 教学班 ID
     * @return 已退课记录数量
     */
    default long countWithdrawnByTeachingClassId(Long teachingClassId) {
        return selectCount(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getTeachingClassId, teachingClassId)
                .eq(CourseSelectionDO::getStatus, SelectionStatusEnum.WITHDRAWN));
    }

    /**
     * 查询教学班下的全部选课记录，包含已选 / 已退课 / 候补中，调用方按需过滤状态
     *
     * @param teachingClassId 教学班 ID
     * @return 选课记录集合
     */
    default List<CourseSelectionDO> listSelectionByTeachingClassId(Long teachingClassId) {
        return selectList(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getTeachingClassId, teachingClassId)
                .orderByAsc(CourseSelectionDO::getId));
    }

    /**
     * 批量统计多个教学班的已选记录总数，教学班 ID 集合为空时返回 0
     *
     * @param teachingClassIds 教学班 ID 集合
     * @return 已选记录总数
     */
    default long countSelectedByTeachingClassIds(List<Long> teachingClassIds) {
        if (teachingClassIds == null || teachingClassIds.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<CourseSelectionDO>lambdaQuery()
                .in(CourseSelectionDO::getTeachingClassId, teachingClassIds)
                .eq(CourseSelectionDO::getStatus, SelectionStatusEnum.SELECTED));
    }

    /**
     * 批量统计多个教学班的已选人数，返回教学班 ID 到已选人数的映射
     *
     * <p>通过一次 IN 查询取回命中的教学班 ID，再在内存中按教学班聚合计数，避免逐教学班 N+1 查询。
     *
     * @param teachingClassIds 教学班 ID 集合
     * @return 教学班 ID 到已选人数的映射
     */
    default Map<Long, Long> countSelectedMapByTeachingClassIds(List<Long> teachingClassIds) {
        if (teachingClassIds == null || teachingClassIds.isEmpty()) {
            return Map.of();
        }
        List<Object> records = selectObjs(Wrappers.<CourseSelectionDO>lambdaQuery()
                .select(CourseSelectionDO::getTeachingClassId)
                .in(CourseSelectionDO::getTeachingClassId, teachingClassIds)
                .eq(CourseSelectionDO::getStatus, SelectionStatusEnum.SELECTED));
        Map<Long, Long> result = new HashMap<>();
        for (Object each : records) {
            if (each == null) {
                continue;
            }
            Long classId = ((Number) each).longValue();
            result.merge(classId, 1L, Long::sum);
        }
        return result;
    }

    /**
     * 查询存在候补记录的教学班 ID 集合
     *
     * @return 教学班 ID 集合
     */
    default List<Long> listTeachingClassIdHavingWaiting() {
        List<Object> records = selectObjs(Wrappers.<CourseSelectionDO>lambdaQuery()
                .select(CourseSelectionDO::getTeachingClassId)
                .eq(CourseSelectionDO::getStatus, SelectionStatusEnum.WAITING)
                .groupBy(CourseSelectionDO::getTeachingClassId));
        if (records == null) {
            return List.of();
        }
        return records.stream()
                .filter(Objects::nonNull)
                .map(each -> ((Number) each).longValue())
                .distinct()
                .toList();
    }

    /**
     * 统计学生在指定学期下的已选课程数量
     *
     * @param studentId  学生 ID
     * @param semesterId 学期 ID
     * @return 已选课程数量
     */
    default long countSelectedByStudentAndSemester(Long studentId, Long semesterId) {
        return selectCount(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getStudentId, studentId)
                .eq(CourseSelectionDO::getStatus, SelectionStatusEnum.SELECTED)
                .inSql(CourseSelectionDO::getTeachingClassId,
                        "SELECT id FROM teaching_class WHERE semester_id = " + semesterId));
    }

    /**
     * 查询学生的全部学期选课记录，用于跨学期学分累计（C8 专用方法）
     * 该方法不做任何学期过滤，也不限定状态，调用方按业务口径自行过滤
     *
     * @param studentId 学生 ID
     * @return 选课记录集合
     */
    default List<CourseSelectionDO> listSelectionByStudentId(Long studentId) {
        return selectList(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getStudentId, studentId)
                .orderByAsc(CourseSelectionDO::getId));
    }

    /**
     * 构建学生候补队列查询条件，固定过滤候补状态
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<CourseSelectionDO> buildWaitlistQueryWrapper(StudentSelectionQueryReqDTO requestParam) {
        LambdaQueryWrapper<CourseSelectionDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(requestParam.getStudentId() != null, CourseSelectionDO::getStudentId, requestParam.getStudentId());
        queryWrapper.eq(CourseSelectionDO::getStatus, SelectionStatusEnum.WAITING);
        if (requestParam.getSemesterId() != null) {
            queryWrapper.inSql(CourseSelectionDO::getTeachingClassId,
                    "SELECT id FROM teaching_class WHERE semester_id = " + requestParam.getSemesterId());
        }
        queryWrapper.orderByAsc(CourseSelectionDO::getWaitlistNo);
        queryWrapper.orderByAsc(CourseSelectionDO::getId);
        return queryWrapper;
    }

    /**
     * 保存选课记录
     *
     * @param requestParam 选课记录数据对象
     * @return 影响行数
     */
    default int saveSelection(CourseSelectionDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新选课记录
     *
     * @param requestParam 选课记录数据对象
     * @return 影响行数
     */
    default int updateSelection(CourseSelectionDO requestParam) {
        return updateById(requestParam);
    }

    /**
     * 构建选课记录查询条件
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<CourseSelectionDO> buildQueryWrapper(CourseSelectionDO requestParam) {
        LambdaQueryWrapper<CourseSelectionDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(requestParam.getStudentId() != null, CourseSelectionDO::getStudentId, requestParam.getStudentId());
        queryWrapper.eq(requestParam.getTeachingClassId() != null, CourseSelectionDO::getTeachingClassId,
                requestParam.getTeachingClassId());
        queryWrapper.eq(requestParam.getBatchId() != null, CourseSelectionDO::getBatchId, requestParam.getBatchId());
        queryWrapper.eq(requestParam.getStatus() != null, CourseSelectionDO::getStatus, requestParam.getStatus());
        queryWrapper.orderByAsc(CourseSelectionDO::getId);
        return queryWrapper;
    }

    /**
     * 构建学生维度选课查询条件，学期过滤通过教学班子查询实现
     *
     * @param requestParam 查询条件
     * @return 查询条件包装器
     */
    default LambdaQueryWrapper<CourseSelectionDO> buildStudentQueryWrapper(StudentSelectionQueryReqDTO requestParam) {
        LambdaQueryWrapper<CourseSelectionDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(requestParam.getStudentId() != null, CourseSelectionDO::getStudentId, requestParam.getStudentId());
        SelectionStatusEnum status = EnumParseUtil.parseOrNull(SelectionStatusEnum.class, requestParam.getStatus());
        queryWrapper.eq(status != null, CourseSelectionDO::getStatus, status);
        if (requestParam.getSemesterId() != null) {
            queryWrapper.inSql(CourseSelectionDO::getTeachingClassId,
                    "SELECT id FROM teaching_class WHERE semester_id = " + requestParam.getSemesterId());
        }
        queryWrapper.orderByDesc(CourseSelectionDO::getId);
        return queryWrapper;
    }
}
