package com.smart.university.domain.dto.req;

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
     * 教师编号
     */
    @NotBlank(message = "教师编号不能为空")
    private String teacherNo;

    /**
     * 教师姓名
     */
    @NotBlank(message = "教师姓名不能为空")
    private String teacherName;

    /**
     * 职称
     */
    private String title;
}
