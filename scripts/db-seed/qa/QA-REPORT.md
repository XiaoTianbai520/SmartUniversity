# QA 验收报告 · 数据库测试数据补全

- 验收人：Edward（QA）
- 验收时间：2026-10-20 17:19 ~ 17:35
- 对象：`edu_course_selection` @ 100.83.74.68:3306（MySQL 8.4.11）+ 12 个智能功能接口
- 校验脚本：`scripts/db-seed/qa/`（A1/A2_A8/B/B2/C/D 六个 SQL + api_probe/deep_probe/smoke_qa 三个 py）
- 只读声明：全程未执行任何 INSERT/UPDATE/DELETE；应用以逐任务关闭方式启动（见缺陷 M-05），验收前后行数完全一致

## 1. 结论

**PASS-with-issues** —— 数据本身干净且自洽（一致性 8 项全绿、分布有区分度、接口 12 项全部 HTTP 200），但上层存在 **3 个必修代码缺陷**，其中「毕业风险剩余学期把"年"当"学期"」导致 67.55% 学生被误判 HIGH（真实应为 0.2%），是最严重问题。

## 2. 验收表

| # | 检查项 | 预期 | 实测 | 判定 |
|---|---|---|---|---|
| A1 | 脏数据残留（全表正则扫描） | 0 | 指定脏记录全部为 0；正则命中项经逐条回溯均为合法数据（`20220111` 学号、`T10111` 工号、课程名「软件工程**软件测试**」「材料**测试**技术」） | **PASS** |
| A2 | `selected_count` = 实际 SELECTED 条数 | 0 不一致 | 3326 个教学班全量比对，**不一致 0 条**，max_diff=0 | **PASS** |
| A3 | `(student_id, teaching_class_id)` 重复 | 0 | 0 组 | **PASS** |
| A4 | SELECTED 满足 major/grade 范围 | 0 违规 | major 违规 0 / grade 违规 0；且无"无范围记录"的教学班 | **PASS** |
| A5 | 每生每学期学分 ≤ max_selection_credit(30) | 0 超限 | 0 条 | **PASS** |
| A6 | score 只挂 SELECTED | 0 挂非 SELECTED | 65425 条成绩 **100% 挂在 SELECTED**，WITHDRAWN/WAITING 挂 0；孤儿 score 0 | **PASS** |
| A7 | 学期时间轴自洽 | 无重叠/状态匹配/PLANNED 无班 | 相邻学期重叠 0 对；状态-日期矛盾 0 条；PLANNED(6/7) 教学班 0 | **PASS** |
| A8 | 外键悬空 | 0 | 9 类外键悬空全部为 0；选课批次与教学班学期错配 0 | **PASS** |
| B9 | 成绩总体分布与课程极差 | 有区分度 | 均值 74.31 / 标准差 11.17 / 及格率 89.86% / 优秀率 17.78%；五分段 6636/16498/21890/14730/5671；**课程均分极差 25.15**（低 62.48 人工智能机器学习 → 高 87.63 计算机组成原理），课程均值标准差 7.07 | **PASS** |
| B10 | 同课跨学期均分稳定 | 波动小 | ACCT200 84.95~89.46（Δ4.51）、ACCT201 67.35~71.20（Δ3.85）、ACCT202 77.67~85.44（Δ7.77），9 学期均覆盖 | **PASS** |
| B11 | credit_progress 不堆单桶 | 多桶分布 | sem5 学生口径：<30 → 794、30-60 → 348、60-80 → 137、80-100 → 721、>100 → 0 | **PASS** |
| B12 | 年级 × risk_level 分档 | 给出实测 | 2022级 0% HIGH / 2023级 32.32% / 2024级 96.39% / **2025级 100%** / **2026级 100%**；全体 HIGH 67.55%（1351/2000） | **PASS（数字如实，但成因是代码缺陷 M-01）** |
| B13 | 满班率 | 有梯队 | sem5：58/370 = **15.68%**；over_capacity 1（id=737，cap12/sel15）、capacity_zero 0；`stat_course_selection.full_flag` 逐学期与实算**完全一致** | **PASS** |
| B14 | 教室利用率 ≤100% 且有梯队 | max≤100 | sem5：min **0.00** / median **38.40** / max **70.40**；>100% 的 0 条；`usage_hours/total_hours` 与 `usage_rate` 换算一致 | **PASS** |
| B15 | 教师工作量梯度 | 有梯度、无 0 | sem5：min 64 / median 128 / max 384，0 工作量教师 **0**；9 学期均 154 人在册 | **PASS** |
| B16 | 巡检按 rule_code 分组 + 冲突真实性 | 46+34 | 实测 **84 条**（非 83）：CAPACITY_ZERO 1、NO_SCHEDULE 2、SCHEDULE_CONFLICT 教室 **46** + 教师 **35**（非 34）。抽样回溯全部真实：教室 28 周3 第9-10节有 828/916/936/998 四班完全重叠；教室 33 周3 第1-2节有 817/887/1015/1021；738 与 740 同为教室1 周3 第5-6节 1-16 周（日志写"周6"有误） | **PASS（条数与报告不符，见 M-04）** |
| C | 12 个接口实测 | 全通 | 全部 HTTP 200；除 3 个"参数缺失/越权"的预期内 4xx（5a/5c 缺 teachingClassId→40405、12e 跨教师→40303），其余 code=200 | **PASS** |
| C1 | 红线：统计禁读 `selected_count` | 不得读 | dashboard `selectionCount=12756` = 实际 SELECTED 12756；`StatisticsSnapshotServiceImpl` 全文无 `getSelectedCount/selected_count`，走 `countSelectedByBatchId`；9 个学期 `stat_semester_overview` 三口径与实算 **逐行相等** | **PASS（未破红线）** |
| D13 | 跑 `target/smoke.py` / `smoke_admin.py` | 全 PASS | **两个文件均不存在**（`target/` 下只有 classes/db-backup/generated-sources/maven-status；`src/test` 无任何测试类） | **FAIL（脚本缺失，无法执行）** |
| D13' | 替代回归（自建 `smoke_qa.py`，58 项） | 全 PASS | **58/58 PASS**（详见第 6 节） | **PASS** |
| E16 | 零成绩班误判 FAIL_RATE_HIGH | 复核 | **证实**：106 条异常中 **83 条 scoredCount=0（78.3%）全部被判 FAIL_RATE_HIGH**；真实 FAIL_RATE_HIGH 仅 92-83=**9** 条 | **缺陷成立** |
| E17 | 剩余学期数口径 | 复核 | **证实**：实测 remainingSemesters 2022级=1 / 2023级=1 / 2024级=2 / 2025级=3 / 2026级=4，**恰为真实值的一半**（应 2/2/4/6/8） | **缺陷成立** |

