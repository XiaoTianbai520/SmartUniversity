# CODEBUDDY.md

This file provides guidance to CodeBuddy Code when working with code in this repository.

## 项目简介

高校智慧教务选课平台（SmartUniversity）后端，Spring Boot 3 单体应用，接口契约对齐 `docs/接口清单.md`（V1.0 与 V1.1 全量接口的唯一契约文档，由原 V1.0 接口文档与 V1.1 接口清单合并而成），命名对齐《命名规范》（已封装为用户级技能 `code-naming-convention`）。

## 常用命令

```bash
# 编译（依赖已缓存时可加 -o 离线）
mvn -B compile

# 启动：必须显式指定端口，否则会落到随机端口
mvn -B spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080"

# 打包
mvn -B clean package -DskipTests
```

- 本机 JDK 21 + Maven 3.9.4。
- 首次拉依赖若慢/超时，挂本机代理：`$env:MAVEN_OPTS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890"`。
- 无测试目录，不要尝试 `mvn test`。
- 接口基础路径 `http://localhost:8080/api/v1`，无额外 context-path。

## 环境

MySQL 8.4 / Redis / RocketMQ 均在 `192.168.1.102`（root / 123456），配置在 `src/main/resources/application-dev.yml`。本机没有 MySQL 与 RocketMQ 服务端，只能编译 + 连远程库做冒烟。

- 建表：执行 `docs/高校智慧教务选课平台_V1_数据库建表.sql`（库名 `edu_course_selection`），再执行 `docs/V1.1_数据库增量设计.sql`。
- 灌测试数据：`db/test-data.sql`。
- 本机 mysql 客户端：`C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`。

### 测试账号（密码全部为 123456）

| 角色 | 账号 | 密码 | loginType |
|---|---|---|---|
| 管理员 | `admin` | `123456` | `ADMIN` |
| 教师 | `T10001` / `T10002` | `123456` | `TEACHER` |
| 学生 | `20260001` ~ `20260005` | `123456` | `STUDENT` |

登录：`POST /api/v1/auth/login`，body `{"username":"admin","password":"123456","loginType":"ADMIN"}`。

### 回归冒烟

```bash
python target/smoke.py         # 学生端 + 教师端：登录、可选课程、选课、课表、学分、退课、成绩
python target/smoke_admin.py   # 管理端：17 个只读接口 + 专业 CRUD + 越权/未鉴权校验
```

冒烟会在 `major` 表留下 `TEST01`，跑完用 `DELETE FROM major WHERE major_code='TEST01'` 清掉。

## 架构总览

```
com.smart.university
├── common          通用能力
│   ├── base        Result / PageResult 统一响应
│   ├── constant    RedisCommonConstant / RocketMQConstant
│   ├── context     UserContext / UserContextHolder（ThreadLocal 透传登录态）
│   ├── enums       ResultCodeEnum（全量业务错误码）/ RoleEnum
│   ├── exception   BizException / GlobalExceptionHandler
│   └── util        JwtUtil / RedisKeyUtil / EnumParseUtil
├── config          DataBaseConfiguration（含 MP 分页插件）/ RedisConfiguration / WebConfiguration
├── web
│   ├── annotation  RequireRole（角色权限）
│   └── interceptor AuthenticationInterceptor（Token 解析 + 角色校验）
├── controller      按角色分包：auth / student / teacher / admin
├── service         接口 + impl，实现类继承 MP 的 ServiceImpl<Mapper, DO>
├── mapper          MyBatis-Plus Mapper，继承 BaseMapper<DO>
├── domain
│   ├── entity      xxxDO（@TableName / @TableId(AUTO)）
│   ├── dto.req     xxxReqDTO，对象入参统一命名 requestParam
│   ├── dto.resp    xxxRespDTO
│   └── enums       业务状态枚举，枚举名即数据库存储值
└── mq              message / producer / consumer
```

请求链路：`Controller（@RequireRole 判角色）→ Service（判数据归属）→ Mapper（BaseMapper + Wrapper）`。
学生身份一律从 `UserContextHolder` 取，不信任请求参数里的 `studentId` / `teacherId`。

## 关键约定

**命名**：方法用 `getXxx / listXxx / countXxx / saveXxx / removeXxx / updateXxx`；Controller/Service/Mapper 三层对象入参统一叫 `requestParam`；返回值变量 `result`，循环变量 `each`，捕获异常 `ex`。

