package com.smart.university.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smart.university.domain.entity.SysUserDO;

/**
 * 系统用户持久层，单表操作由 MyBatis-Plus 的 BaseMapper 提供能力，
 * 方法名遵循 get / list / count / save / remove / update 前缀规范
 */
public interface SysUserMapper extends BaseMapper<SysUserDO> {

    /**
     * 根据 ID 查询用户
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    default SysUserDO getUserById(Long userId) {
        return selectById(userId);
    }

    /**
     * 根据登录账号查询用户
     *
     * @param username 登录账号
     * @return 用户信息
     */
    default SysUserDO getUserByUsername(String username) {
        return selectOne(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getUsername, username)
                .last("LIMIT 1"));
    }

    /**
     * 统计登录账号占用数量
     *
     * @param username 登录账号
     * @return 数量
     */
    default long countUserByUsername(String username) {
        return selectCount(Wrappers.<SysUserDO>lambdaQuery().eq(SysUserDO::getUsername, username));
    }

    /**
     * 保存用户
     *
     * @param requestParam 用户数据对象
     * @return 影响行数
     */
    default int saveUser(SysUserDO requestParam) {
        return insert(requestParam);
    }

    /**
     * 更新用户
     *
     * @param requestParam 用户数据对象
     * @return 影响行数
     */
    default int updateUser(SysUserDO requestParam) {
        return updateById(requestParam);
    }
}
