# 高校智慧教务选课平台（SmartUniversity）长期项目记忆

## 项目结构与文档位置

- 后端单体项目根目录：`C:\Users\c1338\Desktop\workplace\高校智慧教务选课平台\SmartUniversity`
- **文档统一在 `docs/` 单目录**（2026-09-28 已把早期单数 `doc/` 的全部内容 git mv 进去，`doc/` 不再存在）。
- **接口契约唯一入口：`docs/接口文档.md`** —— 由 V1.0 接口文档与 V1.1 接口清单合并而成，2560+ 行。
  章节编号：§1~§21 为 V1.0 基础接口，§22~§25 为 V1.1 新增（候补/通知/导入导出/成绩统计），§26 之后为实现建议。
- 其他 V1.1 文档：`docs/V1.1_开发计划.md`（任务分解 + 文件归属矩阵 + 公共契约 + 验收）、
  `docs/V1.1_业务流程设计.md`、`docs/V1.1_数据库增量设计.sql`。
- V1.0 文档（已移入 `docs/`，文件名保留原样）：`高校智慧教务选课平台_需求文档.md`、`_版本开发规划.md`、
  `_V1业务流程设计.md`、`_V1_ER图与数据库模型设计.md`、`_V1_数据库建表.sql`。
- **外部旧源 `高校智慧教务选课平台_V1_接口文档.md`（在 `高校智慧教务选课平台/` 父目录，不在 `docs/`）已过时**：它是纯 V1.0 源，被 `docs/接口文档.md` 取代。注意两处冲突——
  ① 章节编号错位：外部 §22~§28 = 事务边界/Controller/Service/DTO/优先级/总览/后续预留；接口清单 §22~§25 已被 V1.1（候补/通知/导入导出/成绩统计）占用，建议类挪到 §26~§32。
  ② 外部 §28「后续预留」里的候补路径 `/student/waitlists`、通知已读 `PATCH /notifications/{id}/read` 与实现不符——实际代码与接口清单 §22/§23 是 `POST /student/teaching-classes/{id}/waitlist`、`PUT /notifications/{id}/read`（另加 `unread-count`/`read-all`）。以 `docs/接口文档.md` + 代码为准。

## 技术栈与版本红线

- JDK 21 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.14 + MySQL 8.x + Redis + HuTool 5.8.29 + POI 5.4.1 + Knife4j 4.5.0
- **RocketMQ 已于 2026-10-16 移除**（提交 b627b2b / a2b65a5，含 pom 依赖、整个 `mq` 包、yml 配置与全量文档）。当前**无 MQ、无调度框架**（无 Quartz / XXL-Job），异步联动走 Spring 应用事件 + 定时兜底扫描。设计时不要再假设有 MQ。
- Spring Boot 3 必须用 `mybatis-plus-spring-boot3-starter`（`mybatis-plus-boot-starter` 只到 3.5.7 且给 Boot 2 用）
- MP 3.5.9 起 `PaginationInnerInterceptor` 拆到 `mybatis-plus-jsqlparser`，必须显式引入
- POI 用 5.4.1，本机 `~/.m2` 已离线可用，不需要联网

## 版本节奏

V1.0 核心闭环 → V1.1 业务增强（候补 / 通知 / 导入导出 / 成绩统计，已完成）→ V1.2 高并发（Redis + Lua）→ V2.0 智慧教务（数据分析）

**V1.2 与 V2.0 已规划完毕并决定并行推进（2026-10-16）**，三份规划文档：
- `docs/V1.2_V2.0_增量PRD.md`（PM 产出：V1.2 8 条 / V2.0 20 条需求池、7 个冲突点决议、18 个待拍板问题）
- `docs/V1.2_V2.0_架构设计与开发计划.md`（架构产出：15 章、7 阶段 26 子任务、文件归属矩阵、Redis Key / Lua / 快照 / 任务设计）
- `docs/V1.2_V2.0_数据库增量设计.sql`（9 张新表 + 2 项存量改造）+ `docs/V1.2_V2.0_接口清单.md`（28 个新接口，§34~§42）

**规划期确立的硬约束（后续开发必须遵守）**：
1. **C1 权威口径唯一**：「已选人数」= DB `COUNT(course_selection WHERE status='SELECTED')`；Redis 计数与新增 `teaching_class.selected_count` 冗余列**只作并发互斥器，统计严禁读**。
2. **排期**：T0 基础契约层（串行先行）→ W1∥W3 → W2∥W4∥W5 → T9 联调。
3. **T0 冻结制**：`RedisCommonConstant` / `RedisKeyUtil` / `ResultCodeEnum` / `application*.yml` 一次性交付后并行期禁改（只能末尾追加开关）；9 个已有 Mapper 的 default 方法契约由 T0 一次性交付后冻结。
4. `CourseSelectionServiceImpl` 由 V1.2（W1）独占，V2.0 候补兜底只能调 `WaitlistService#tryPromote`。
5. 新接口从 **§34** 起编号（§26~§33 已被"实现建议 / 接口总览 / 后续预留"等非业务章节占用）。

## 当前开发进度（2026-10-18 更新：T0/W1/W3/W4/W5 全部完成，已推送 GitHub）

