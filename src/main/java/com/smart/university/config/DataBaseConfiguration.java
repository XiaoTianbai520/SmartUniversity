package com.smart.university.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 数据库持久层配置
 */
@Configuration
@EnableTransactionManagement
@MapperScan("com.smart.university.mapper")
public class DataBaseConfiguration {
}
