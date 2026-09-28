# 高校智慧教务选课平台（SmartUniversity）

Spring Boot 3 + MyBatis + MySQL + Redis + RocketMQ 单体式后端骨架。
接口契约严格对齐 `docs/接口清单.md`（全量接口的唯一契约文档，含 V1.0 与 V1.1），命名严格对齐《命名规范.md》。

## 技术选型

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | |
| Spring Boot | 3.2.5 | Web / Validation / AOP / Data Redis |
| MyBatis-Plus | 3.5.14 | 单表 CRUD 走 `BaseMapper` + `LambdaQueryWrapper`；物理分页走 `PaginationInnerInterceptor`；XML 仅保留联表查询 |
| MySQL | 8.x | 库名 `edu_course_selection`，建表脚本见上级目录 `高校智慧教务选课平台_V1_数据库建表.sql` |
| Redis | — | 登录态、教学班容量、学期与批次缓存 |
| RocketMQ | 2.3.1（starter） | 选课结果、成绩发布的异步解耦 |
| HuTool | 5.8.29 | 工具类统一走 HuTool，禁止重复造轮子 |

## 目录结构

```
com.smart.university
├── common          通用能力
│   ├── base        Result / PageResult 统一响应
│   ├── constant    RedisCommonConstant / RocketMQConstant
│   ├── context     UserContext / UserContextHolder（ThreadLocal 透传登录态）
│   ├── enums       ResultCodeEnum（全量业务错误码）/ RoleEnum
│   ├── exception   BizException / GlobalExceptionHandler
│   └── util        JwtUtil / RedisKeyUtil
├── config          DataBaseConfiguration / RedisConfiguration / WebConfiguration
├── web
│   ├── annotation  RequireRole（角色权限）
│   └── interceptor AuthenticationInterceptor（Token 解析 + 角色校验）
├── controller      按角色分包：auth / student / teacher / admin
├── service         接口 + impl，方法名 get / list / count / save / remove / update 前缀
├── mapper          MyBatis 接口，一一对应 resources/mapper/*.xml
├── domain
│   ├── entity      xxxDO，字段名与建表 SQL 完全对齐
│   ├── dto.req     xxxReqDTO，对象入参统一命名 requestParam
│   ├── dto.resp    xxxRespDTO
│   └── enums       业务状态枚举，枚举名即数据库存储值
└── mq              message / producer / consumer
```

> 注意：数据对象包名是 `domain.entity` 而不是 `domain.do`，因为 `do` 是 Java 关键字，不能作为包名。

## 命名约定落地情况

- 方法：`getXxx` / `listXxx` / `countXxx` / `saveXxx` / `removeXxx` / `updateXxx`
- 对象入参：Controller、Service、Mapper 三层统一 `requestParam`
- 领域模型：`SysUserDO`、`StudentSaveReqDTO`、`StudentPageQueryRespDTO`
- 配置类 / 常量类 / 上下文 / 枚举：`XxxConfiguration` / `XxxConstant` / `XxxContext` / `XxxEnum`
- 返回值变量 `result`，循环变量 `each`，Map 遍历 `entry`，捕获异常 `ex`
- RocketMQ：
  - Topic：`edu_smart-university_topic`
  - Tag：`edu_smart-university_course-selection_tag`、`..._course-withdraw_tag`、`..._score-publish_tag`
  - 生产者组：`edu_smart-university_course-selection_pg`
  - 消费者组：`edu_smart-university_course-selection_cg`、`edu_smart-university_score-publish_cg`
  - 发送时设置 KEYS（学生ID_教学班ID）、超时 2000ms、打印 SendResult；消费端保证幂等并按规范打印消费日志

## MyBatis-Plus 落地约定

1. **Mapper 继承 `BaseMapper<XxxDO>`，但对外仍只暴露规范方法名。**
   单表操作在接口内以 `default` 方法组合 `selectList / selectCount / selectPage / insert / updateById / deleteById` 实现，
   方法名保持 `get / list / count / save / remove / update` 前缀，Service 层调用方式与命名规范一致。
