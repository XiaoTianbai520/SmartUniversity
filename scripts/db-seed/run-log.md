# 数据补数执行日志（SmartUniversity · edu_course_selection）

- 执行时间：2026-10-20 16:35 ~ 17:12
- 环境：MySQL 8.4.11 `100.83.74.68:3306` / Redis `100.83.74.68:6379`
- 备份：`target/db-backup/before-seed-20261020.sql`（16.4MB，保留未删）
- 约束遵守：未修改任何 `src/` 文件；脚本全部落在 `scripts/db-seed/`

## 脚本清单

| 脚本 | 作用 | 幂等性 |
|---|---|---|
| `01_cleanup.sql` | 任务#1：清脏数据、修学期、建 4 学期+批次、固定账号改名 | DELETE / INSERT..WHERE NOT EXISTS |
| `common.py` | mysql CLI 子进程封装（查询 TSV / 批量 INSERT） | - |
| `03_generate_classes.py` | 任务#2-1：clone 6 学期 ×370 教学班 + 范围 + 排课 + 批次关联 | 先删后插 |
| `04_generate_selections.py` | 任务#2-2：逐学期逐学生选课历史 | 先删后插 |
| `05_generate_scores.py` | 任务#3：成绩重算 + 异常样本 | DELETE 全表后重建 |
| `06_finalize_samples.py` | 任务#4：重算 selected_count + 巡检样本 | 幂等 UPDATE/DELETE |
| `06b_redis_flush.py` | 任务#4：清理 Redis 容量缓存（最小 RESP 客户端） | 幂等 |
| `07_run_api.sh` | 任务#5：快照刷新 / 数据巡检 / 学期归档（curl，逐次重新登录） | 幂等 |
| `07_run_api.py` | 同上的 urllib 版本（实际因连接复用挂起，未采用） | - |

## 表行数 before → after

| 表 | before | after cleanup | after all |
|---|---|---|---|
| semester | 8 | 11 | 11 |
| selection_batch | 8 | 9 | 9 |
| selection_batch_class | 1111 | 1106 | 3326 |
| course | 168 | 164 | 164 |
| teaching_class | 1112 | 1106 | 3326 |
| teaching_class_major | 4966 | 4927 | 15187 |
| teaching_class_grade | 5548 | 5530 | 16630 |
| teaching_class_schedule | 2216 | 2212 | 6648 |
| course_selection | 36577 | 36573 | 73627 |
| score | 25360 | 25358 | 65425 |
| student | 2001 | 2000 | 2000 |
| sys_user | 2183 | 2182 | 2182 |
| notification | 32931 | 20921 | 20921 |
| semester_archive | 6 | 3 | 24 |
| data_audit_issue | 1 | 0 | 83 |
| stat_semester_overview | 5 | 0 | 9 |
| stat_course_selection | 13 | 0 | 3326 |
| stat_credit_progress | 2023 | 0 | 18000 |
| stat_teacher_workload | 5 | 0 | 1389 |
| stat_classroom_usage | 72 | 0 | 540 |

## 任务#1 清理明细

- 删除 semester id=8(`111111`)、selection_batch 6/7(`1111`)/8、course 165~168、teaching_class 1107(`2027Java`)~1112(`c011`)、student 2001(`11111`)+sys_user 2182
- 连带删除：course_selection 4 条、score 5 条、notification 12010 条、统计快照全部清空
- 修正 semester 6/7 日期与状态为 PLANNED；semester 7 max_selection_credit 9.0 → 30.0
- 新增学期 id 9~12：2022-2023-1 / 2022-2023-2 / 2023-2024-1 / 2023-2024-2（FINISHED, 30 学分）
- 新增批次 id 9~12，start=学期+2周 08:00，end=学期结束-10天，status=ENDED
- 改名：20260001→张明远 / 20260002→李思远 / 20260003→王雨桐；teacher 1→张启明（账号密码未动）

## 任务#2 规模

- 6 个空学期各 370 个教学班（共 +2220），class_code 形如 `2022-2023-1-CS200-01`
- 热门课程（容量压至 45%）：course_id 23,29,30,43,48,87,103,105,116,128
- 新增选课 37054 条；SELECTED 34025 / WITHDRAWN 1870 / WAITING 1159
- 约束校验：major/grade 适用范围违规 0、单学期学分>30 的 0、重复(student,class) 0、候补无编号 0、退课无时间 0、选课时间越界 0

