package com.smart.university.domain.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 教师工作量统计查询入参（37.2）
 */
@Data
public class TeacherWorkloadQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学期 ID，缺省当前学期
     */
    @Schema(example = "1")
    private Long semesterId;

    /**
     * 教师 ID，教务端可选，教师端忽略该参数只查本人
     */
    @Schema(example = "1")
    private Long teacherId;

    /**
     * 教师姓名 / 工号关键字
     */
    @Schema(example = "张伟")
    private String keyword;
}