2. **分页统一交给 `PaginationInnerInterceptor`。**
   Service 构造 `Page.of(current, size)` 传入 Mapper，从返回的 `IPage` 取 `getRecords()` 与 `getTotal()`，
   不再手写 `count` + `LIMIT`，也不引入 PageHelper。单页上限 1000 条。
3. **XML 只保留联表查询。**
   当前仅 `TeachingClassMapper.xml` 保留 4 个语句（管理端按课程名模糊匹配、学生端可见范围判定），
   并去掉了手写 `LIMIT`，由分页插件改写。跨表的简单过滤改用 `inSql` 子查询（如成绩按学期过滤、排课冲突判定）。
4. **枚举字段必须传枚举实例。**
   `TeachingClassDO.status`、`SemesterDO.status`、`CourseSelectionDO.status`、`ScoreDO.status`、
   `SelectionBatchDO.status`、`CourseDO.courseType`、`SysUserDO.role` 均为枚举类型，
   从字符串 DTO 转换统一走 `EnumParseUtil.parseOrNull`，不要直接把字符串塞进 `Wrapper`。
5. **Service 实现类继承 `ServiceImpl<XxxMapper, XxxDO>` 作为能力底座**，业务方法仍调用 Mapper 规范方法。

### 版本坑（本机验证）

- Spring Boot 3 的坐标是 **`mybatis-plus-spring-boot3-starter`**，`mybatis-plus-boot-starter` 只到 3.5.7 且用于 Boot 2。
- 3.5.7 与 Spring Boot 3.2 不兼容，启动报 `Invalid value type for attribute 'factoryBeanObjectType'`。
- 3.5.9 起 `PaginationInnerInterceptor` 被拆到 **`mybatis-plus-jsqlparser`** 模块，需**显式引入**该依赖，
  否则编译报找不到符号。

## 安全设计（对应接口文档第 21 节）

1. `studentId` / `teacherId` / `userId` 一律从 Token 还原，禁止前端传入；查询类 DTO 中的这些字段由 Service 在后端填充。
2. `@RequireRole` 解决"你是不是这个角色"，Service 内再做数据归属校验解决"这个教学班是不是你的"。
3. 学生课程查询与选课是**两层校验**：列表查询时按专业 / 年级 / 学期 / 批次 / 开放状态过滤，点击选课时再完整跑一遍 13 步校验。
4. 密码使用 BCrypt 哈希，不落明文。

## 核心链路：学生选课

`CourseSelectionServiceImpl#selectCourse` 按文档建议顺序校验：

```
1  当前用户是否为学生      → 40101 / 40402
2  教学班是否存在          → 40405
3  教学班是否属于当前学期
4  是否存在有效选课批次    → 40912
5  教学班是否属于当前批次  → 40912
6  教学班状态是否允许选课
7  专业是否符合            → 40914
8  年级是否符合            → 40915
9  是否重复选课            → 40909
10 是否与已选课程时间冲突  → 40908
11 是否超过学期学分上限    → 40911
12 教学班容量是否已满      → 40910
13 写入 / 恢复选课记录（事务）+ 发送选课消息
```

容量控制由 `smart-university.selection.redis-deduct-enabled` 控制：
- `true`：Redis 原子自增预扣减，超出则回退并抛 40910（V1.2 高并发形态，可进一步换成 Lua 脚本）
- `false`：直接以数据库实时统计判定

## 事务边界

已加 `@Transactional` 的位置：新增 / 修改学生、新增 / 修改教师（含 sys_user 联动）、创建教学班（含专业年级关联）、选课、退课、成绩保存与发布。

## 异常处理

`GlobalExceptionHandler` 统一兜底：业务异常按错误码返回，参数校验异常返回 `40001`，JSON 解析 / 参数缺失 / 类型不匹配返回 `40002`，**数据库唯一键冲突返回 `40900 数据已存在`**（避免穿透成 500），其余归为 `500 服务器内部异常`。

