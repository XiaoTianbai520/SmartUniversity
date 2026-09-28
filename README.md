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

## 接口速查与说明（每个接口是干什么的）

> 字段级契约（请求体 / 响应体 / 枚举取值 / 错误码）以 `docs/接口清单.md` 为唯一权威来源。
> 下表只讲「这个接口是干什么的、谁能用、传什么」，方便对照调试。所有路径前缀均为 `/api/v1`。

### 认证（auth）

| 接口 | 方法 + 路径 | 干什么 | 权限 | 关键入参 / 出参 |
|---|---|---|---|---|
| 登录 | `POST /auth/login` | 用账号 + 密码 + 角色拿 Token，后续所有接口靠它鉴权 | 免登录 | 入：`{username,password,loginType}`；出：`{token,userId,role,username,displayName}` |
| 当前用户 | `GET /auth/me` | 返回登录者身份（学生额外带专业 / 年级 / 班级） | 已登录 | 出：当前用户详情 |
| 退出登录 | `POST /auth/logout` | 使当前 Token 失效 | 已登录 | — |

### 学生端（student）

| 接口 | 方法 + 路径 | 干什么 | 关键说明 |
|---|---|---|---|
| 可选课程 | `GET /student/teaching-classes` | 列出当前学生**有资格选**的教学班（按专业 / 年级 / 学期 / 批次 / 开放状态过滤） | 支持 `keyword` / `weekday` 筛选；不返回无权限的课程 |
| 课程详情 | `GET /student/teaching-classes/{id}` | 单个教学班详情，含课程 / 教师 / 排课 / 允许专业年级 | 无权查看返回 `40302` |
| 选课 | `POST /student/selections` | 选课，只传 `teachingClassId`，后端跑 13 步校验 | 冲突 / 已满 / 超学分等见 `docs` §4 错误码 |
| 我的课程 | `GET /student/selections` | 已选 / 已退课程列表 | 支持 `semesterId`、`status` |
| 退课 | `DELETE /student/selections/{id}` | 退课（状态改 `WITHDRAWN`），有退课时间窗口 | 非本人 / 已截止会报错 |
| 课表 | `GET /student/timetable` | 由选课记录动态生成周课表（不独立建表） | 支持 `semesterId` |
| 学分统计 | `GET /student/credits` | 本学期已选 / 已修 / 上限学分 | — |
| 我的成绩 | `GET /student/scores` | 只返回**已发布**成绩 | 未发布看不到 |

### 教师端（teacher）

| 接口 | 方法 + 路径 | 干什么 | 关键说明 |
|---|---|---|---|
| 我的教学班 | `GET /teacher/teaching-classes` | 当前教师名下教学班列表 | 用登录教师身份查，不传 `teacherId` |
| 教学班详情 | `GET /teacher/teaching-classes/{id}` | 单个教学班详情 | 必须是本人教学班，否则 `40303` |
| 学生名单 | `GET /teacher/teaching-classes/{id}/students` | 该班已选学生（`SELECTED`） | 支持 `keyword` 搜学号 / 姓名 |
| 成绩列表 | `GET /teacher/teaching-classes/{id}/scores` | 该班成绩（含未发布） | — |
| 保存成绩 | `PUT /teacher/teaching-classes/{id}/scores/{selectionId}` | 录入 / 修改单条成绩 `{score}` | 0–100；发布后不可改 `40916` |
| 发布成绩 | `POST /teacher/teaching-classes/{id}/scores/publish` | 把该班未发布成绩批量改 `PUBLISHED` | 学生端随后可见 |

### 教务端（admin，基础数据 CRUD）

每个基础模块都提供标准五件套：`POST` 新增、`GET` 列表、`GET /{id}` 详情、`PUT /{id}` 修改、`PATCH /{id}/status` 启停。
路径前缀统一 `/api/v1/admin/`。

