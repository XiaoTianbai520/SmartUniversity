package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 教师新增 / 修改入参
 */
@Data
public class TeacherSaveReqDTO {

    /**
     * 教师 ID，修改时必传
     */
    @Schema(example = "1")
    private Long id;

    /**
     * 登录账号
     */
    @Schema(example = "20260001")
    @NotBlank(message = "登录账号不能为空")
    private String username;

    /**
     * 登录密码，新增时必传
     */
    @Schema(example = "123456")
    private String password;

    /**
     * 教师编号
     */
    @Schema(example = "T10001")
    @NotBlank(message = "教师编号不能为空")
    private String teacherNo;

    /**
     * 教师姓名
     */
    @Schema(example = "张伟")
    @NotBlank(message = "教师姓名不能为空")
    private String teacherName;

    /**
     * 职称
     */
    @Schema(example = "系统通知标题")
    private String title;
}