`40900` 是接口文档 26 个错误码之外的补充码，用于兜底所有未显式查重的唯一键（年级、行政班、教室、学期等基础数据）。若某类资源需要更精确的提示，可在对应 Service 内参照 `MajorServiceImpl#checkMajorCodeUnique` 做显式查重。

## 启动前准备

1. 执行上级目录的 `高校智慧教务选课平台_V1_数据库建表.sql` 建库建表。
2. 可选：执行 `db/test-data.sql` 灌入测试数据（含管理员 / 教师 / 学生账号、课程、教学班、排课、选课批次、一条已发布成绩）。
3. 修改 `src/main/resources/application-dev.yml` 里的 MySQL 与 Redis 地址。
4. 初始化一个管理员账号（`sys_user.password_hash` 存 BCrypt 哈希，可用 `BCrypt.hashpw("123456", BCrypt.gensalt())` 生成）。
5. RocketMQ：`smart-university.mq.enabled` 控制，本地没部署 NameServer + Broker 时置 `false` 即可正常启动。
6. 启动：`mvn spring-boot:run`，接口基础路径 `http://localhost:8080/api/v1`。

## 测试数据

`db/test-data.sql` 灌入后可直接联调。**测试账号密码如下，8 个账号的密码全部是 `123456`**：

| 角色 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 管理员 | `admin` | `123456` | 可访问全部 `/api/v1/admin/**` |
| 教师 | `T10001` | `123456` | 张老师（副教授） |
| 教师 | `T10002` | `123456` | 李老师 |
| 学生 | `20260001` | `123456` | 张三，软件工程 2026 级 1 班 |
| 学生 | `20260002` ~ `20260005` | `123456` | 同专业同班级 |

登录时 `loginType` 取值 `ADMIN` / `TEACHER` / `STUDENT`，示例：

```json
POST /api/v1/auth/login
{"username": "admin", "password": "123456", "loginType": "ADMIN"}
```

密码以 BCrypt 存在 `sys_user.password_hash`，8 个账号共用同一哈希；改密码用
`BCrypt.hashpw("新密码", BCrypt.gensalt())` 重新生成后替换。

数据规模：5 名学生、2 名教师、2 个专业、1 个年级、2 个行政班、3 间教室、1 个当前学期、5 门课程、5 个教学班（含排课）、1 个进行中的选课批次。

## 环境验证状态

已在一台 MySQL 8.4 + Redis + RocketMQ 的机器上完成端到端冒烟，全部通过：

- 学生端：登录、`/auth/me`、可选课程列表（含教师姓名）、选课、课表、学分统计、退课、已发布成绩、登出
- 教师端：登录、我的教学班、学生名单、成绩列表
- 管理端：17 个只读查询接口全 200；专业新增 / 修改 / 停用成功；专业编号重复返回 `40900 专业编号已存在`
- 安全：越权访问返回 `40301 无接口访问权限`，未携带 Token 返回 `40101 未登录`

## V1.1 业务增强版

V1.1 在核心闭环之上补齐候补、通知、批量导入导出、成绩统计四条辅助业务线。

设计文档统一放在 `docs/`（早期曾分散在 `doc/` 与 `docs/`，现已合并为一处）：

| 文档 | 内容 |
|---|---|
| `docs/接口清单.md` | **全量接口契约**：V1.0 基础接口 + V1.1 新增接口、错误码、权限矩阵 |
| `docs/V1.1_开发计划.md` | 任务分解、依赖关系、文件归属矩阵、公共契约、验收标准 |
| `docs/V1.1_业务流程设计.md` | 候补选课、系统通知、批量导入导出、成绩统计的可执行流程 |
| `docs/V1.1_数据库增量设计.sql` | 候补字段扩展 + `notification` 表 |
| `docs/高校智慧教务选课平台_V1_数据库建表.sql` | V1.0 建表脚本 |
| `docs/高校智慧教务选课平台_V1_ER图与数据库模型设计.md` | V1.0 ER 与数据库模型设计 |
| `docs/高校智慧教务选课平台_V1业务流程设计.md` | V1.0 业务流程设计 |
| `docs/高校智慧教务选课平台_需求文档.md` | 项目需求说明 |
| `docs/高校智慧教务选课平台_版本开发规划.md` | 版本演进规划 |