## 3. 必须修复项

### M-01【严重】毕业风险剩余学期数把"年"当"学期"，67.55% 学生被误判 HIGH

- **位置**：`CreditProgressServiceImpl.java:383`（`resolveRemainingTermCount`）、`GraduationRiskServiceImpl.java:453`（`resolveRemainingSemesters`），两处同一公式
  ```java
  int remaining = defaultSchoolYears - (LocalDate.now().getYear() - entryYear);  // 4 - 年差
  return remaining < 1 ? 1 : remaining;
  ```
- **问题**：`defaultSchoolYears=4`（application.yml:124），算出的是**年**数，却作为 `remainingSemesters` 直接除学分缺口（`CreditProgressServiceImpl.java:145-146`、`GraduationRiskServiceImpl.java:196/270`）。且完全忽略月份/`term_no`，大一上与大一下同值。
- **实测证据**（admin API，semesterId=5）：

  | 年级 | 实际剩余学期 | API 返回 | 偏差 |
  |---|---|---|---|
  | 2026级（大一上） | 8 | 4 | ×0.5 |
  | 2025级（大二上） | 6 | 3 | ×0.5 |
  | 2024级（大三上） | 4 | 2 | ×0.5 |
  | 2023级（大四上） | 2 | 1 | ×0.5 |
  | 2022级 | 0（已满） | 1（被 clamp） | 虚高 |

- **影响面**：`/admin/analytics/graduation-risks`（total=2000）、`/admin/warnings/credit`（total=2000）、`/student/graduation-risk`、`stat_credit_progress.risk_level`（18000 行快照全部污染）。
- **量化影响**（用同一份 sem5 快照按阈值 high>24、medium>18 反算）：
  - 现口径：HIGH **1351** / MEDIUM 83 / LOW 566
  - 修正后：HIGH **4** / MEDIUM 717 / LOW 1279
  - 即 **1347 条 HIGH 是假阳性（99.7%）**，学生端会看到「按当前进度，剩余学期需每学期修满 32.2 学分，达到学期上限的 107%」这类错误告警（2026级大一上学生实际只需 ~16 学分/学期）。