- **五个工作流全部 ✅ 且已 push**：25 个 commit，推送区间 `0b12462..8c7a6ee`，远程 master = `8c7a6ee`（已 ls-remote 验证一致）。
  T0 4 个（bdbf457~3cd2158）、W3 6 个（d023925~93b998d）、W5 4 个（68a0a0d~b385b02）、
  W4 6 个（8583731~bdc6b66）、**W1 5 个（a03f4bf~8c7a6ee，V1.2 核心）**。Java 文件 209 → 334。
- **验证结论**：编译 BUILD SUCCESS；QA 六条红线全 PASS（`docs/V1.2_V2.0_阶段验证报告.md`）；
  冒烟 smoke.py 21 / smoke_admin.py 44 / 选课退课闭环 8 全 PASS（在 `redis-deduct-enabled=true` 下）。
- **W1 关键实现**：Lua 原子预扣 + DB 条件更新双保险；`executeRestore` 同时回补 Redis 与
  `selected_count`；每次预扣前 `alignSelectedCount` 对齐 DB 权威值；Redis 不可用走降级不抛 5xx。
  错误码已改号 40947~40949（原 40933~40935 撞 V1.1）。
- **剩余**：W2（降级/对账/100-500-1000 压测）未开始；T9 集成联调（补录 `docs/接口文档.md` §34 起）未开始。
- **9 项待裁决**：见 `docs/V1.2_V2.0_交接说明.md` §5。第 1 项是真实并发缺口 —— 递补路径
  `WaitlistServiceImpl:549` 只有 Lua 第一道防线，缺 `updateSelectedCountWithCapacityCheck`，
  存在超卖窗口（建议修法已写入交接文档，未擅自改，待 W2 压测验证）。
- **交接**：`docs/V1.2_V2.0_交接说明.md` 含进度、剩余任务、硬约束与两份可直接复制的交接提示词。

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

## 接口契约关键事实（与文档/代码冲突时以此为准）

- **§7.5 教学班成绩列表返回全量 `SELECTED` 学生**：已录入 + 未录入都会返回；未录入行 `score=null`、`status=null`，但**必带 `selectionId`**。首次调 §7.4 保存即创建成绩记录（UNPUBLISHED），重复保存覆盖。
  接口文档 §7.5 说明曾误写为「只返回已录入成绩的学生，未录入的不在列表」，已于 2026-10-14 在 `docs/接口文档.md` 更正。前端须渲染 `score=null` 的行，并以行内 `selectionId` 调 `PUT .../scores/{selectionId}` 补录/改成绩。

## 本机环境约束

- 无 MySQL / Redis / RocketMQ，**做不到运行时验证，只能过编译与静态审查**。
- `mvn -o -B clean compile -DskipTests` 离线可编译。缺依赖时挂代理：`-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890`（HTTPS 同理）。
- Bash 工具缺部分 coreutils（ls/mkdir 不可用，`find` 可用）；列目录优先用 Glob。
- 仓库里 `application*.yml` 长期存在用户自己未提交的改动（server-ip 按环境拆分），不要误回滚。
- **数据库 / Redis 连接地址：项目统一用 `100.83.74.68`（虚拟局域网地址，便于团队跨网络配合）**；
  `192.168.1.102` 是真实局域网地址，仅在走不了虚拟局域网时才用。以 `application-dev.yml` 为准，不要改。
  （用户 2026-10-18 明确指示；`CODEBUDDY.md` 里写的 192.168.1.102 已过时，不要据此"纠正"配置。）

## 性能优化约定（2026-10-20 新增，分析型/预警接口）

- **毕业风险 / 学分预警（§40.1 / §42.1）**：`GraduationRiskServiceImpl.pageGraduationRisk` 已改为读 `stat_credit_progress` 快照（`StatisticsSnapshotService.getCreditProgressBySemester`，命中当日快照 / 冷路径实时构建回写）。**严禁恢复逐学生 `getCreditProgressByStudentId` 的 N+1 跨学期扫描**。
- **成绩异常（§39.2 / §40.3）**：`ScoreAnomalyServiceImpl` 历史及格率基线已加"近 N 学期窗口过滤 + TTL 缓存"，**严禁恢复 `SEMESTER_SCAN_LIMIT=200` 全历史学期扫描**。
- **招生预测（§40.2）**：`EnrollmentForecastServiceImpl` 同样近学期窗口 + TTL 缓存；候补计数走批量 `countWaitingMapByTeachingClassIds`。
- **教学班列表已选人数**：`TeachingClassServiceImpl.convertToRespDTO` 用批量 `CourseSelectionMapper.countSelectedMapByTeachingClassIds`，**禁止逐班 `countSelectedByTeachingClassId`**。
- **教室推荐（§40.4）排课查询**：走批量 `TeachingClassScheduleMapper.listScheduleByClassroomIds`，禁止逐教室 `listScheduleByClassroomId`。
- 以上快照表 `stat_credit_progress` 由 `StatisticsSnapshotServiceImpl` 维护（readOrCompute 模式），冷路径首次请求会实时算并回写，之后命中当日快照即返回。
