package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程选课分析口径回显（37.3 extra）
 */
@Data
public class CourseSelectionAnalysisExtraRespDTO {

    /**
     * 学期 ID
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 整体满班率百分比，保留 1 位小数
     */
    private BigDecimal fullClassRate;

    /**
     * 专业分布，合计等于已选人次之和
     */
    private List<MajorDistributionRespDTO> majorDistribution;

    /**
     * 年级分布，合计等于已选人次之和
     */
    private List<GradeDistributionRespDTO> gradeDistribution;

    /**
     * 快照生成时间，无快照时为当前时间
     */
    private LocalDateTime snapshotTime;
}