- **建议修法**：`remaining = max(1, 2*(defaultSchoolYears - (当前学年 - entryYear)) - 已过学期数)`，用 `semester.term_no`/当前学期推已过学期数，而非只比年份。

### M-02【严重】零成绩教学班被判"及格率过低"，成绩异常接口 78.3% 是假阳性

- **位置**：`ScoreAnomalyServiceImpl.java:435-445`
  ```java
  int scoredCount = scoreDOList.size();                       // 435，可为 0
  BigDecimal passRate = calculatePassRate(scoreValueList);    // 436
  ...
  if (passRate.compareTo(baselinePassRate.subtract(BigDecimal.valueOf(passRateDeviation))) < 0) {
      anomalyTypes.add(ANOMALY_FAIL_RATE_HIGH);               // 444
  }
  ```
  `calculatePassRate`（572-575）→ `calculateRate(n, 0)`（593-596）**分母 0 直接返回 0**，于是 0% < (基线-15) 恒成立。
- **实测**：sem5 有 83 个教学班有 SELECTED 但 0 条 score 行；`/admin/analytics/score-anomalies?semesterId=5` 返回 **total=106**，其中 `scoredCount=0` 的 **83 条**，anomalyTypes 全部为 `FAIL_RATE_HIGH`（例：id=755 `2026-2027-1-SE202-02`，selectedCount 23 / scoredCount 0 / passRate 0.0 / baseline 99.2 / deviation -99.2）。真实异常只剩：FAIL_RATE_HIGH **9** 条 + SCORE_TOO_HIGH **14** 条。
- **波及教师端**：`/teacher/score-warnings`（T10005）已返回 `scoredCount=0, passRate=0.0` 的记录，教师会看到自己"及格率 0%"的预警。
- **修法**：`scoredCount == 0` 直接跳过及格率/优秀率判定（只保留 SCORE_MISSING）。

### M-03【中】巡检唯一键缺 `biz_type`，教室与教师 ID 命名空间互撞，11% 冲突永久丢失

- **位置**：`data_audit_issue.uk_audit_issue(rule_code, biz_id)`（`SHOW INDEX FROM data_audit_issue` 实证）；落库循环 `DataAuditTask.java:241-252`
- **实测铁证**：

  | 量 | 值 |
  |---|---|
  | 真实教室冲突涉及教室数 | **51** |
  | 真实教师冲突涉及教师数 | **44** |
  | 两者 ID 重叠（教室 id 与教师 id 相撞） | **14** |
  | 并集 | **81** |
  | 实际落库 `SCHEDULE_CONFLICT` 行数 | **81** |

  三者严丝合缝：`51 + 44 - 14 = 81`。说明**每个 ID 只能存一条**，撞号的对侧被丢弃。
  漏报清单：教室 **41、53、13、59、47** 完全无记录；教师 **3、4、5、6、18、22、25、27、54** 完全无记录。
  可复现例：教室 41 在周5 第1-2节有 758 与 847 两班 1-16 周完全重叠 → `WHERE biz_type='CLASSROOM' AND biz_id=41` 返回 0 行。
- **附带**：`teacher_id=41` 在教师冲突集合中，其记录先落库，抢占了 `biz_id=41`。
- **修法**：唯一键改为 `(rule_code, biz_type, biz_id, stat_date)` 或给 `biz_id` 加 `biz_type` 前缀。

### M-04【中】验收报告口径错误：巡检 83 条实为 84 条，教师冲突 34 实为 35，冲突时段描述错误

- 实测 `SELECT COUNT(*) FROM data_audit_issue` = **84**（46+35+2+1），`run-log.md:101` 与团队结论写 83（46 教室 + **34** 教师）。
- 738/740 冲突位置是 **周3 第5-6节**，`run-log.md:93` 写成"weekday=6，第1-2节"。
- 影响：结论数字不可信，需更正后入档。

### M-05【中】`smart-university.schedule.enabled` 是死开关，关闭它不会停任何定时任务