| 模块 | 路径 | 干什么 |
|---|---|---|
| 学生 | `/admin/students` | 学生账号 + 档案（联动 `sys_user`） |
| 教师 | `/admin/teachers` | 教师账号 + 档案 |
| 专业 | `/admin/majors` | 专业字典 |
| 年级 | `/admin/grades` | 年级字典 |
| 行政班 | `/admin/academic-classes` | 行政班（绑定专业 / 年级） |
| 教室 | `/admin/classrooms` | 教室资源 |
| 学期 | `/admin/semesters` | 学期（含最大学分上限） |
| 课程 | `/admin/courses` | 课程字典 |
| 教学班 | `/admin/teaching-classes` | 教学班 + 关联专业 / 年级 |
| 排课 | `/admin/teaching-classes/{id}/schedules` | 教学班排课，带教师 / 教室冲突校验 |
| 选课批次 | `/admin/selection-batches` | 选课时间段 + 可退课截止 |
| 批次开课 | `/admin/selection-batches/{id}/teaching-classes` | 把教学班挂到批次，决定学生能否选 |

### V1.1 业务增强接口

| 接口 | 方法 + 路径 | 干什么 | 权限 |
|---|---|---|---|
| 加入候补 | `POST /student/teaching-classes/{teachingClassId}/waitlist` | 满员时进候补队列 | STUDENT |
| 取消候补 | `DELETE /student/waitlist/{selectionId}` | 退出候补 | STUDENT |
| 候补列表 | `GET /student/waitlist` | 我的候补 + 实时位次 | STUDENT |
| 候补递补 | `POST /admin/teaching-classes/{teachingClassId}/waitlist/promote` | 教务手动递补 | ADMIN |
| 通知列表 | `GET /notifications` | 分页查我的通知 | 任意登录角色 |
| 未读数量 | `GET /notifications/unread-count` | 红点用 | 任意登录角色 |
| 标记已读 | `PUT /notifications/{id}/read` | 单条已读 | 本人 |
| 全部已读 | `PUT /notifications/read-all` | 一键清 | 本人 |
| 导入学生 | `POST /admin/students/import` | xlsx 批量导入（逐行隔离） | ADMIN |
| 学生模板 | `GET /admin/students/import-template` | 下载空模板 | ADMIN |
| 导出学生 | `GET /admin/students/export` | 导出 xlsx | ADMIN |
| 教师导入 / 模板 / 导出 | `/admin/teachers/import` 等 | 同上，教师版 | ADMIN |
| 名单导出 | `GET /teacher/teaching-classes/{id}/students/export` | 教师导自己班的名单 | TEACHER |
| 成绩模板 | `GET /teacher/teaching-classes/{id}/scores/import-template` | 预置学号姓名的成绩模板 | TEACHER |
| 导入成绩 | `POST /teacher/teaching-classes/{id}/scores/import` | xlsx 批量录成绩 | TEACHER |
| 成绩统计 | `GET /teacher/teaching-classes/{id}/score-statistics` | 平均分 / 及格率 / 优秀率 / 分布 | TEACHER |

---

## 没有前端页面时如何测试调试

后端是纯 HTTP / JSON 接口，没有页面也能完整联调。按推荐程度排序：

### 方式一：Knife4j 在线调试页（最省事，零前端）

项目已集成 **Knife4j**（基于 springdoc-openapi），启动后自带可交互文档页，等于一个现成的前端：

- 文档主页：**`http://localhost:8080/doc.html`**
  （Swagger 原生页 `/swagger-ui.html`，OpenAPI JSON `/v3/api-docs`）
- 页面里每个接口都能直接填参数、**点「调试 / 试一下」**发真实请求并看响应。
- **鉴权**：点右上角 **Authorize**（或「文档管理 → 全局参数设置」），在 `Authorization` 值里填 `Bearer <token>`（注意 `Bearer` 后有空格）。设置后所有请求自动带 Token，不用逐个接口手填。

> 调试顺序：先调 `/auth/login` 拿 token → 复制 `data.token` → Authorize 粘贴 → 再调任意业务接口。

### 方式二：curl 命令行（适合远程服务器 / CI / 无浏览器）

