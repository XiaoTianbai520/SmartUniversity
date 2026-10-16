package com.smart.university.service.impl;

import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.common.util.ScheduleConflictUtil;
import com.smart.university.domain.dto.req.ClassroomRecommendQueryReqDTO;
import com.smart.university.domain.dto.resp.ClassroomRecommendItemRespDTO;
import com.smart.university.domain.dto.resp.ClassroomRecommendRespDTO;
import com.smart.university.domain.entity.ClassroomDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.entity.TeachingClassScheduleDO;
import com.smart.university.mapper.ClassroomMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.smart.university.mapper.TeachingClassScheduleMapper;
import com.smart.university.service.ClassroomRecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排课冲突检测与教室推荐服务实现（I-04）
 *
 * <p>冲突判定复用 T0 交付的 {@link ScheduleConflictUtil}（星期 + 教学周 + 节次三维），
 * 候选教室取自 `classroom.status=1` 且容量不小于教学班需求容量，只做推荐不做自动排课求解（Q15）。
 */
@Service
@RequiredArgsConstructor
public class ClassroomRecommendServiceImpl implements ClassroomRecommendService {

    /**
     * 默认推荐条数
     */
    private static final int DEFAULT_TOP_N = 5;

    /**
     * 可用教室状态值
     */
    private static final int CLASSROOM_STATUS_ENABLED = 1;

    private final ClassroomMapper classroomMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final TeachingClassScheduleMapper teachingClassScheduleMapper;

    @Override
    public ClassroomRecommendRespDTO listClassroomRecommend(ClassroomRecommendQueryReqDTO requestParam) {
        TeachingClassDO teachingClassDO = teachingClassMapper.getTeachingClassById(requestParam.getTeachingClassId());
        if (teachingClassDO == null) {
            throw new BizException(ResultCodeEnum.TEACHING_CLASS_NOT_EXIST);
        }
        int requiredCapacity = teachingClassDO.getCapacity() == null ? 0 : teachingClassDO.getCapacity();
        List<ClassroomDO> classroomDOList = classroomMapper.listClassroomByStatus(CLASSROOM_STATUS_ENABLED).stream()
                .filter(each -> each.getCapacity() != null && each.getCapacity() >= requiredCapacity)
                .toList();
        if (classroomDOList.isEmpty()) {
            throw new BizException(ResultCodeEnum.CLASSROOM_NOT_EXIST);
        }
        String preferredBuildingName = resolvePreferredBuildingName(teachingClassDO.getId());
        boolean timeSpecified = requestParam.getWeekday() != null
                && requestParam.getStartSection() != null
                && requestParam.getEndSection() != null
                && requestParam.getStartWeek() != null
                && requestParam.getEndWeek() != null;
        List<ClassroomRecommendItemRespDTO> records = new ArrayList<>();
        for (ClassroomDO each : classroomDOList) {
            boolean conflict = false;
            if (timeSpecified) {
                for (TeachingClassScheduleDO scheduleDO : teachingClassScheduleMapper
                        .listScheduleByClassroomId(each.getId())) {
                    if (ScheduleConflictUtil.isConflict(requestParam.getWeekday(), requestParam.getStartWeek(),
                            requestParam.getEndWeek(), requestParam.getStartSection(), requestParam.getEndSection(),
                            scheduleDO)) {
                        conflict = true;
                        break;
                    }
                }
            }
            if (conflict) {
                continue;
            }
            records.add(buildRecommendItem(each, requiredCapacity, preferredBuildingName, timeSpecified));
        }
        records.sort(Comparator.comparing(ClassroomRecommendItemRespDTO::getCapacityMargin)
                .thenComparing(each -> Boolean.TRUE.equals(each.getBuildingName() != null
                        && each.getBuildingName().equals(preferredBuildingName)) ? 0 : 1)
                .thenComparing(each -> each.getRoomNo() == null ? "" : each.getRoomNo()));
        int topN = requestParam.getTopN() == null || requestParam.getTopN() < 1
                ? DEFAULT_TOP_N
                : requestParam.getTopN();
        ClassroomRecommendRespDTO result = new ClassroomRecommendRespDTO();
        result.setTeachingClassId(teachingClassDO.getId());
        result.setRequiredCapacity(requiredCapacity);
        result.setRecords(records.stream().limit(topN).toList());
        return result;
    }

    /**
     * 构建单个推荐记录
     *
     * @param classroomDO          教室数据对象
     * @param requiredCapacity     需求容量
     * @param preferredBuildingName 优先教学楼名称，为空表示无偏好
     * @param timeSpecified        是否指定了目标时段
     * @return 推荐记录
     */
    private ClassroomRecommendItemRespDTO buildRecommendItem(ClassroomDO classroomDO, int requiredCapacity,
                                                             String preferredBuildingName, boolean timeSpecified) {
        int capacityMargin = classroomDO.getCapacity() - requiredCapacity;
        boolean sameBuilding = preferredBuildingName != null
                && preferredBuildingName.equals(classroomDO.getBuildingName());
        List<String> reasonList = new ArrayList<>();
        reasonList.add("容量冗余最小（" + capacityMargin + " 个座位）");
        if (sameBuilding) {
            reasonList.add("与目标教学班同处" + preferredBuildingName);
        }
        reasonList.add(timeSpecified ? "该时段无排课冲突" : "未指定目标时段，未做冲突校验");
        ClassroomRecommendItemRespDTO result = new ClassroomRecommendItemRespDTO();
        result.setClassroomId(classroomDO.getId());
        result.setBuildingName(classroomDO.getBuildingName());
        result.setRoomNo(classroomDO.getRoomNo());
        result.setCapacity(classroomDO.getCapacity());
        result.setCapacityMargin(capacityMargin);
        result.setReason(String.join("，", reasonList));
        return result;
    }

    /**
     * 取教学班现有排课所在教学楼，作为同楼优先的依据
     *
     * @param teachingClassId 教学班 ID
     * @return 教学楼名称，无既有排课时返回 null
     */
    private String resolvePreferredBuildingName(Long teachingClassId) {
        List<TeachingClassScheduleDO> scheduleDOList = teachingClassScheduleMapper
                .listScheduleByTeachingClassId(teachingClassId);
        List<Long> classroomIds = scheduleDOList.stream()
                .map(TeachingClassScheduleDO::getClassroomId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (classroomIds.isEmpty()) {
            return null;
        }
        Map<Long, ClassroomDO> classroomMap = classroomMapper.listClassroomByIds(classroomIds).stream()
                .collect(Collectors.toMap(ClassroomDO::getId, Function.identity(), (first, second) -> first));
        for (TeachingClassScheduleDO each : scheduleDOList) {
            ClassroomDO classroomDO = classroomMap.get(each.getClassroomId());
            if (classroomDO != null && classroomDO.getBuildingName() != null) {
                return classroomDO.getBuildingName();
            }
        }
        return null;
    }
}
