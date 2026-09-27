package com.smart.university.domain.dto.req;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 学生新增 / 修改入参
 */
@Data
public class StudentSaveReqDTO {

    /**
     * 学生 ID，修改时必传
     */
    private Long id;

    /**
     * 登录账号
     */
    @NotBlank(message = "登录账号不能为空")
    private String username;

    /**
     * 登录密码，新增时必传
     */
    private String password;

    /**
     * 学号
     */
    @NotBlank(message = "学号不能为空")
    private String studentNo;

    /**
     * 学生姓名
     */
    @NotBlank(message = "学生姓名不能为空")
    private String studentName;

    /**
     * 所属专业 ID
     */
    @NotNull(message = "专业 ID 不能为空")
    private Long majorId;

    /**
     * 所属年级 ID
     */
    @NotNull(message = "年级 ID 不能为空")
    private Long gradeId;

    /**
     * 所属行政班级 ID
     */
    @NotNull(message = "班级 ID 不能为空")
    private Long classId;
}