`curl` 能覆盖全部接口，最适合在没有桌面的测试机上跑。下面是一段可直接复制的完整流程（在 Git Bash / WSL / macOS 终端执行；PowerShell 见文末提示）：

```bash
BASE=http://localhost:8080/api/v1

# 1) 登录拿 token（loginType 取 ADMIN / TEACHER / STUDENT）
TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"20260001","password":"123456","loginType":"STUDENT"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
echo "TOKEN=$TOKEN"

# 2) 带鉴权调业务接口（Authorization: Bearer 后有空格）
curl -s $BASE/student/teaching-classes \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool

# 3) 选课（只传 teachingClassId，身份由后端从 Token 取）
curl -s -X POST $BASE/student/selections \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"teachingClassId":100}' | python -m json.tool

# 4) 管理端 / 教师端：换对应角色账号重新登录拿另一个 token 即可
```

> 远程测试（dev 环境 MySQL / Redis / RocketMQ 在 `100.83.74.68`）把 `BASE` 改成
> `http://100.83.74.68:8080/api/v1` 即可，接口路径不变。

**文件上传类接口（导入）**用 `-F`：

```bash
curl -s -X POST $BASE/admin/students/import \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "file=@students.xlsx"
```

> **Windows PowerShell 提示**：`$TOKEN=$(...)` 语法不同，建议用 **Git Bash**（本机已装）或 **WSL** 跑上面的 bash 片段；
> 若必须用 PowerShell，用 `Invoke-RestMethod` 并手动把 token 拼进请求头。

### 方式三：Postman / Apifox（团队复用）

- 导入 OpenAPI 描述一键生成接口集合：地址填 `http://localhost:8080/v3/api-docs`。
- 配两个环境变量：`baseUrl`、`token`。
- 在 `/auth/login` 请求的 **Tests** 脚本里自动提取 token 写回环境变量，后续请求统一引用 `{{token}}`；
  切换角色只需用对应账号重新登录。

### 必须理解的鉴权机制

1. 除 `/auth/login` 外，**所有接口都要 `Authorization: Bearer <token>`**，否则返回 `40101 未登录`。
2. 当前用户身份（学生 / 教师）**由 Token 还原，接口不接收 `studentId` / `teacherId` 参数**——
   所以调试学生 / 教师接口时，必须用对应角色的账号登录，而不是在参数里塞 ID。
3. 角色不符返回 `40301`；想测学生接口用学生号登录，想测 admin 用 admin 登录（账号密码见上文「测试数据」）。

### 调试排错速查

| 现象 | 原因 / 处理 |
|---|---|
| `40101 未登录` | 没带 token 或 token 失效 → 重新 `/auth/login` 拿 token |
| `40301 无接口访问权限` | 角色不对 → 换对应角色账号登录 |
| `40405 教学班不存在` / `40912 当前不在选课时间` 等 | 业务校验未过 → 对照 `docs/接口清单.md` §4 错误码 |
| 启动后连不上（随机端口） | 必须固定端口：`mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080"`，基础路径 `http://localhost:8080/api/v1` |
| 调接口报 Redis 连接不上 | 登录态 / 容量 / 缓存依赖 Redis；`dev` 环境 Redis 在 `application-dev.yml` 的 `server-ip`（默认 `100.83.74.68`），本机没有需改 `server-ip` 或自建 |
| 没有 RocketMQ 起不来 | 把 `smart-university.mq.enabled` 置 `false` 即可正常启动，不影响接口调试 |
| 想看 SQL / 业务日志 | `application.yml` 把 `com.smart.university.mapper` 日志级别改 `debug` |

### 关于内置冒烟脚本

> 早期文档提到的 `target/smoke.py` / `target/smoke_admin.py` **并未随本仓库提交**（仅留存于远程测试机）。
> 日常联调用上面的 **Knife4j 页面** 或 **curl** 即可 100% 覆盖；若需要一份可重复的本地冒烟脚本，可基于上方 curl 流程生成。