- **位置**：`application.yml:92` 定义了 `schedule.enabled: true`，但 `grep -rn "schedule.enabled" src/main/java` **无任何命中**；9 个 Task 各自读 `smart-university.schedule.tasks.<name>.enabled`（默认 `true`）。
- **实证**：以 `--smart-university.schedule.enabled=false` 启动后，日志出现
  `DashboardCacheRefreshTask : 教务看板缓存刷新完成，学期ID：5`（`scheduling-1` 线程），说明任务照跑。
- **风险**：`batch-status`（60s）、`waitlist-scan`（300s）会**写库**（改 `selection_batch.status`、提升候补改 `course_selection.status`）。本次因窗口短 + 已核对 `selection_batch.updated_at` 未变而未造成污染，但这是踩到就会毁掉验收结论的雷。
- **修法**：要么给每个 `@Scheduled` 方法加 `@ConditionalOnProperty`/在方法内先判主开关，要么把 master 开关真正接进 `ScheduleTaskManageServiceImpl`。
- **本次应对**：改用 9 个 `--smart-university.schedule.tasks.<name>.enabled=false` 启动（见第 5 节启动命令）。

## 4. 建议项

| 编号 | 建议 | 依据 |
|---|---|---|
| S-01 | `stat_credit_progress` 9 个学期的 `progress_rate/completed_credit` **完全一样**，无历史演进 | 2000 名学生 `COUNT(DISTINCT progress_rate)` 全为 1；根因 `CreditProgressServiceImpl:112-127` 调 `listPublishedScoreByStudentId` 不过滤学期，`StatisticsSnapshotServiceImpl:320-350` 把同一份累计值盖了 9 个学期章。任何"学分进度趋势图"都会是一条直线。建议按 `semester.start_date` 过滤成绩 |
| S-02 | 演示教师账号 T10001（张启明）在**当前学期无任何教学班**（仅 sem4 有 3 个），教师端 3 个智能功能全空 | `/teacher/teaching-classes` 返回 `[]`、`/teacher/workload` → `classCount=0, classes=[]`、`/teacher/score-warnings` → `total=0`。演示路径会"看起来坏了"。建议给 teacher_id=1 在 sem5 补 1~2 个班 |
| S-03 | 3 名教师从未授课（id 136/148/164），177/180 有课 | `teacher` 180 人 vs `teaching_class.teacher_id` 177 人；这 3 人 `/teacher/workload` 永远 0 |
| S-04 | `classroom-recommendations` 缺 `teachingClassId` 时返回 `40405 教学班不存在`，语义误导（用户只传了时段） | `ClassroomRecommendServiceImpl:53-56` 无条件先查教学班；DTO 却提供 weekday/section/week 独立字段。建议参数校验返回 40001 并提示"teachingClassId 必填" |
| S-05 | 不存在的路由被包装成 `code=500` | 访问 `/api/v1/admin/archives`（实际不存在）返回 `500`（`NoResourceFoundException` 被 `GlobalExceptionHandler` 归入系统异常），应为 404 |
| S-06 | 学期归档只有 `POST /admin/semesters/{id}/archive`，无查询接口，24 条归档数据前端取不到 | `AdminSemesterArchiveController` 仅 1 个 `@PostMapping` |
| S-07 | sem3/sem4 的 WITHDRAWN=0（9559/11969 条选课零退课），与其它学期 6~7% 退课率不一致 | `stat_semester_overview` sem3/sem4 的 `withdraw_count=0` |
| S-08 | 优秀率阈值 60% 过松，`SCORE_TOO_HIGH` 14 条里有课程本身基线 100% 的（如 746：baseline 100、exc 88.9） | `application.yml` `excellent-rate-threshold: 60`；建议改为"相对基线偏离"而非绝对值 |

## 5. 接口实测原始摘要

启动命令（**逐任务关闭**，8080）：
```bash
mvn -o -B spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080 \
 --smart-university.schedule.tasks.batch-status.enabled=false \
 --smart-university.schedule.tasks.waitlist-scan.enabled=false \
 --smart-university.schedule.tasks.stat-snapshot.enabled=false \
 --smart-university.schedule.tasks.dashboard-refresh.enabled=false \
 --smart-university.schedule.tasks.credit-warning.enabled=false \
 --smart-university.schedule.tasks.data-audit.enabled=false \
 --smart-university.schedule.tasks.semester-archive.enabled=false \
 --smart-university.schedule.tasks.cache-reconcile.enabled=false \
 --smart-university.schedule.tasks.selection-cache-warmup.enabled=false"
```

