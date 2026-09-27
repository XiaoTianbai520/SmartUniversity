package com.smart.university.common.context;

import com.smart.university.common.enums.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前登录用户上下文，身份信息一律从 Token 解析，不信任前端入参
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 系统用户 ID
     */
    private Long userId;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 展示名称
     */
    private String displayName;

    /**
     * 角色
     */
    private RoleEnum role;

    /**
     * 学生 ID，角色为学生时非空
     */
    private Long studentId;

    /**
     * 教师 ID，角色为教师时非空
     */
    private Long teacherId;

    /**
     * 当前请求携带的 Token
     */
    private String token;

    /**
     * 判断当前用户是否为学生
     *
     * @return 学生返回 true
     */
    public boolean isStudent() {
        return RoleEnum.STUDENT == this.role;
    }

    /**
     * 判断当前用户是否为教师
     *
     * @return 教师返回 true
     */
    public boolean isTeacher() {
        return RoleEnum.TEACHER == this.role;
    }

    /**
     * 判断当前用户是否为教务管理员
     *
     * @return 管理员返回 true
     */
    public boolean isAdmin() {
        return RoleEnum.ADMIN == this.role;
    }
}
