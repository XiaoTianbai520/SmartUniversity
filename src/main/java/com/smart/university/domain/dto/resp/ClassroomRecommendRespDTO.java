package com.smart.university.domain.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 教室推荐出参（40.4）
 */
@Data
public class ClassroomRecommendRespDTO {

    /**
     * 教学班 ID
     */
    @Schema(example = "12")
    private Long teachingClassId;

    /**
     * 需要的教室容量
     */
    private Integer requiredCapacity;

    /**
     * 推荐教室集合，容量冗余升序 → 同教学楼优先 → 教室编号升序
     */
    private List<ClassroomRecommendItemRespDTO> records;
}