**MyBatis-Plus**：
1. Mapper 继承 `BaseMapper<XxxDO>`，但对外只暴露规范方法名——单表操作在接口内用 `default` 方法组合 `selectList / selectCount / selectPage / insert / updateById / deleteById`。
2. 分页统一走 `PaginationInnerInterceptor`：Service 构造 `Page.of(current, size)` 传入 Mapper，从 `IPage` 取 `getRecords()` 与 `getTotal()`，不手写 count + LIMIT。
3. XML 只保留联表查询，当前仅 `TeachingClassMapper.xml`（管理端课程名模糊匹配、学生端可见范围 3 个 EXISTS），且不写 LIMIT。跨表简单过滤改用 `inSql` 子查询。
4. 7 个 DO 字段是枚举类型（教学班/学期/选课/成绩/批次的 `status`、`CourseDO.courseType`、`SysUserDO.role`），Wrapper 里必须传枚举实例，从字符串 DTO 转换走 `EnumParseUtil.parseOrNull`。

**错误码**：`ResultCodeEnum` 覆盖 `docs/接口清单.md` 声明的业务码，V1.0 段为 40001~40917，V1.1 段扩展至 40932，另补 `40900 数据已存在` 兜底唯一键冲突（`GlobalExceptionHandler` 捕获 `DuplicateKeyException`）。

**RocketMQ**：一个 Topic `edu_smart-university_topic`，业务用 Tag 区分；生产者组 `edu_smart-university_course-selection_pg`。无 MQ 环境时把 `smart-university.mq.enabled` 置 `false` 即可正常启动。

**核心链路**：`CourseSelectionServiceImpl#selectCourse` 按文档顺序做 13 步校验（身份→教学班→学期→批次→状态→专业→年级→重复选课→时间冲突→学分上限→容量→落库+发消息）。容量控制开关 `smart-university.selection.redis-deduct-enabled`。

## 文档索引

文档统一放在 `docs/`（早期分散在 `doc/` 与 `docs/`，已合并）：

- `docs/接口清单.md` —— 全量接口契约：V1.0 基础接口 + V1.1 新增接口、业务错误码、权限矩阵
- `docs/高校智慧教务选课平台_V1_数据库建表.sql` —— V1.0 建表脚本
- `docs/高校智慧教务选课平台_V1_ER图与数据库模型设计.md` —— ER 与数据库模型
- `docs/高校智慧教务选课平台_V1业务流程设计.md` —— V1.0 业务流程
- `docs/V1.1_开发计划.md` / `V1.1_业务流程设计.md` / `V1.1_数据库增量设计.sql` —— V1.1 设计与增量
- `docs/高校智慧教务选课平台_需求文档.md` / `版本开发规划.md` —— 需求与版本规划
- `README.md` —— 技术栈、目录结构、MyBatis-Plus 约定、安全设计、启动步骤

## 开发原则（来自 `.workbuddy/CODEBUDDY.md`）

1. 严格遵循需求文档定义的业务范围，当前只做 V1 范围内功能，不擅自添加或修改业务规则。
2. 数据库设计以现有 SQL 与 ER 模型为准，API 实现以接口文档为准。
3. 开发前先分析任务涉及哪些文档：需求 → 版本规划 → 业务流程 → ER/建表 → 接口文档。
4. **文档之间有冲突时先报告冲突，不要自行猜测**。

## 已知坑

- **启动端口**：不加 `--server.port=8080` 会落到随机端口，冒烟脚本连不上。
- **MyBatis-Plus 坐标**：Spring Boot 3 用 `mybatis-plus-spring-boot3-starter`；3.5.7 与 Spring Boot 3.2 不兼容（报 `factoryBeanObjectType`）；3.5.9 起 `PaginationInnerInterceptor` 拆到 `mybatis-plus-jsqlparser`，必须显式引入。当前用 3.5.14。
- **DO 包名是 `domain.entity` 不是 `domain.do`** —— `do` 是 Java 关键字。
- **Hutool 5.8.29 没有 `EnumUtil.getEnumOrNull`**，用项目内的 `EnumParseUtil`。
- **git push 后 `refs/remotes/origin/master` 写不进文件**，`git branch -vv` 会显示 `[origin/master: gone]`，但推送实际成功（看 `.git/FETCH_HEAD`）。修复：直接用 Python 写 `.git/refs/remotes/origin/master`（内容一行 40 位 sha）。推送走代理 `127.0.0.1:7890`。