数据库升级：先执行 `docs/高校智慧教务选课平台_V1_数据库建表.sql`，再执行 `docs/V1.1_数据库增量设计.sql`。

### V1.1 新增接口

```
POST   /api/v1/student/teaching-classes/{teachingClassId}/waitlist
DELETE /api/v1/student/waitlist/{selectionId}
GET    /api/v1/student/waitlist
POST   /api/v1/admin/teaching-classes/{teachingClassId}/waitlist/promote

GET    /api/v1/notifications
GET    /api/v1/notifications/unread-count
PUT    /api/v1/notifications/{notificationId}/read
PUT    /api/v1/notifications/read-all

POST   /api/v1/admin/students/import
GET    /api/v1/admin/students/import-template
GET    /api/v1/admin/students/export
POST   /api/v1/admin/teachers/import
GET    /api/v1/admin/teachers/import-template
GET    /api/v1/admin/teachers/export
GET    /api/v1/teacher/teaching-classes/{id}/students/export
GET    /api/v1/teacher/teaching-classes/{id}/scores/import-template
POST   /api/v1/teacher/teaching-classes/{id}/scores/import

GET    /api/v1/teacher/teaching-classes/{id}/score-statistics
```

### V1.1 关键设计

- **候补复用选课记录表**：`course_selection.status` 扩展 `WAITING`，唯一键 `(student_id, teaching_class_id)` 保证同一学生不会同时占用正式名额与候补队列；候补序号 `waitlist_no` 不插队、不重排，位次实时计算。
- **通知走领域事件解耦**：业务侧调用 `NoticeEventPublisher.publish` 发布 `NoticeEvent`，由监听器在事务提交后落库，唯一键 `(notice_type, biz_id, receiver_user_id)` 保证幂等。
- **导入逐行隔离**：Excel 导入不套外层大事务，单行失败记录行号与原因后继续，避免一个脏数据毁掉整个文件。

## 接口清单

以下为 V1.0 接口一览，完整的请求 / 响应字段、错误码与 V1.1 新增接口统一见 `docs/接口清单.md`：

```
POST   /api/v1/auth/login
GET    /api/v1/auth/me
POST   /api/v1/auth/logout

GET    /api/v1/student/teaching-classes
GET    /api/v1/student/teaching-classes/{id}
POST   /api/v1/student/selections
GET    /api/v1/student/selections
DELETE /api/v1/student/selections/{id}
GET    /api/v1/student/timetable
GET    /api/v1/student/credits
GET    /api/v1/student/scores

GET    /api/v1/teacher/teaching-classes
GET    /api/v1/teacher/teaching-classes/{id}
GET    /api/v1/teacher/teaching-classes/{id}/students
GET    /api/v1/teacher/teaching-classes/{id}/scores
PUT    /api/v1/teacher/teaching-classes/{id}/scores/{selectionId}
POST   /api/v1/teacher/teaching-classes/{id}/scores/publish

CRUD   /api/v1/admin/students
CRUD   /api/v1/admin/teachers
CRUD   /api/v1/admin/majors
CRUD   /api/v1/admin/grades
CRUD   /api/v1/admin/academic-classes
CRUD   /api/v1/admin/classrooms
CRUD   /api/v1/admin/semesters
CRUD   /api/v1/admin/courses
CRUD   /api/v1/admin/teaching-classes
CRUD   /api/v1/admin/teaching-classes/{id}/schedules
CRUD   /api/v1/admin/selection-batches
CRUD   /api/v1/admin/selection-batches/{id}/teaching-classes
```
