package com.smart.university.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 成绩发布事件，成绩发布事务提交后由监听器触发自动统计（A-06）
 *
 * <p>独立于 V1.1 的 {@link NoticeEvent}：本事件承载统计口径所需的教学班与选课记录维度，
 * 不参与站内通知落库，因此不复用也不修改 {@link NoticeEvent}。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScorePublishedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 教学班 ID
     */
    private Long teachingClassId;

    /**
     * 本次参与发布的选课记录 ID 集合
     */
    private List<Long> selectionIds;

    /**
     * 本次实际发布成功的条数
     */
    private Integer publishedCount;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 所属学期 ID
     */
    private Long semesterId;
}
