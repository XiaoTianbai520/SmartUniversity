package com.smart.university.mapper;

import com.smart.university.domain.entity.SysUserDO;

/**
 * 系统用户持久层
 */
public interface SysUserMapper {

    /**
     * 根据 ID 查询用户
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    SysUserDO getUserById(Long userId);

    /**
     * 根据登录账号查询用户
     *
     * @param username 登录账号
     * @return 用户信息
     */
    SysUserDO getUserByUsername(String username);

    /**
     * 统计登录账号占用数量
     *
     * @param username 登录账号
     * @return 数量
     */
    long countUserByUsername(String username);

    /**
     * 保存用户
     *
     * @param requestParam 用户数据对象
     * @return 影响行数
     */
    int saveUser(SysUserDO requestParam);

    /**
     * 更新用户
     *
     * @param requestParam 用户数据对象
     * @return 影响行数
     */
    int updateUser(SysUserDO requestParam);
}