| # | 接口 | HTTP | code | 条数 | 样例（截断） |
|---|---|---|---|---|---|
| 1 | `/admin/dashboard/overview?semesterId=5` | 200 | 200 | 单对象 | `{"semesterId":5,"semesterCode":"2026-2027-1","studentCount":2000,"selectedStudentCount":2000,"courseCount":164,"teachingClassCount":370,"teacherCount":154,"selectionCount":12756,"waitlistCount":350,"withdrawCount":1939}` |
| 2 | `/admin/analytics/graduation-risks?semesterId=5` | 200 | 200 | total=2000 | `{"studentNo":"20260049","gradeName":"2026级","riskLevel":"HIGH","riskReason":"按当前进度，剩余学期需每学期修满 34.6 学分，达到学期上限的 115%"}` |
| 3 | `/admin/analytics/enrollment-forecast?semesterId=5` | 200 | 200 | 20 | `{"courseCode":"GE018","courseName":"逻辑思维","currentCapacity":450,"selectedCount":450,"waitingCount":10,"historyAvgSelected":132.4,"forecastDemand":552,"suggestedCapacityIncrement":102,"fullRisk":true}` |
| 4 | `/admin/analytics/score-anomalies?semesterId=5` | 200 | 200 | **total=106** | `{"teachingClassId":739,"scoredCount":54,"passRate":9.3,"baselinePassRate":99.7,"deviation":-90.4,"anomalyTypes":["FAIL_RATE_HIGH"]}`；`{"teachingClassId":755,"scoredCount":0,"passRate":0.0,"anomalyTypes":["FAIL_RATE_HIGH"]}` ←假阳性 |
| 5a | `/admin/analytics/classroom-recommendations`（无参） | 200 | 40405 | data=null | `{"code":40405,"message":"教学班不存在"}` |
| 5b | `...?teachingClassId=738&topN=5` | 200 | 200 | 5 | `{"classroomId":4,"buildingName":"第一教学楼","roomNo":"A04","capacity":50,"capacityMargin":0,"reason":"容量冗余最小（0 个座位），与目标教学班同处第一教学楼"}` |
| 5c | `...?weekday=3&startSection=5&...`（无 teachingClassId） | 200 | 40405 | data=null | 同 5a |
| 6 | `/admin/statistics/classroom-usage?semesterId=5` | 200 | 200 | total=60 | `{"classroomId":49,"buildingName":"图书馆报告厅","roomNo":"E09","usageHours":704,"totalHours":1000,"usageRate":70.4}`；min 0 / median 38.40 / max 70.40 |
| 7 | `/admin/statistics/teacher-workload?semesterId=5` | 200 | 200 | total=154 | `{"teacherId":4,"teacherName":"马文煦","classCount":6,"totalHours":384,"studentCount":209}`；min 64 / median 128 / max 384 |
| 8 | `/admin/statistics/course-selection?semesterId=5` | 200 | 200 | total=370 | `{"classCode":"2026-2027-1-GE002-01","capacity":150,"selectedCount":150,"waitingCount":1,"withdrawCount":33,"fullFlag":1}` |
| 9 | `/admin/data-audit/issues` | 200 | 200 | **total=84** | `{"ruleCode":"CAPACITY_ZERO","bizType":"TEACHING_CLASS","bizId":737,"severity":"HIGH","description":"教学班【2026-2027-1-CS200-01】容量为 0 或小于已选人数 15"}` |
| 9b | `...?ruleCode=SCHEDULE_CONFLICT&bizType=CLASSROOM` | 200 | 200 | 46 | `{"bizId":1,"description":"教室【1】在周2 第1-2节存在排课冲突"}` |
| 9c | `...?ruleCode=SCHEDULE_CONFLICT&bizType=TEACHER` | 200 | 200 | **35** | — |
| 10 | `/admin/warnings/credit?semesterId=5` | 200 | 200 | total=2000 | 同 2（HIGH 1351 / MEDIUM 83 / LOW 566） |
| 11a | `/student/graduation-risk`（20260001 张明远） | 200 | 200 | 单对象 | `{"riskLevel":"HIGH","completedCredit":21.3,"planTotalCredit":150.0,"progressRate":14.2,"remainingCredit":128.7,"remainingSemesters":4,"requiredCreditPerSemester":32.2}` ← 应为 8/16.1 |
| 11b | `/student/credit-progress` | 200 | 200 | items=3 | `{"courseType":"REQUIRED","completedCredit":4.4,"requiredCredit":83.1,"gapCredit":78.7,"progressRate":5.3}` |
| 11c | `/student/credit-progress?semesterId=9` | 200 | 200 | items=3 | 与 11b 数值完全相同 → 证实 M-01/S-01 的历史口径问题 |
| 12a | `/teacher/workload`（T10005 韩睿超） | 200 | 200 | 单对象 | `{"teacherId":5,"classCount":5,"totalHours":320,"studentCount":74,"classes":[{"teachingClassId":738,...}]}` |
| 12b | `/teacher/score-warnings`（T10005） | 200 | 200 | total=2 | `{"teachingClassId":814,"scoredCount":0,"inputCompleteRate":0.0,"passRate":0.0}` ← M-02 波及教师端 |
| 12c | `/teacher/teaching-classes`（T10005） | 200 | 200 | list=5 | — |
| 12d | `/teacher/teaching-classes/738/score-statistics` | 200 | 200 | 单对象 | `{"selectedCount":22,"scoredCount":22,"unscoredCount":0,"averageScore":86.0,"maxScore":96.8}` |
| 12e | `/teacher/teaching-classes/759/score-statistics`（越权） | 200 | **40303** | data=null | `{"code":40303,"message":"无教学班操作权限"}` ← 权限校验正确 |
| 13a | `/teacher/workload`（T10001 张启明） | 200 | 200 | 单对象 | `{"teacherId":1,"classCount":0,"totalHours":0,"classes":[]}` ← S-02 |
| 13b | `/teacher/teaching-classes`（T10001） | 200 | 200 | list=**0** | ← S-02 |

