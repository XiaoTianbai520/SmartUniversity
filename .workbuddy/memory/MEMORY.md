# 高校智慧教务选课平台（SmartUniversity）长期项目记忆

## 项目结构与文档位置

- 后端单体项目根目录：`C:\Users\c1338\Desktop\workplace\高校智慧教务选课平台\SmartUniversity`
- **注意有两个文档目录**：V1 时期的设计文档在单数 `doc/`，V1.1 起的文档在 `docs/`。
- V1.1 文档：`docs/V1.1_开发计划.md`（任务分解 + 文件归属矩阵 + 公共契约 + 验收）、
  `docs/V1.1_业务流程设计.md`、`docs/V1.1_接口清单.md`、`docs/V1.1_数据库增量设计.sql`。

## 技术栈与版本红线

- JDK 21 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.14 + MySQL 8.x + Redis + RocketMQ 2.3.1 + HuTool 5.8.29 + POI 5.4.1
- Spring Boot 3 必须用 `mybatis-plus-spring-boot3-starter`（`mybatis-plus-boot-starter` 只到 3.5.7 且给 Boot 2 用）
- MP 3.5.9 起 `PaginationInnerInterceptor` 拆到 `mybatis-plus-jsqlparser`，必须显式引入
- POI 用 5.4.1，本机 `~/.m2` 已离线可用，不需要联网

## 版本节奏

V1.0 核心闭环 → V1.1 业务增强（候补 / 通知 / 导入导出 / 成绩统计，已完成）→ V1.2 高并发（Redis + Lua）→ V2.0 智慧教务（数据分析）

## 不变的系统性约定

1. **三层校验不可省**：`@RequireRole` 判角色 → Service 内判数据归属 → 学生身份一律从 `UserContextHolder` 取，不信任请求参数。
2. **MyBatis-Plus 用法**：Mapper 继承 `BaseMapper` 但对外只暴露 `get/list/count/save/update/remove` 前缀的 `default` 方法；分页交给 `PaginationInnerInterceptor`；XML 只保留联表。
3. **枚举字段传枚举实例**，字符串转枚举统一走 `EnumParseUtil.parseOrNull`。
4. `" "` **主键全部自增**（`id-type: auto`），无逻辑删除，用 `status` 字段表达启停。

## V1.1 遗留的设计决策（改代码前务必先读）

1. 候补复用 `course_selection`（status 扩展 `WAITING`），唯一键 `(student_id, teaching_class_id)` 天然互斥，不建候补表。
2. 通知一人一条，幂等靠 `uk_notice_dedup(notice_type, biz_id, receiver_user_id)`。**该唯一键的副作用**：`biz_id` 为空时不去重，`COURSE_ADJUSTED` 因此必须传 `bizId = null` 才能每次调整都通知；监听器也只在 `bizId != null` 时才查重。
3. 通知走 Spring 应用事件：`NoticeEventPublisher.publish` → `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)` → 每个接收人一个 `REQUIRES_NEW` 事务落库。
4. 容量上调触发候补递补必须走 `afterCommit` 回调，不能在修改教学班的事务内同步 `tryPromote`。
5. Excel 导入一律**逐行独立事务**（外层不加 `@Transactional`），单行脏数据不能毁整个文件；必须用 `DataFormatter` 且对大整数做原值短路。

## 本机环境约束

- 无 MySQL / Redis / RocketMQ，**做不到运行时验证，只能过编译与静态审查**。
- `mvn -o -B clean compile -DskipTests` 离线可编译。缺依赖时挂代理：`-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890`（HTTPS 同理）。
- Bash 工具缺部分 coreutils（ls/mkdir 不可用，`find` 可用）；列目录优先用 Glob。
- 仓库里 `application*.yml` 长期存在用户自己未提交的改动（server-ip 按环境拆分），不要误回滚。
