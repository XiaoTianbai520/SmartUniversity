package com.smart.university.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 数据库持久层配置
 */
@Configuration
@EnableTransactionManagement
@MapperScan("com.smart.university.mapper")
public class DataBaseConfiguration {

    /**
     * MyBatis-Plus 插件体系，当前启用 MySQL 物理分页插件
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页条数上限，防止一次性拉取过多数据
        paginationInterceptor.setMaxLimit(1000L);
        // 超出总页数时回到第一页
        paginationInterceptor.setOverflow(false);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}
