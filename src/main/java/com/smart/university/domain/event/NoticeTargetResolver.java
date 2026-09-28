package com.smart.university.domain.event;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.common.enums.RoleEnum;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.StudentDO;
import com.smart.university.domain.entity.SysUserDO;
import com.smart.university.domain.enums.SelectionStatusEnum;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.StudentMapper;
import com.smart.university.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 通知接收人解析器，将角色 / 教学班 / 选课记录解析为 sys_user.id 集合，
 * 仅使用 MyBatis-Plus BaseMapper 自带方法完成查询
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeTargetResolver {

    /**
     * 启用状态
     */
    private static final int ENABLED_STATUS = 1;

    private final SysUserMapper sysUserMapper;

    private final StudentMapper studentMapper;

    private final CourseSelectionMapper courseSelectionMapper;

    /**
     * 查询指定角色下的全部启用用户 ID，用于全角色群发
     *
     * @param role 角色
     * @return 用户 ID 列表，已去重且升序排列
     */
    public List<Long> listUserIdsByRole(RoleEnum role) {
        if (role == null) {
            return Collections.emptyList();
        }
        List<SysUserDO> userList = sysUserMapper.selectList(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getRole, role)
                .eq(SysUserDO::getStatus, ENABLED_STATUS)
                .orderByAsc(SysUserDO::getId));
        List<Long> result = new ArrayList<>(userList.size());
        for (SysUserDO each : userList) {
            result.add(each.getId());
        }
        return distinctSorted(result);
    }

    /**
     * 查询教学班下已选学生对应的用户 ID
     *
     * @param teachingClassId 教学班 ID
     * @return 用户 ID 列表，已去重且升序排列
     */
    public List<Long> listUserIdsByTeachingClassId(Long teachingClassId) {
        if (teachingClassId == null) {
            return Collections.emptyList();
        }
        List<CourseSelectionDO> selectionList = courseSelectionMapper.selectList(Wrappers.<CourseSelectionDO>lambdaQuery()
                .eq(CourseSelectionDO::getTeachingClassId, teachingClassId)
                .eq(CourseSelectionDO::getStatus, SelectionStatusEnum.SELECTED)
                .orderByAsc(CourseSelectionDO::getId));
        return listUserIdsBySelectionList(selectionList);
    }

    /**
     * 查询指定选课记录对应学生的用户 ID
     *
     * @param selectionIds 选课记录 ID 集合
     * @return 用户 ID 列表，已去重且升序排列
     */
    public List<Long> listUserIdsBySelectionIds(List<Long> selectionIds) {
        if (CollUtil.isEmpty(selectionIds)) {
            return Collections.emptyList();
        }
        List<CourseSelectionDO> selectionList = courseSelectionMapper.selectList(Wrappers.<CourseSelectionDO>lambdaQuery()
                .in(CourseSelectionDO::getId, selectionIds)
                .orderByAsc(CourseSelectionDO::getId));
        return listUserIdsBySelectionList(selectionList);
    }

    /**
     * 将选课记录转换为学生对应的用户 ID
     *
     * @param selectionList 选课记录集合
     * @return 用户 ID 列表，已去重且升序排列
     */
    private List<Long> listUserIdsBySelectionList(List<CourseSelectionDO> selectionList) {
        if (CollUtil.isEmpty(selectionList)) {
            return Collections.emptyList();
        }
        List<Long> studentIdList = new ArrayList<>(selectionList.size());
        for (CourseSelectionDO each : selectionList) {
            studentIdList.add(each.getStudentId());
        }
        List<StudentDO> studentList = studentMapper.selectList(Wrappers.<StudentDO>lambdaQuery()
                .in(StudentDO::getId, distinctSorted(studentIdList))
                .orderByAsc(StudentDO::getId));
        List<Long> result = new ArrayList<>(studentList.size());
        for (StudentDO each : studentList) {
            result.add(each.getUserId());
        }
        return distinctSorted(result);
    }

    /**
     * 去除空值与重复项，并按升序排列
     *
     * @param idList 待处理的 ID 列表
     * @return 去重且升序排列的 ID 列表
     */
    private List<Long> distinctSorted(List<Long> idList) {
        Set<Long> idSet = new TreeSet<>();
        for (Long each : idList) {
            if (each != null) {
                idSet.add(each);
            }
        }
        return new ArrayList<>(idSet);
    }
}