| 学期 | 教学班 | SELECTED | WAITING | WITHDRAWN | 成绩行 | 均分 |
|---|---|---|---|---|---|---|
| 2022-2023-1(9) | 370 | 2847 | 0 | 251 | 2847 | 74.2 |
| 2022-2023-2(10) | 370 | 2861 | 0 | 235 | 2861 | 74.3 |
| 2023-2024-1(11) | 370 | 5704 | 0 | 508 | 5704 | 74.4 |
| 2023-2024-2(12) | 370 | 5700 | 1 | 513 | 5700 | 74.2 |
| 2024-2025-1(1) | 370 | 8467 | 5 | 741 | 8467 | 74.4 |
| 2024-2025-2(2) | 370 | 8446 | 3 | 772 | 8446 | 74.2 |
| 2025-2026-1(3) | 372 | 9559 | 0 | 0 | 9559 | 74.5 |
| 2025-2026-2(4) | 364 | 11969 | 0 | 0 | 11964 | 74.3 |
| 2026-2027-1(5,当前) | 370 | 12756 | 350 | 1939 | 9877 | 74.1 |

## 任务#3 成绩

- 模型：`75.5 + 课程偏移[-12,12] + 学生能力N(0,5) + 12%困难-6 + 噪声N(0,7)`，clamp 0~100
- 全库实测（65425 条）：均值 **74.31**、标准差 **11.17**、及格率 **89.86%**、优秀率(≥85) **17.78%**
- 分段：0-59 **6636**，60-69 **16498**，70-79 **21890**，80-89 **14730**，90-100 **5671**
- 已结束 8 学期全部 PUBLISHED，published_at 落在学期结束后 0.2~6.5 天
- 当前学期 sem5：370 班中 PUBLISHED 55 / UNPUBLISHED 232 / 无成绩行 83（≈15%/63%/22%）
- 异常样本（已用 `/admin/analytics/score-anomalies` 验证命中）：
  - `739`(CS200-03) 均分 52.1 标准差 5.8 → **FAIL_RATE_HIGH**（pass 9.3% vs 基线 99.8%，dev -90.5）
  - `759`(SE204-01) 均分 93.1 标准差 2.9 → **SCORE_TOO_HIGH**（exc 100% vs 基线 68.8%）
  - `373`(2025-2026-2 CS200-01) 5 名学生（student 57/189/201/429/545）无 score 行 → **SCORE_MISSING**

## 任务#4 计数与巡检样本

- `selected_count` 全量重算，口径不一致残留 0
- 满班样本（capacity = selected）：`817`(57)、`857`(49)、`974`(47)
- 超容量样本（capacity = selected-3）：`737`(cap 12 / sel 15)
- 无排课样本：`800`、`935`（删除全部 schedule 行）
- 排课冲突样本：`738` 与 `740` 同 classroom=1、第 1-16 周、教师不同（5 / 108）
  - 初版落在 `weekday=6 第1-2节`；因配置 `days-per-week=5` 教室利用率只统计周一~周五，已修正为 **`weekday=3 第5-6节`**（schedule id 1475 / 1479）
  - 修正后 `SELECT COUNT(*) FROM teaching_class_schedule WHERE weekday>5` = **0**
  - 注意：classroom=1、weekday=3、第5-6节 在 sem5 内另有教学班 `803`（teacher 105），因此该时段形成 3 班共用同一教室（按 lead 要求保留）；audit 按 `(rule,biz_id=classroom)` 去重后只记 1 条 CLASSROOM 冲突
  - 重跑巡检后 SCHEDULE_CONFLICT 教师维度 34→35：因 740(teacher 108) 移到 weekday3 第5-6节后，与教学班 `876`（teacher 108、教室 6，同 weekday3 第5-6节）形成真实教师冲突
  - 重跑巡检（2026-10-20）分组：CAPACITY_ZERO 1、NO_SCHEDULE 2、SCHEDULE_CONFLICT 教室 46 / 教师 35，合计 **84**，`%周6%` 描述 **0 条**
- Redis 清理：删除 teaching-class/dashboard/stat 缓存 Key 共 **380** 个

## 任务#5 接口执行结果

- 9 个学期 `snapshots/refresh` 均 `successCount=5 / failCount=0`
  - 首次落库耗时 78~91s/学期（逐行 INSERT 走远程库）；重复调用 ~0.1s
