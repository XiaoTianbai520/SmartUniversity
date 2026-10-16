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
 * stat_teacher_workload 表，教师工作量统计快照
 */
@Data
@TableName("stat_teacher_workload")
public class StatTeacherWorkloadDO implements Serializable {

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
     * 教师ID
     */
    private Long teacherId;

    /**
     * 教师编号快照
     */
    private String teacherNo;

    /**
     * 教师姓名快照
     */
    private String teacherName;

    /**
     * 教学班数
     */
    private Integer classCount;

    /**
     * 总课时（节次×周数之和）
     */
    private Integer totalHours;

    /**
     * 学生总人数（去重后）
     */
    private Integer studentCount;

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
