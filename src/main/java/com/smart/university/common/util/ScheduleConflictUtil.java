package com.smart.university.common.util;

import com.smart.university.domain.entity.TeachingClassScheduleDO;

/**
 * 排课冲突判定工具，按「星期 + 教学周 + 节次」三维判定，供选课时间冲突校验与智能排课复用。
 * 判定口径与 TeachingClassScheduleMapper#buildConflictWrapper 保持一致：
 * 星期相同、教学周区间有交集、节次区间有交集时判定为冲突；任一维度缺失按不冲突处理
 */
public final class ScheduleConflictUtil {

    private ScheduleConflictUtil() {
    }

    /**
     * 判定两段排课是否冲突
     *
     * @param source 待判定的排课
     * @param target 已有排课
     * @return 冲突返回 true
     */
    public static boolean isConflict(TeachingClassScheduleDO source, TeachingClassScheduleDO target) {
        if (source == null || target == null) {
            return false;
        }
        return isConflict(source.getWeekday(), source.getStartWeek(), source.getEndWeek(),
                source.getStartSection(), source.getEndSection(), target);
    }

    /**
     * 判定指定时间区间与已有排课是否冲突
     *
     * @param weekday      星期：1周一...7周日
     * @param startWeek    开始周
     * @param endWeek      结束周
     * @param startSection 开始节次
     * @param endSection   结束节次
     * @param target       已有排课
     * @return 冲突返回 true
     */
    public static boolean isConflict(Integer weekday, Integer startWeek, Integer endWeek,
                                     Integer startSection, Integer endSection,
                                     TeachingClassScheduleDO target) {
        if (target == null) {
            return false;
        }
        return isConflict(weekday, startWeek, endWeek, startSection, endSection,
                target.getWeekday(), target.getStartWeek(), target.getEndWeek(),
                target.getStartSection(), target.getEndSection());
    }

    /**
     * 判定两组「星期 + 教学周 + 节次」区间是否冲突
     *
     * @param weekday       星期一
     * @param startWeek     开始周一
     * @param endWeek       结束周一
     * @param startSection  开始节次一
     * @param endSection    结束节次一
     * @param targetWeekday 星期二
     * @param targetStartWeek 开始周二
     * @param targetEndWeek   结束周二
     * @param targetStartSection 开始节次二
     * @param targetEndSection   结束节次二
     * @return 冲突返回 true
     */
    public static boolean isConflict(Integer weekday, Integer startWeek, Integer endWeek,
                                     Integer startSection, Integer endSection,
                                     Integer targetWeekday, Integer targetStartWeek, Integer targetEndWeek,
                                     Integer targetStartSection, Integer targetEndSection) {
        if (weekday == null || targetWeekday == null || !weekday.equals(targetWeekday)) {
            return false;
        }
        return isRangeOverlap(startWeek, endWeek, targetStartWeek, targetEndWeek)
                && isRangeOverlap(startSection, endSection, targetStartSection, targetEndSection);
    }

    /**
     * 判定两个闭区间是否有交集，任一端点为空按无交集处理
     *
     * @param start        区间一起点
     * @param end          区间一终点
     * @param targetStart  区间二起点
     * @param targetEnd    区间二终点
     * @return 有交集返回 true
     */
    public static boolean isRangeOverlap(Integer start, Integer end, Integer targetStart, Integer targetEnd) {
        if (start == null || end == null || targetStart == null || targetEnd == null) {
            return false;
        }
        return start <= targetEnd && targetStart <= end;
    }
}