- `DATA_AUDIT_TASK/run`：scanCount 370、successCount **84**、failCount 160、logId 4657（教师改派后终态 logId 4693，仍 84 条）
  - 落库 84 条：CAPACITY_ZERO 1、NO_SCHEDULE 2、SCHEDULE_CONFLICT(教室) **46**、SCHEDULE_CONFLICT(教师) **35**
  - failCount 160 为 `uk_audit_issue(rule_code,biz_id)` 去重导致（多对冲突共享同一教室/教师 biz_id）
- 学期归档：8 个已结束学期 × 3 类（SELECTION/SCORE/OVERVIEW）均 successCount=3，共 **24** 条
- 应用已关闭，8081 端口无 LISTENING

## 与预期不符 / 需关注

1. **新增选课 37054 条，而非 ~6 万**：因 sem3/4/5 已有数据按要求不动；且 2025级、2026级覆盖学期全部落在 sem3/4/5，新增为 0。全库选课总量 73627 条。人均约 8.4 门（目标学分 26~30，课程均学分 ~3.4）。
2. **当前学期 sem5 的 83 个未录入成绩教学班**会被成绩异常接口判为 `FAIL_RATE_HIGH`（scoredCount=0 被当作 0% 及格），导致 sem5 共 106 条异常。属检测口径问题，非数据错误；3 个目标样本均正确命中。
3. **快照首次刷新 78~91s/学期**：`saveStatCreditProgress` 逐行写入远程库（~761 行 × RTT）。重复刷新 ~0.1s。
4. `data_audit_issue` 的 failCount（158→160）为唯一键去重所致，不影响已落库的 **84** 条（教室46 / 教师35 / 无排课2 / 容量1）。

## 任务#6 收尾：教师端演示账号补课（2026-10-20 17:26）

- sem5 实际带班最多为 **6 个**（teacher 4/27/90/137 并列；team-lead 所述"7"来自跨学期 `stat_teacher_workload.class_count` 的历史最大值，sem5 内不存在 7 班教师）
- 从 teacher 90 名下改派：
  - `833`(PHY201-01) → teacher **1**（T10001 张启明），排课 d4 3-4 / d5 9-10
  - `1014`(CHIN205-02) → teacher **1**，排课 d1 3-4 / d1 7-8（与 833 无重叠）
  - `1045`(MED201-01) → teacher **2**（T10002 李老师），排课 d2 5-6 / d2 9-10（与 T10002 原 d1/d3/d5 排课无重叠）
- 未触碰 738/740/803、737/800/935/817/857/974 及 10 门热门课；改派后 teacher 1/2 无任何教师维度冲突（SQL 校验为空）
- 强制重刷 sem5 快照：先删 `stat_teacher_workload`/`stat_semester_overview` 当日行再刷新（因 `readOrComputeList` 对"当日已有快照"直接 hit、不重算）
  - `stat_teacher_workload` sem5：T10001 class_count=2 / 128 学时 / 34 人；T10002=4 / 256 / 167；teacher90=3 / 192 / 146
  - `stat_semester_overview` sem5：teacher_count 154→**155**，avg_teacher_workload=151.90
- 巡检终态（logId 4693）：84 条（教室46 / 教师35 / 无排课2 / 容量1），教师维度未因改派增加
- 接口实测：T10001 `/teacher/workload` classCount=2、`/teaching-classes`=2 条、`/score-warnings` total=2；T10002 `/teacher/workload` classCount=4、`/teaching-classes`=4 条

## 学分进度快照跨学期一致性（复核，未改动）

- `stat_credit_progress` 每个学生在 9 个学期的 `completed_credit` / `progress_rate` **完全相同**（例：student 1=21.3/14.20，2=19.6/13.10，3=20.9/13.90，在 sem 9/10/11/12 四行一致）
- 结论：快照累计已发布成绩时未按学期截断，导致各学期都等于"截至当前"的累计值。属代码口径问题，待 lead 决定是否改代码
- 注：`semester_id=17` 不存在（实际为 1,2,3,4,5,9,10,11,12）

## 任务#7 四项代码缺陷修复 + 退课补齐（2026-10-20 17:26~18:03）

