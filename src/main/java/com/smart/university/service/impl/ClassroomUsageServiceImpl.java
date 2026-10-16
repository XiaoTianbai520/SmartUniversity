package com.smart.university.service.impl;

import com.smart.university.domain.dto.req.ClassroomUsageQueryReqDTO;
import com.smart.university.domain.dto.resp.ClassroomUsageExtraRespDTO;
import com.smart.university.domain.dto.resp.ClassroomUsagePageRespDTO;
import com.smart.university.domain.dto.resp.ClassroomUsageRespDTO;
import com.smart.university.domain.entity.ClassroomDO;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.entity.StatClassroomUsageDO;
import com.smart.university.domain.entity.TeachingClassScheduleDO;
import com.smart.university.mapper.ClassroomMapper;
import com.smart.university.mapper.StatClassroomUsageMapper;
import com.smart.university.mapper.TeachingClassScheduleMapper;
import com.smart.university.service.ClassroomUsageService;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教室利用率统计服务实现（S-03）
 *
 * <p>分母口径（决策 5 / Q7、决策 3 / Q19）：`教室可用数 × daysPerWeek × sectionsPerDay × 学期周数`，
 * 学期周数由 `LEAST(CEIL(DATEDIFF(end_date, start_date)/7), maxWeeks)` 推导，不补表字段；
 * 三项参数均可配，且必须在 extra 中回显。
 */
@Service
@RequiredArgsConstructor
public class ClassroomUsageServiceImpl implements ClassroomUsageService {

    /**
     * 按教室维度
     */
    private static final String DIMENSION_CLASSROOM = "CLASSROOM";

    /**
     * 按教学楼维度
     */
    private static final String DIMENSION_BUILDING = "BUILDING";

    /**
     * 按星期节次热力维度
     */
    private static final String DIMENSION_WEEKDAY_SECTION = "WEEKDAY_SECTION";

    /**
     * 空闲时段维度
     */
    private static final String DIMENSION_FREE_SLOT = "FREE_SLOT";

    /**
     * 一周天数，空闲时段按 7 天遍历
     */
    private static final int WEEKDAY_COUNT = 7;

    /**
     * 可用教室状态值
     */
    private static final int CLASSROOM_STATUS_ENABLED = 1;

    private final ClassroomMapper classroomMapper;
    private final TeachingClassScheduleMapper teachingClassScheduleMapper;
    private final SemesterService semesterService;
    private final StatClassroomUsageMapper statClassroomUsageMapper;

    /**
     * 每周计天数
     */
    @Value("${smart-university.statistics.classroom-usage.days-per-week:5}")
    private int daysPerWeek;

    /**
     * 每天计节次
     */
    @Value("${smart-university.statistics.classroom-usage.sections-per-day:10}")
    private int sectionsPerDay;

    /**
     * 学期周数推导上限
     */
    @Value("${smart-university.statistics.classroom-usage.max-weeks-per-semester:20}")
    private int maxWeeksPerSemester;

    @Override
    public ClassroomUsagePageRespDTO pageClassroomUsage(ClassroomUsageQueryReqDTO requestParam) {
        Long semesterId = resolveSemesterId(requestParam.getSemesterId());
        UsageContext context = buildUsageContext(semesterId);
        String dimension = requestParam.getDimension() == null || requestParam.getDimension().isEmpty()
                ? DIMENSION_CLASSROOM
                : requestParam.getDimension().toUpperCase();
        List<ClassroomUsageRespDTO> records = switch (dimension) {
            case DIMENSION_BUILDING -> buildBuildingRecords(context, requestParam.getBuildingName());
            case DIMENSION_WEEKDAY_SECTION -> buildWeekdaySectionRecords(context, requestParam.getBuildingName());
            case DIMENSION_FREE_SLOT -> buildFreeSlotRecords(context, requestParam.getBuildingName());
            default -> buildClassroomRecords(context, requestParam.getBuildingName());
        };
        int currentPage = requestParam.getCurrentPage();
        int limit = requestParam.getLimit();
        ClassroomUsagePageRespDTO result = new ClassroomUsagePageRespDTO();
        result.setRecords(paginate(records, currentPage, limit));
        result.setPage(currentPage);
        result.setPageSize(limit);
        result.setTotal(records.size());
        result.setExtra(buildExtra(context, requestParam.getBuildingName() == null
                || requestParam.getBuildingName().isEmpty()));
        return result;
    }

