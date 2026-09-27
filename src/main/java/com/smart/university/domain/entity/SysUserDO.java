package com.smart.university.domain.entity;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.smart.university.common.enums.RoleEnum;

/**
 * 系统用户表
 */
@Data
public class SysUserDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 密码哈希，不允许明文保存
     */
    private String passwordHash;

    /**
     * 角色：STUDENT/TEACHER/ADMIN
     */
    private RoleEnum role;

    /**
     * 账号状态：1启用，0禁用
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
