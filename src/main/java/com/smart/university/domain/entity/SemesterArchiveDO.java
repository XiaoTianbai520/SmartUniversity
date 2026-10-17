package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * semester_archive 表，学期数据归档结果，按「学期 + 归档类型」覆盖写
 */
@Data
@TableName("semester_archive")
public class SemesterArchiveDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 归档ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 归档类型：SELECTION/SCORE/OVERVIEW
     */
    private String archiveType;

    /**
     * 归档内容（JSON）
     */
    private String archiveContent;

    /**
     * 归档时间
     */
    private LocalDateTime archivedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
