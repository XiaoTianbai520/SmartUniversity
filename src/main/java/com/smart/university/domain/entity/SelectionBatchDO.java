package com.smart.university.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.smart.university.domain.enums.SelectionBatchStatusEnum;

/**
 * selection_batch 表
 */
@Data
@TableName("selection_batch")
public class SelectionBatchDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次名称
     */
    private String batchName;

    /**
     * 所属学期ID
     */
    private Long semesterId;

    /**
     * 选课开始时间
     */
    private LocalDateTime startTime;

    /**
     * 选课结束时间
     */
    private LocalDateTime endTime;

    /**
     * 退课截止时间
     */
    private LocalDateTime dropDeadline;

    /**
     * NOT_STARTED/IN_PROGRESS/ENDED
     */
    private SelectionBatchStatusEnum status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