### 代码修复（`mvn -o -B clean compile` BUILD SUCCESS）
| 缺陷 | 文件 | 关键改动 |
|---|---|---|
| M-01 剩余学期用年差 | `CreditProgressServiceImpl`（resolveRemainingTermCount 加 semesterId，新增 resolveEntrySemesterStart/resolveSemesterStart）、`GraduationRiskServiceImpl`（resolveRemainingSemesters 两个重载加 semesterId + 批量按年级预计算）、`SemesterMapper`（新增 getSemesterByAcademicYearAndTerm / countSemesterByStartDateBetween / listSemesterByIds） | 改为 `学制×2 − 已读学期数 + 1`，按 semester.start_date 实算；实测 2022级=1、2023级=2、2024级=4、2025级=6、2026级=8 |
| M-02 零成绩班误判 | `ScoreAnomalyServiceImpl:442` | `scoredCount==0` 时跳过 FAIL_RATE_HIGH / SCORE_TOO_HIGH，仅保留 SCORE_MISSING；score-anomalies sem5 total 106→**23**，739/759 仍在 |
| S-01 学分不按学期切片 | `CreditProgressServiceImpl`（accumulateCredit 增加 targetSemesterStart + semesterStartMap 过滤） | 只累计开课学期 start_date ≤ 目标学期的已发布成绩；2000 学生 `COUNT(DISTINCT progress_rate)` 均 >1（min 2 / avg 5.28） |
| M-03 巡检唯一键缺 biz_type | `DataAuditTask`（内存 dedup 加 biz_type）、`docs/V2.0_数据修复迁移.sql`（ALTER）、`docs/V1.2_V2.0_数据库增量设计.sql` | 唯一键改 (rule_code,biz_type,biz_id)；教室/教师不再撞号，巡检 84→**98**（教室 46→51、教师 35→44，教室 41 已落库） |
| **M-06 手动刷新快照是空操作** | `StatisticsSnapshotServiceImpl.java:271`（刷新前清理当日行）、`:309`（新增 `removeTodaySnapshot`） | 无 force 参数，`readOrComputeList` 命中「当日已有快照」直接返回不计算。改为刷新入口按请求的 statTypes 先删「该学期 + stat_date=今天」旧行再重算，只删今天、保留历史日期。实测同一 statType 连续两次刷新均重算（第二次 generated_at 变新、1.8s 而非 hit 的 ~0.05s） |

### 文档同步（3 份口径一致）
`docs/接口文档.md`（示例 + 新增口径说明）、`docs/V1.2_V2.0_接口清单.md:407`、`docs/V1.2_V2.0_架构设计与开发计划.md:544`，统一为 `剩余学期数 = 学制×2 − 已读学期数 + 1（按 semester.start_date 实算，最小 1）`。

### 退课补齐（`08_withdraw_backfill.sql`）
- sem3(id=3)：9559 SELECTED → 抽 620 条转 WITHDRAWN（6.49%）；sem4(id=4)：11969 → 抽 780 条（6.52%）
- 同步删除对应 score 1399 条（1 条本属 SCORE_MISSING 样本无录取行）；`withdrawn_at` 全部落在 [selected_at, drop_deadline] 内（越界 0）
- 学分影响：合计移除 4123.9 学分；受影响学生 1180 人，人均 3.49 学分；摊到全体 2000 人平均 2.06 学分（sem3 0.887 + sem4 1.175）
- 重算 sem3/sem4 `selected_count`，口径不一致 0

### 修复后重跑
- 9 学期快照全部 `successCount=5`（其中 sem3 因 token 过期补刷 1 次；sem5 因存在当日陈旧命中快照，清空后强制重算）
- 巡检 logId=4751：98 条（CAPACITY_ZERO 1 / NO_SCHEDULE 2 / SCHEDULE_CONFLICT 教室 51、教师 44），failCount 0
- 归档：8 学期 ×3 类，共 24 条
- `warnings/credit` sem5：HIGH **9** / MEDIUM 769 / LOW 1222（修前 HIGH 1351）
- 2022级 20220001 九学期 completed_credit 递增：25.8→53.1→74.0→100.8→125.3→146.5→164.6→182.7→185.6

### C 阶段（含 M-06）权威重跑 2026-10-20 18:05~18:28
- 前置：`DELETE stat_*`（5 表 × 9 学期）全部清零，`semester_archive` 保留 24 条
- 9 学期刷新全部 successCount=5（无重试；单次 106~200s，M-06 每次强制重算）
- 每学期 5 类齐备：overview=1、classroom=60、teacher=154~157、course_selection=364~372、credit_progress=2000
- 巡检 logId=4839：**98**（教室 51 / 教师 44 / 无排课 2 / 容量 1），failCount=0
- 归档 8 学期 ×3 = 24 条
- `score-anomalies` sem5：total **24**（FAIL_RATE_HIGH 10 + SCORE_TOO_HIGH 14），739/759 均在
- `remainingSemesters`：1/2/4/6/8；`warnings/credit`：HIGH **9** / MEDIUM 769 / LOW 1222
- 2000 学生跨学期 `COUNT(DISTINCT progress_rate)`：min 2、max 9、avg 5.28，全部 >1