**红线交叉核对（C1）**：
- `SELECT COUNT(*) FROM course_selection cs JOIN teaching_class tc ON tc.id=cs.teaching_class_id WHERE tc.semester_id=5 AND cs.status='SELECTED'` = **12756**，与 `overview.selectionCount` 完全一致；waitlist 350、withdraw 1939 亦一致。
- 代码侧：`DashboardServiceImpl:240-249` → `courseSelectionMapper.countSelectedByBatchId(...)`；`StatisticsSnapshotServiceImpl` 全文无 `selected_count`。**红线未被突破。**

## 6. 回归结果明细

`target/smoke.py`、`target/smoke_admin.py` **均不存在**（`glob **/smoke*.py` 无结果），D13 无法按要求执行。
替代方案：自建 `scripts/db-seed/qa/smoke_qa.py`（58 项，黑盒覆盖登录/鉴权/三角色核心只读链路），结果 **58/58 PASS**。

| 分组 | 项数 | 结果 | 关键点 |
|---|---|---|---|
| 登录与改名账号 | 9 | 全 PASS | admin 登录 OK；**20260001/02/03 显示名分别为 张明远/李思远/王雨桐**；**T10001 显示名 张启明**；密码未变 |
| 鉴权负向 | 2 | 全 PASS | 无 token→40101；学生 token 访管理端→40301 |
| 教务端基础数据 | 10 | 全 PASS | 学期 11 / 批次 9 / 课程 164 / 教学班 370(sem5) / 教室 / 教师 / 学生 / 专业 / 年级 / 定时任务 |
| 学生端链路 ×3 账号 | 27 | 全 PASS | 可选教学班、当前批次、我的选课、候补、成绩、课表、学分汇总、学分进度、毕业风险 |
| 教师端链路（T10005） | 8 | 全 PASS | 教学班 5 个、详情、学生名单、成绩列表、成绩统计、工作量、成绩预警 |
| 通知 | 1 | PASS | `/notifications` 列表 |

改名结论：3 个学生账号 + 1 个教师账号的改名**完全不影响登录与业务链路**。

## 7. 复验清单（给工程师改完后）

1. `GET /admin/analytics/graduation-risks?semesterId=5&gradeId=5` → `remainingSemesters` 应为 **8**，`riskLevel` 应为 LOW/MEDIUM
2. 各年级 riskLevel 的 HIGH 总数应从 1351 降到 ~4 量级
3. `score-anomalies?semesterId=5` total 应从 106 降到 ~23（9 + 14）
4. `data-audit/issues` 教室冲突应 ≥51、教师冲突应 ≥44，且教室 41 必须有记录
5. `stat_credit_progress` 同一学生 `COUNT(DISTINCT progress_rate)` 应 > 1
