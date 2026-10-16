package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * stat_course_selection 表，课程选课分析统计快照
 */
@Data
@TableName("stat_course_selection")
public class StatCourseSelectionDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 快照ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 教学班ID
     */
    private Long teachingClassId;

    /**
     * 课程编号快照
     */
    private String courseCode;

    /**
     * 课程名称快照
     */
    private String courseName;

    /**
     * 教学班编号快照
     */
    private String classCode;

    /**
     * 任课教师ID
     */
    private Long teacherId;

    /**
     * 任课教师姓名快照
     */
    private String teacherName;

    /**
     * 教学班容量
     */
    private Integer capacity;

    /**
     * 已选人数（DB 权威口径，不含候补）
     */
    private Integer selectedCount;

    /**
     * 候补人数
     */
    private Integer waitingCount;

    /**
     * 退课人数（仅 WITHDRAWN）
     */
    private Integer withdrawCount;

    /**
     * 是否满班：1 满班，0 未满
     */
    private Integer fullFlag;

    /**
     * 快照日期
     */
    private LocalDate statDate;

    /**
     * 快照生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