### 退课时间越界修复（2026-10-20 18:35）
- 问题：04 生成的 6 个学期（1/2/9/10/11/12）有 **1184** 条 WITHDRAWN 的 `withdrawn_at` 晚于 batch.drop_deadline（最大值即生成时刻 2026-10-20 16:51）。
- 修法：`withdrawn_at = selected_at + rand[0, drop_deadline−selected_at)` 秒；若 `selected_at > drop_deadline` 则取 `drop_deadline − 1 天`。
- 修复前 over_deadline：sem1 301 / sem2 278 / sem9 97 / sem10 88 / sem11 215 / sem12 205 = **1184**。
- 修复后：全库 `withdrawn_at > drop_deadline` = **0**。
- 其中 **58** 条属「异常种子」（`selected_at` 本身晚于 drop_deadline，全部在 sem1）：先把 withdrawn_at 置 drop_deadline−1 天会破坏 `withdrawn_at >= selected_at`，故对这 58 条同时把 `selected_at` / `withdrawn_at` 归一化为 `drop_deadline − 1 天`，最终 `withdrawn_at < selected_at` 也 = **0**。
- sem3/sem4 的 1400 条未触碰（620/780，越界 0/0）。

### sem1 selected_at 越界收口（2026-10-20 18:45）
- 根因：04 生成时 sem1 有选课时间落在 drop_deadline 之后。全库 `selected_at > drop_deadline` 原有的 691 条 = 58 条退课（已归一化）+ **633 条 SELECTED**。
- 修法：这 633 条（全为 SELECTED）`selected_at = batch.start_time + rand[0, min(30天, drop_deadline−start_time))`；未改 status、未改 score、未改 stat_*。
- 结果：全库 `selected_at > drop_deadline` = 0、`selected_at < start_time` = 0；WITHDRAWN 两越界仍为 0。
- sem1 selected_at 范围：2024-09-16 08:08:15 ~ 2025-01-01 23:23:40，均在 [start_time, drop_deadline] 内。
- 旁证：score 行数 64029（非本人改动，18:21~18:25 有 3 条 tc=1014/sem5 的 UNPUBLISHED 成绩由外部接口录入）。

### QA 第 2 轮 5 项收口（2026-10-20 18:30~18:42）
- **A1** 删除脏成绩 score id 156211/156212/156213；`teaching_class_id=1014` 成绩行 = 0；sem5 异常 total 回到 **23**（FAIL 9 + TOO_HIGH 14）。
- **A2** 删除 2026级在 sem4（入学前）的选课 **2474** 条（SELECTED 2313 / WITHDRAWN 161）及其成绩 **2311** 条；重算 sem4 selected_count（不一致 0）；2026级现仅 sem5（3128 条）；`entry_year > 学期起始年` 违规 0。
- **A3** 满班样本：sem3 因无 30~60 区间班级（GE 100+ / 专业班 ≤29），取 3 个最大非满班 `366/328/350`(144/141/141)；sem4 取 `734/689/698`(60/57/57) → sem3/sem4 满班各 **3**；sem5 专项样本未动（57 个满班不变）。
- **B1** 巡检跨天修复：`DataAuditIssueMapper` 新增 `removeDataAuditIssueBySemester`，`DataAuditTask:244` 改为按学期全删（唯一键仍 `(rule_code,biz_type,biz_id)`）；连续两次 run 均 success=98/fail=0（logId 4872/4873）。
- **B2** 文档示例统一：`接口文档.md:6152`=4、`:6470`=8(2026级)；`接口清单.md:388`=4、`:796`=8(2026级)，两处列表项口径一致。
- 另：清理原始遗留 WAITING 脏行 `cs.id=36573` 的 `withdrawn_at`（置 NULL），使「全状态」口径四计数也全 0。
- 最终：score **61715**；stat 9/3326/18000/1390/540；四时间越界 0/0/0/0；`mvn clean compile` BUILD SUCCESS。
- 不重跑快照（withdraw_count 只看 status）。

### 8081 实例归属说明
- QA 看到的 PID 2712 / 18:05:22 是本人的 C 阶段实例（task Oqvks8），启动参数为「8 个 tasks false + data-audit=true」，已于 C 跑完后关闭；当前 8081 仅 TIME_WAIT、无 LISTENING。
- `promoted_at` 全表 0、WAITING 359 与生成后一致，证明 waitlist-scan 未在 C 期间改动选课状态。