    @Override
    public ClassroomUsageExtraRespDTO getUsageExtra(Long semesterId) {
        UsageContext context = buildUsageContext(resolveSemesterId(semesterId));
        return buildExtra(context, true);
    }

    @Override
    public List<StatClassroomUsageDO> listClassroomUsageSnapshot(Long semesterId) {
        Long targetSemesterId = resolveSemesterId(semesterId);
        UsageContext context = buildUsageContext(targetSemesterId);
        LocalDate statDate = LocalDate.now();
        LocalDateTime generatedAt = LocalDateTime.now();
        List<StatClassroomUsageDO> result = new ArrayList<>();
        for (ClassroomUsageRespDTO each : context.classroomUsageList) {
            StatClassroomUsageDO snapshot = new StatClassroomUsageDO();
            snapshot.setSemesterId(targetSemesterId);
            snapshot.setClassroomId(each.getClassroomId());
            snapshot.setBuildingName(each.getBuildingName() == null ? "" : each.getBuildingName());
            snapshot.setRoomNo(each.getRoomNo() == null ? "" : each.getRoomNo());
            snapshot.setCapacity(each.getCapacity() == null ? 0 : each.getCapacity());
            snapshot.setUsageCount(each.getUsageCount());
            snapshot.setUsageHours(each.getUsageHours());
            snapshot.setTotalHours(each.getTotalHours());
            snapshot.setUsageRate(each.getUsageRate());
            snapshot.setStatDate(statDate);
            snapshot.setGeneratedAt(generatedAt);
            result.add(snapshot);
        }
        return result;
    }

    /**
     * 构建教室维度的记录
     *
     * @param context      统计上下文
     * @param buildingName 教学楼筛选，为空不过滤
     * @return 教室维度记录集合
     */
    private List<ClassroomUsageRespDTO> buildClassroomRecords(UsageContext context, String buildingName) {
        return context.classroomUsageList.stream()
                .filter(each -> matchBuilding(each.getBuildingName(), buildingName))
                .map(this::copyUsageRespDTO)
                .toList();
    }

    /**
     * 构建教学楼维度的记录
     *
     * @param context      统计上下文
     * @param buildingName 教学楼筛选，为空不过滤
     * @return 教学楼维度记录集合
     */
    private List<ClassroomUsageRespDTO> buildBuildingRecords(UsageContext context, String buildingName) {
        Map<String, List<ClassroomUsageRespDTO>> buildingMap = new HashMap<>();
        for (ClassroomUsageRespDTO each : context.classroomUsageList) {
            if (!matchBuilding(each.getBuildingName(), buildingName)) {
                continue;
            }
            buildingMap.computeIfAbsent(each.getBuildingName(), key -> new ArrayList<>()).add(each);
        }
        List<ClassroomUsageRespDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<ClassroomUsageRespDTO>> entry : buildingMap.entrySet()) {
            ClassroomUsageRespDTO record = new ClassroomUsageRespDTO();
            int usageCount = 0;
            int usageHours = 0;
            int totalHours = 0;
            for (ClassroomUsageRespDTO each : entry.getValue()) {
                usageCount += each.getUsageCount();
                usageHours += each.getUsageHours();
                totalHours += each.getTotalHours();
            }
            record.setBuildingName(entry.getKey());
            record.setUsageCount(usageCount);
            record.setUsageHours(usageHours);
            record.setTotalHours(totalHours);
            record.setUsageRate(calculateRate(usageHours, totalHours));
            result.add(record);
        }
        result.sort(Comparator.comparing(ClassroomUsageRespDTO::getBuildingName));
        return result;
    }

    /**
     * 构建星期节次热力记录
     *
     * @param context      统计上下文
     * @param buildingName 教学楼筛选，为空不过滤
     * @return 星期节次记录集合
     */
    private List<ClassroomUsageRespDTO> buildWeekdaySectionRecords(UsageContext context, String buildingName) {
        List<ClassroomDO> classroomDOList = filterClassroom(context.classroomDOList, buildingName);
        Set<Long> classroomIdSet = classroomDOList.stream().map(ClassroomDO::getId).collect(Collectors.toSet());
        Map<String, int[]> heatMap = new HashMap<>();
        for (Map.Entry<Long, List<TeachingClassScheduleDO>> entry : context.scheduleMap.entrySet()) {
            if (!classroomIdSet.contains(entry.getKey())) {
                continue;
            }
            for (TeachingClassScheduleDO each : entry.getValue()) {
                if (each.getWeekday() == null || each.getStartSection() == null || each.getEndSection() == null) {
                    continue;
                }
                for (int section = each.getStartSection(); section <= each.getEndSection(); section++) {
                    int[] counter = heatMap.computeIfAbsent(each.getWeekday() + "-" + section, key -> new int[2]);
                    counter[0] = counter[0] + 1;
                    counter[1] = counter[1] + calculateScheduleHours(each);
                }
            }
        }
        List<ClassroomUsageRespDTO> result = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : heatMap.entrySet()) {
            String[] parts = entry.getKey().split("-");
            ClassroomUsageRespDTO record = new ClassroomUsageRespDTO();
            record.setWeekday(Integer.valueOf(parts[0]));
            record.setSection(Integer.valueOf(parts[1]));
            record.setUsageCount(entry.getValue()[0]);
            record.setUsageHours(entry.getValue()[1]);
            result.add(record);
        }
        result.sort(Comparator.comparing(ClassroomUsageRespDTO::getWeekday)
                .thenComparing(ClassroomUsageRespDTO::getSection));
        return result;
    }

    /**
     * 构建空闲时段记录
     *
     * @param context      统计上下文
     * @param buildingName 教学楼筛选，为空不过滤
     * @return 空闲时段记录集合
     */
    private List<ClassroomUsageRespDTO> buildFreeSlotRecords(UsageContext context, String buildingName) {
        List<ClassroomDO> classroomDOList = filterClassroom(context.classroomDOList, buildingName);
        List<ClassroomUsageRespDTO> result = new ArrayList<>();
        for (ClassroomDO classroomDO : classroomDOList) {
            Set<String> occupiedSlotSet = new HashSet<>();
            for (TeachingClassScheduleDO each : context.scheduleMap.getOrDefault(classroomDO.getId(), List.of())) {
                if (each.getWeekday() == null || each.getStartSection() == null || each.getEndSection() == null) {
                    continue;
                }
                for (int section = each.getStartSection(); section <= each.getEndSection(); section++) {
                    occupiedSlotSet.add(each.getWeekday() + "-" + section);
                }
            }
            for (int weekday = 1; weekday <= WEEKDAY_COUNT; weekday++) {
                for (int section = 1; section <= sectionsPerDay; section++) {
                    if (occupiedSlotSet.contains(weekday + "-" + section)) {
                        continue;
                    }
                    ClassroomUsageRespDTO record = new ClassroomUsageRespDTO();
                    record.setClassroomId(classroomDO.getId());
                    record.setBuildingName(classroomDO.getBuildingName());
                    record.setRoomNo(classroomDO.getRoomNo());
                    record.setCapacity(classroomDO.getCapacity());
                    record.setWeekday(weekday);
                    record.setSection(section);
                    result.add(record);
                }
            }
        }
        return result;
    }

    /**
     * 构建统计上下文：学期周数、教室列表、排课分组与每个教室的利用率
     *
     * @param semesterId 学期 ID
     * @return 统计上下文
     */
    private UsageContext buildUsageContext(Long semesterId) {
        UsageContext context = new UsageContext();
        context.semesterId = semesterId;
        SemesterDO semesterDO = semesterService.getSemesterById(semesterId);
        context.semesterWeeks = resolveSemesterWeeks(semesterDO);
        context.totalHours = daysPerWeek * sectionsPerDay * context.semesterWeeks;
        context.classroomDOList = classroomMapper.listClassroomByStatus(CLASSROOM_STATUS_ENABLED);
        List<TeachingClassScheduleDO> scheduleDOList = teachingClassScheduleMapper.listScheduleBySemesterId(semesterId);
        Map<Long, List<TeachingClassScheduleDO>> scheduleMap = new HashMap<>();
        for (TeachingClassScheduleDO each : scheduleDOList) {
            if (each.getClassroomId() == null) {
                continue;
            }
            scheduleMap.computeIfAbsent(each.getClassroomId(), key -> new ArrayList<>()).add(each);
        }
        context.scheduleMap = scheduleMap;
        List<ClassroomUsageRespDTO> classroomUsageList = new ArrayList<>();
        for (ClassroomDO classroomDO : context.classroomDOList) {
            List<TeachingClassScheduleDO> scheduleList = scheduleMap.getOrDefault(classroomDO.getId(), List.of());
            int usageHours = 0;
            for (TeachingClassScheduleDO each : scheduleList) {
                usageHours += calculateScheduleHours(each);
            }
            ClassroomUsageRespDTO record = new ClassroomUsageRespDTO();
            record.setClassroomId(classroomDO.getId());
            record.setBuildingName(classroomDO.getBuildingName());
            record.setRoomNo(classroomDO.getRoomNo());
            record.setCapacity(classroomDO.getCapacity());
            record.setUsageCount(scheduleList.size());
            record.setUsageHours(usageHours);
            record.setTotalHours(context.totalHours);
            record.setUsageRate(calculateRate(usageHours, context.totalHours));
            classroomUsageList.add(record);
        }
        classroomUsageList.sort(Comparator
                .comparing(ClassroomUsageRespDTO::getUsageRate, Comparator.reverseOrder())
                .thenComparing(ClassroomUsageRespDTO::getClassroomId));
        context.classroomUsageList = classroomUsageList;
        return context;
    }

    /**
     * 构建口径回显
     *
     * @param context            统计上下文
     * @param useAllClassroom    是否按全部教室统计整体利用率
     * @return 口径回显
     */
    private ClassroomUsageExtraRespDTO buildExtra(UsageContext context, boolean useAllClassroom) {
        int usageHours = 0;
        int totalHours = 0;
        int classroomCount = 0;
        for (ClassroomUsageRespDTO each : context.classroomUsageList) {
            usageHours += each.getUsageHours();
            totalHours += each.getTotalHours();
            classroomCount++;
        }
        ClassroomUsageExtraRespDTO result = new ClassroomUsageExtraRespDTO();
        result.setSemesterId(context.semesterId);
        result.setDaysPerWeek(daysPerWeek);
        result.setSectionsPerDay(sectionsPerDay);
        result.setSemesterWeeks(context.semesterWeeks);
        result.setClassroomCount(classroomCount);
        result.setOverallUsageRate(calculateRate(usageHours, totalHours));
        result.setSnapshotTime(resolveSnapshotTime(context.semesterId));
        return result;
    }

    /**
     * 快照生成时间，无快照时返回当前时间
     *
     * @param semesterId 学期 ID
     * @return 快照时间
     */
    private LocalDateTime resolveSnapshotTime(Long semesterId) {
        LocalDateTime generatedAt = statClassroomUsageMapper.getLatestGeneratedAt(semesterId);
        return generatedAt == null ? LocalDateTime.now() : generatedAt;
    }

    /**
     * 推导学期周数：LEAST(CEIL(DATEDIFF(结束日, 开始日)/7), 上限)
     *
     * @param semesterDO 学期数据对象
     * @return 学期周数，至少为 1
     */
    private int resolveSemesterWeeks(SemesterDO semesterDO) {
        if (semesterDO == null || semesterDO.getStartDate() == null || semesterDO.getEndDate() == null) {
            return maxWeeksPerSemester;
        }
        long days = ChronoUnit.DAYS.between(semesterDO.getStartDate(), semesterDO.getEndDate());
        long rawWeeks = (long) Math.ceil(days / 7.0);
        int weeks = (int) Math.min(rawWeeks, maxWeeksPerSemester);
        return weeks < 1 ? 1 : weeks;
    }

    /**
     * 计算单条排课的课时：节次数 × 周数
     *
     * @param scheduleDO 排课数据对象
     * @return 课时
     */
    private int calculateScheduleHours(TeachingClassScheduleDO scheduleDO) {
        if (scheduleDO.getStartSection() == null || scheduleDO.getEndSection() == null
                || scheduleDO.getStartWeek() == null || scheduleDO.getEndWeek() == null) {
            return 0;
        }
        int sectionCount = scheduleDO.getEndSection() - scheduleDO.getStartSection() + 1;
        int weekCount = scheduleDO.getEndWeek() - scheduleDO.getStartWeek() + 1;
        return Math.max(sectionCount, 0) * Math.max(weekCount, 0);
    }

    /**
     * 计算百分比，分母为 0 时返回 0，上限 100
     *
     * @param numerator   分子
     * @param denominator 分母
     * @return 百分比，保留 1 位小数
     */
    private BigDecimal calculateRate(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal rate = BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
        BigDecimal maxRate = BigDecimal.valueOf(100).setScale(1, RoundingMode.HALF_UP);
        return rate.compareTo(maxRate) > 0 ? maxRate : rate;
    }

    /**
     * 过滤教室列表
     *
     * @param classroomDOList 教室集合
     * @param buildingName    教学楼筛选，为空不过滤
     * @return 过滤后的教室集合
     */
    private List<ClassroomDO> filterClassroom(List<ClassroomDO> classroomDOList, String buildingName) {
        return classroomDOList.stream()
                .filter(each -> matchBuilding(each.getBuildingName(), buildingName))
                .toList();
    }

    /**
     * 判断教学楼是否命中筛选条件
     *
     * @param buildingName 教学楼名称
     * @param keyword      筛选关键字，为空时全部命中
     * @return 命中返回 true
     */
    private boolean matchBuilding(String buildingName, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        return Objects.equals(buildingName, keyword);
    }

    /**
     * 解析学期 ID，为空取当前学期
     *
     * @param semesterId 学期 ID
     * @return 学期 ID
     */
    private Long resolveSemesterId(Long semesterId) {
        return semesterId == null ? semesterService.getCurrentSemester().getId() : semesterId;
    }

    /**
     * 内存分页
     *
     * @param source      全量记录
     * @param currentPage 当前页码
     * @param limit       每页条数
     * @return 当前页记录
     */
    private <T> List<T> paginate(List<T> source, int currentPage, int limit) {
        int fromIndex = Math.min((currentPage - 1) * limit, source.size());
        int toIndex = Math.min(fromIndex + limit, source.size());
        return new ArrayList<>(source.subList(fromIndex, toIndex));
    }

    /**
     * 复制利用率记录，避免分页结果与原集合共享对象
     *
     * @param source 原记录
     * @return 复制后的记录
     */
    private ClassroomUsageRespDTO copyUsageRespDTO(ClassroomUsageRespDTO source) {
        ClassroomUsageRespDTO result = new ClassroomUsageRespDTO();
        result.setClassroomId(source.getClassroomId());
        result.setBuildingName(source.getBuildingName());
        result.setRoomNo(source.getRoomNo());
        result.setCapacity(source.getCapacity());
        result.setUsageCount(source.getUsageCount());
        result.setUsageHours(source.getUsageHours());
        result.setTotalHours(source.getTotalHours());
        result.setUsageRate(source.getUsageRate());
        return result;
    }

    /**
     * 教室利用率统计上下文
     */
    private static class UsageContext {

        /**
         * 学期 ID
         */
        private Long semesterId;

        /**
         * 推导出的学期周数
         */
        private int semesterWeeks;

        /**
         * 单个教室的可排课时总数
         */
        private int totalHours;

        /**
         * 参与统计的教室集合
         */
        private List<ClassroomDO> classroomDOList = List.of();

        /**
         * 教室 ID 到排课集合的映射
         */
        private Map<Long, List<TeachingClassScheduleDO>> scheduleMap = Map.of();

        /**
         * 按教室维度的利用率结果
         */
        private List<ClassroomUsageRespDTO> classroomUsageList = List.of();
    }
}
