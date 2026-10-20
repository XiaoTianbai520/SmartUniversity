# -*- coding: utf-8 -*-
"""
生成高校智慧教务选课平台真实规模测试数据（确定性，可复现）。
输出 db/seed-realistic.sql，清空旧数据后批量写入，并校正 selected_count。
保留 8 个固定测试账号（admin / T10001 / T10002 / 20260001~20260005，密码均为 123456）。
"""
import random
from datetime import date, datetime, timedelta

SEED = 20261019
random.seed(SEED)

OUT = "db/seed-realistic.sql"
PWD_HASH = "$2a$10$.0zTnnD6R7vfJ8guuui0f.o.mlMDxGOMK6SVha5PeoOavnKCjMGeK"  # 123456

# ---------------- 规模参数 ----------------
N_STUDENTS = 2000
N_TEACHERS = 180          # 含 T10001 / T10002
N_MAJORS = 24
GRADE_YEARS = [2022, 2023, 2024, 2025, 2026]
N_CLASSROOMS = 60
N_COURSES_PER_MAJOR = 6
N_PUBLIC_ELECTIVES = 20

# 生成有教学班的学期（最近 3 个学期）；其余仅作校历占位
CLASS_BEARING_SEMESTERS = ["2025-2026-1", "2025-2026-2", "2026-2027-1"]

# ---------------- 基础词库 ----------------
SURNAMES = list("王李张刘陈杨黄赵周吴徐孙朱马胡郭林何高梁郑罗宋谢唐韩冯于董萧程曹袁邓许傅沈曾彭吕苏卢蒋蔡贾丁魏薛叶阎余潘杜戴夏钟汪田任姜范方石姚谭廖邹熊金陆郝孔白崔康毛邱秦江史顾侯邵孟龙万段雷钱汤尹黎易常武乔贺赖龚文")
GIVEN = ["伟","芳","娜","秀英","敏","静","丽","强","磊","洋","艳","勇","军","杰","娟","涛","明","超","秀兰","霞","平","刚","桂英","文","辉","力","俊","晨","宇","浩","欣","悦","梓","涵","思","雨","佳","晓","婷","雪","倩","璐","航","睿","嘉","博","阳","宁","轩","琪","琳","彤","鑫","泽","瑞","颖","萌","冉","逸","帆","骏","彤","婧","昊","芊","沐","森","煦","越","灿","朗","曦","璇","澜","昂","峥","锦","淳","翊"]

def rand_name():
    return random.choice(SURNAMES) + random.choice(GIVEN) + (random.choice(GIVEN) if random.random() < 0.35 else "")

# ---------------- 专业与课程 ----------------
MAJORS = [
    ("CS",  "计算机科学与技术", ["计算机导论","程序设计基础","数据结构","计算机组成原理","操作系统","计算机网络"]),
    ("SE",  "软件工程",         ["软件工程导论","面向对象程序设计","软件测试","Web开发技术","软件项目管理","人机交互"]),
    ("AI",  "人工智能",         ["人工智能导论","机器学习","深度学习","自然语言处理","计算机视觉","知识工程"]),
    ("DS",  "数据科学与大数据", ["大数据导论","数据挖掘","数据库系统","分布式计算","数据可视化","统计学习"]),
    ("SEC", "网络空间安全",     ["网络安全导论","密码学","网络攻防","操作系统安全","应用安全","信息论"]),
    ("MATH","数学与应用数学",   ["数学分析","高等代数","解析几何","概率论","常微分方程","抽象代数"]),
    ("STAT","统计学",           ["统计学导论","数理统计","回归分析","多元统计","时间序列","贝叶斯统计"]),
    ("PHY", "物理学",           ["力学","电磁学","量子力学","热力学与统计物理","光学","原子物理"]),
    ("EE",  "电子信息工程",     ["电路分析","模拟电子技术","数字电子技术","信号与系统","数字信号处理","通信原理"]),
    ("COMM","通信工程",         ["通信原理","移动通信","光纤通信","卫星通信","信息论基础","嵌入式系统"]),
    ("AUTO","自动化",           ["自动控制原理","现代控制理论","过程控制","电机与拖动","传感器技术","机器人学"]),
    ("ME",  "机械工程",         ["机械制图","理论力学","材料力学","机械原理","机械制造基础","液压传动"]),
    ("MAT", "材料科学与工程",   ["材料科学基础","材料物理","材料化学","金属材料学","高分子材料","材料测试技术"]),
    ("CHE", "化学工程与工艺",   ["化工原理","物理化学","有机化学","化学反应工程","分离工程","化工热力学"]),
    ("BME", "生物医学工程",     ["生物医学导论","生物力学","医学影像","生物材料","生理信息系统","医疗器械"]),
    ("ECON","经济学",           ["微观经济学","宏观经济学","计量经济学","政治经济学","产业经济学","国际经济学"]),
    ("FIN", "金融学",           ["金融学基础","公司金融","投资学","金融工程","商业银行学","证券投资"]),
    ("ACCT","会计学",           ["基础会计","中级财务会计","审计学","管理会计","税法","会计电算化"]),
    ("BUS", "工商管理",         ["管理学原理","组织行为学","战略管理","市场营销","人力资源管理","运营管理"]),
    ("LAW", "法学",             ["法理学","宪法学","民法学","刑法学","民事诉讼法","国际法"]),
    ("CHIN","汉语言文学",       ["文学概论","中国古代文学","中国现代文学","外国文学","语言学概论","写作学"]),
    ("ENGL","英语",             ["综合英语","英语口语","英语写作","英美文学","翻译理论与实践","语言学"]),
    ("JOURN","新闻学",          ["新闻学概论","新闻采访","新闻写作","传播学","新媒体运营","媒介伦理"]),
    ("MED", "临床医学",         ["人体解剖学","生理学","病理学","药理学","内科学","外科学"]),
]
assert len(MAJORS) == N_MAJORS

PUBLIC_ELECTIVES = [
    "心理健康", "创新创业基础", "中国近现代史纲要", "体育", "音乐鉴赏", "美术鉴赏",
    "军事理论", "大学生职业规划", "环境保护与可持续发展", "人工智能通识",
    "传统文化概论", "社交礼仪", "演讲与口才", "电影鉴赏", "心理学导论", "经济学通识",
    "法律基础", "逻辑思维", "数字素养", "跨文化交流",
]

TITLES = ["助教", "讲师", "副教授", "教授"]

# 时间槽：星期1-5 × (1-2,3-4,5-6,7-8,9-10)
SLOTS = []
for wd in range(1, 6):
    for (s, e) in [(1, 2), (3, 4), (5, 6), (7, 8), (9, 10)]:
        SLOTS.append((wd, s, e))
# 每门课占用 2 个不相邻槽
def pick_slots():
    a, b = random.sample(SLOTS, 2)
    return a, b

def slot_pairs(a, b):
    return {(a[0], s) for s in range(a[1], a[2] + 1)} | {(b[0], s) for s in range(b[1], b[2] + 1)}

def sql_str(s):
    return "'" + str(s).replace("\\", "\\\\").replace("'", "''") + "'"

# ---------------- 构建实体 ----------------
# sys_user
sys_users = []          # (id, username, role)
next_uid = 1

def add_user(username, role):
    global next_uid
    uid = next_uid
    next_uid += 1
    sys_users.append((uid, username, role))
    return uid

# 8 个固定账号
admin_uid = add_user("admin", "ADMIN")
t10001_uid = add_user("T10001", "TEACHER")
t10002_uid = add_user("T10002", "TEACHER")
known_stu_uids = [add_user(f"2026000{i}", "STUDENT") for i in range(1, 6)]

# major
majors = []  # (id, code, name)
for i, (code, name, _courses) in enumerate(MAJORS, start=1):
    majors.append((i, code, name))
major_id_by_code = {code: mid for (mid, code, _n) in majors}

# grade_cohort
grades = []  # (id, entry_year, name)
for i, y in enumerate(GRADE_YEARS, start=1):
    grades.append((i, y, f"{y}级"))
grade_id_by_year = {y: gid for (gid, y, _n) in grades}

# academic_class: 每个 (major,grade) 1~2 个班
classes = []  # (id, class_code, class_name, major_id, grade_id)
cls_id = 1
class_of = {}  # (major_id, grade_id) -> list of class_id
for (mid, code, mname) in majors:
    for (gid, y, gname) in grades:
        n = random.choice([1, 1, 2])
        for k in range(1, n + 1):
            code_i = f"{code}{y}{k:02d}"
            name_i = f"{mname}{y}级{k}班"
            classes.append((cls_id, code_i, name_i, mid, gid))
            class_of.setdefault((mid, gid), []).append(cls_id)
            cls_id += 1

# classroom
classrooms = []  # (id, building, room_no, capacity)
buildings = ["第一教学楼", "第二教学楼", "第三教学楼", "实验楼", "图书馆报告厅"]
rid = 1
for b in buildings:
    cnt = N_CLASSROOMS // len(buildings) + (1 if rid <= N_CLASSROOMS % len(buildings) else 0)
    for j in range(1, cnt + 1):
        if rid > N_CLASSROOMS:
            break
        cap = random.choice([40, 50, 60, 80, 100, 120, 150, 200])
        classrooms.append((rid, b, f"{chr(64 + ((rid-1)//10)%5 +1)}{ (rid-1)%10 +1:02d}", cap))
        rid += 1
classrooms = classrooms[:N_CLASSROOMS]

# semester
semesters = []  # (id, code, year, term, start, end, max_credit, status)
sem_def = [
    ("2024-2025-1", "2024-2025", 1, date(2024, 9, 1),  date(2025, 1, 20), "FINISHED"),
    ("2024-2025-2", "2024-2025", 2, date(2025, 2, 24), date(2025, 7, 10), "FINISHED"),
    ("2025-2026-1", "2025-2026", 1, date(2025, 9, 1),  date(2026, 1, 20), "FINISHED"),
    ("2025-2026-2", "2025-2026", 2, date(2026, 2, 23), date(2026, 7, 10), "FINISHED"),
    ("2026-2027-1", "2026-2027", 1, date(2026, 9, 1),  date(2027, 1, 20), "ACTIVE"),
    ("2026-2027-2", "2026-2027", 2, date(2027, 2, 22), date(2027, 7, 10), "PLANNED"),
]
for i, (code, yr, term, st, en, status) in enumerate(sem_def, start=1):
    semesters.append((i, code, yr, term, st, en, 30.0, status))
sem_id_by_code = {code: sid for (sid, code, *_r) in semesters}

# teacher
teachers = []  # (id, user_id, teacher_no, name, title)
teacher_id = 1
# 已知
teachers.append((teacher_id, t10001_uid, "T10001", "张老师", "副教授")); teacher_id += 1
teachers.append((teacher_id, t10002_uid, "T10002", "李老师", "讲师")); teacher_id += 1
for k in range(N_TEACHERS - 2):
    no = f"T{10003 + k}"
    teachers.append((teacher_id, add_user(no, "TEACHER"), no, rand_name(), random.choice(TITLES)))
    teacher_id += 1

# student
students = []  # (id, user_id, student_no, name, major_id, grade_id, class_id)
student_id = 1
# 已知 5 个：软件/计算机，2026 级
known_plan = [
    (1, 2026, "张三"), (1, 2026, "李四"), (1, 2026, "王五"),
    (2, 2026, "赵敏"), (2, 2026, "孙悦"),
]
for idx, (mid, y, nm) in enumerate(known_plan):
    gid = grade_id_by_year[y]
    clid = class_of[(mid, gid)][0]
    no = f"2026000{idx+1}"
    students.append((student_id, known_stu_uids[idx], no, nm, mid, gid, clid))
    student_id += 1
# 生成其余（student_no 按入学年全局计数，保证唯一且不撞已知 2026000x）
year_seq = {y: 0 for y in GRADE_YEARS}
year_seq[2026] = 5  # 已知账号占用了 20260001~20260005
gen_count = N_STUDENTS - 5
for _ in range(gen_count):
    mid = random.randint(1, N_MAJORS)
    y = random.choice(GRADE_YEARS)
    gid = grade_id_by_year[y]
    clid = random.choice(class_of[(mid, gid)])
    year_seq[y] += 1
    no = f"{y}{year_seq[y]:04d}"
    students.append((student_id, add_user(no, "STUDENT"), no, rand_name(), mid, gid, clid))
    student_id += 1

# 学生按 (major,grade) 索引，便于选课时快速取
stu_by_mg = {}
for s in students:
    stu_by_mg.setdefault((s[4], s[5]), []).append(s)

# course
courses = []  # (id, code, name, credit, ctype, default_cap)
course_id = 1
course_of_major = {}  # major_id -> [course_id...]
for (mid, code, mname, clist) in [(i, m[0], m[1], m[2]) for i, m in enumerate(MAJORS, start=1)]:
    for ci, cname in enumerate(clist):
        ctype = "REQUIRED" if ci < 3 else ("MAJOR_ELECTIVE" if ci < 5 else "REQUIRED")
        credit = round(random.uniform(3.0, 5.0), 1) if ctype == "REQUIRED" else round(random.uniform(2.0, 3.0), 1)
        cap = random.choice([40, 50, 60, 80, 100, 120])
        ccode = f"{code}{200+ci}"
        courses.append((course_id, ccode, f"{mname}{cname}", credit, ctype, cap))
        course_of_major.setdefault(mid, []).append(course_id)
        course_id += 1
# 公共选修
for i, cname in enumerate(PUBLIC_ELECTIVES):
    courses.append((course_id, f"GE{i+1:03d}", cname, round(random.uniform(2.0, 3.0), 1), "PUBLIC_ELECTIVE", random.choice([80, 100, 120, 150])))
    course_id += 1

# teaching_class
teaching_classes = []  # (id, code, name, course_id, sem_id, teacher_id, capacity, status, majors_set, grades_set, slots)
tc_id = 1
tc_by_sem = {}
for sem in semesters:
    sid = sem[0]; scode = sem[1]
    if scode not in CLASS_BEARING_SEMESTERS:
        continue
    status = "AVAILABLE" if sem[7] == "ACTIVE" else "CLOSED"
    for c in courses:
        cid = c[0]; ctype = c[4]
        n_cls = random.choice([2, 3]) if ctype in ("REQUIRED", "PUBLIC_ELECTIVE") else random.choice([1, 2])
            # 开放范围
        if ctype == "PUBLIC_ELECTIVE":
            mset = list(range(1, N_MAJORS + 1))
        else:
            # 反查课程所属专业
            mid = next(k for (k, v) in course_of_major.items() if cid in v)
            mset = [mid]
            if random.random() < 0.4:
                m2 = random.choice([x for x in range(1, N_MAJORS + 1) if x != mid])
                mset.append(m2)
        gset = list(range(1, len(GRADE_YEARS) + 1))
        for k in range(1, n_cls + 1):
            cap = max(c[5], random.choice([40, 50, 60, 80, 100, 120]))
            tno = f"{scode}-{c[1]}-{k:02d}"
            tnm = f"{c[2]}{k}班"
            tch = random.choice(teachers)[0]
            a, b = pick_slots()
            teaching_classes.append((tc_id, tno, tnm, cid, sid, tch, cap, status, mset, gset, (a, b)))
            tc_by_sem.setdefault(sid, []).append(tc_id)
            tc_id += 1

# teaching_class_major / grade / schedule 派生
tc_major_rows = []
tc_grade_rows = []
tc_sched_rows = []
sched_id = 1
tc_cap_remaining = {}   # tc_id -> remaining SELECTED capacity
tc_waiting = {}         # tc_id -> current waitlist no
tc_slots = {}           # tc_id -> occupied (wd,sec) set
for tc in teaching_classes:
    tcid, _code, _nm, _c, _s, _t, cap, _st, mset, gset, (a, b) = tc
    for m in mset:
        tc_major_rows.append((tcid, m))
    for g in gset:
        tc_grade_rows.append((tcid, g))
    room = random.choice(classrooms)
    tc_sched_rows.append((sched_id, tcid, room[0], a[0], a[1], a[2], 1, 16)); sched_id += 1
    tc_sched_rows.append((sched_id, tcid, room[0], b[0], b[1], b[2], 1, 16)); sched_id += 1
    tc_cap_remaining[tcid] = cap
    tc_waiting[tcid] = 0
    tc_slots[tcid] = slot_pairs(a, b)

# selection_batch
batches = []  # (id, name, sem_id, start, end, drop, status)
batch_id = 1
batch_by_sem = {}
for sem in semesters:
    sid, scode, yr, term, st, en, _mc, status = sem
    if scode not in CLASS_BEARING_SEMESTERS:
        # 仍为该学期建一个批次（校历占位）
        bstatus = "NOT_STARTED"
    else:
        bstatus = "IN_PROGRESS" if status == "ACTIVE" else "ENDED"
    start = datetime(st.year, st.month, st.day, 8, 0, 0) + timedelta(days=15)
    end = datetime(en.year, en.month, max(en.day-10,1), 23, 59, 59)
    drop = datetime(en.year, en.month, max(en.day-20,1), 23, 59, 59)
    batches.append((batch_id, f"{scode} 选课批次", sid, start, end, drop, bstatus))
    batch_by_sem[sid] = batch_id
    batch_id += 1

# selection_batch_class
sbc_rows = []
for sem in semesters:
    sid = sem[0]
    if sid in tc_by_sem:
        for tcid in tc_by_sem[sid]:
            sbc_rows.append((batch_by_sem[sid], tcid))

# ---------------- 选课记录生成 ----------------
course_selection = []  # (id, student_id, tc_id, batch_id, status, waitlist_no, selected_at, promoted_at, withdrawn_at)
cs_id = 1
# 索引：学生 -> 可按学期选的教学班（major/grade 匹配）
def eligible_tc(student, sid):
    mid, gid = student[4], student[5]
    return [tcid for tcid in tc_by_sem.get(sid, [])
            if mid in tc_major_map[tcid] and gid in tc_grade_map[tcid]]

tc_major_map = {tc[0]: set(tc[8]) for tc in teaching_classes}
tc_grade_map = {tc[0]: set(tc[9]) for tc in teaching_classes}
_course_credit = {c[0]: c[3] for c in courses}
tc_credit = {tc[0]: _course_credit[tc[3]] for tc in teaching_classes}
tc_cap = {tc[0]: tc[6] for tc in teaching_classes}

sem_entry_order = {scode: i for i, scode in enumerate([s[1] for s in semesters])}

def gen_selections_for_semester(sid, is_active):
    global cs_id
    scode = [s[1] for s in semesters if s[0] == sid][0]
    batch = batch_by_sem[sid]
    sem_obj = [s for s in semesters if s[0] == sid][0]
    sem_start = datetime(sem_obj[4].year, sem_obj[4].month, sem_obj[4].day)
    for stu in students:
        mid = stu[4]
        gid = stu[5]
        y = [g[1] for g in grades if g[0] == gid][0]
        # 入学年必须 ≤ 学期起始年
        if y > sem_obj[4].year:
            continue
        elig = eligible_tc(stu, sid)
        if not elig:
            continue
        random.shuffle(elig)
        occupied = set()
        picked_ids = set()  # 该生本学期已占用的教学班（SELECTED/WITHDRAWN/WAITING 都算，唯一键约束）
        picked = 0
        credits = 0.0
        target = random.randint(5, 8) if is_active else random.randint(5, 7)
        for tcid in elig:
            if picked >= target:
                break
            if tc_cap_remaining[tcid] <= 0:
                continue
            if tc_slots[tcid] & occupied:
                continue
            if credits + tc_credit[tcid] > 30.0 + 1e-9:
                continue
            # 选上
            tc_cap_remaining[tcid] -= 1
            occupied |= tc_slots[tcid]
            credits += tc_credit[tcid]
            picked_ids.add(tcid)
            sel_at = sem_start + timedelta(days=random.randint(15, 60),
                                           hours=random.randint(8, 20), minutes=random.randint(0, 59))
            course_selection.append((cs_id, stu[0], tcid, batch, "SELECTED", None, sel_at, None, None))
            cs_id += 1
            picked += 1
        # 退课若干（仅 ACTIVE）：额外挑 0~2 门（可冲突/超容，但不得与已选重复）
        if is_active:
            n_drop = random.randint(0, 2)
            drop_pool = [t for t in elig if t not in picked_ids]
            random.shuffle(drop_pool)
            for tcid in drop_pool[:n_drop]:
                sel_at = sem_start + timedelta(days=random.randint(15, 50),
                                               hours=random.randint(8, 20))
                wd_at = sel_at + timedelta(days=random.randint(3, 20))
                course_selection.append((cs_id, stu[0], tcid, batch, "WITHDRAWN", None, sel_at, None, wd_at))
                cs_id += 1
                picked_ids.add(tcid)
            # 候补：满班教学班里挑 0~1 门（且不得与已选重复）
            if random.random() < 0.25:
                full = [t for t in elig if tc_cap_remaining[t] <= 0 and t not in picked_ids]
                if full:
                    tcid = random.choice(full)
                    tc_waiting[tcid] += 1
                    sel_at = sem_start + timedelta(days=random.randint(15, 50),
                                                   hours=random.randint(8, 20))
                    course_selection.append((cs_id, stu[0], tcid, batch, "WAITING", tc_waiting[tcid], sel_at, None, None))
                    cs_id += 1

for sem in semesters:
    sid = sem[0]
    if sem[1] in CLASS_BEARING_SEMESTERS:
        gen_selections_for_semester(sid, is_active=(sem[7] == "ACTIVE"))

# ---------------- 成绩生成 ----------------
scores = []  # (id, cs_id, value, status, published_at)
score_id = 1
for r in course_selection:
    rid, sid, tcid, batch, status, wln, sel_at, prom, wd = r
    if status == "SELECTED":
        # 找该教学班所属学期
        tc_sem = [t[4] for t in teaching_classes if t[0] == tcid][0]
        sem_status = [s[7] for s in semesters if s[0] == tc_sem][0]
        if sem_status == "ACTIVE":
            # 30% 已录入未发布
            if random.random() < 0.3:
                val = round(random.uniform(50, 100), 1)
                scores.append((score_id, rid, val, "UNPUBLISHED", None)); score_id += 1
        else:
            # 历史：已发布
            val = round(random.uniform(50, 100), 1)
            pub = sel_at + timedelta(days=random.randint(20, 60))
            scores.append((score_id, rid, val, "PUBLISHED", pub)); score_id += 1

# ---------------- 通知生成 ----------------
notifications = []  # (id, type, title, content, recv_uid, recv_role, biz_id, is_read, read_at)
notif_id = 1
# SELECTION_START：活跃批次给所有符合学生
active_sem = [s for s in semesters if s[7] == "ACTIVE"][0]
active_batch = batch_by_sem[active_sem[0]]
stu_uid_by_id = {s[0]: s[1] for s in students}
for stu in students:
    notifications.append((notif_id, "SELECTION_START",
                         f"{active_sem[1]}选课开始",
                         f"本学期选课已开启，请在截止前完成选课。",
                         stu[1], "STUDENT", active_batch, 0, None))
    notif_id += 1
# SCORE_PUBLISHED：历史已发布成绩的 40% 抽样
cs_map = {r[0]: r for r in course_selection}
sampled = [s for s in scores if s[3] == "PUBLISHED" and random.random() < 0.4]
for s in sampled:
    rid = s[1]
    stu_id = cs_map[rid][1]
    notifications.append((notif_id, "SCORE_PUBLISHED",
                         "成绩已发布",
                         "您有一门课程成绩已发布，请及时查看。",
                         stu_uid_by_id[stu_id], "STUDENT", rid, 0, None))
    notif_id += 1

# ---------------- 培养方案 ----------------
training_plans = []  # (id, name, major_id, grade_id, total, req, me, pe, status)
tp_id = 1
for (mid, code, mname) in majors:
    for (gid, y, gname) in grades:
        total = random.choice([150.0, 155.0, 160.0, 165.0, 170.0])
        req = round(total * random.uniform(0.5, 0.6), 1)
        me = round(total * random.uniform(0.2, 0.3), 1)
        pe = round(total - req - me, 1)
        training_plans.append((tp_id, f"{mname}{y}级培养方案", mid, gid, total, req, me, pe, 1))
        tp_id += 1

# ============================================================
# 写出 SQL
# ============================================================
lines = []
lines.append("-- ============================================================")
lines.append("-- 高校智慧教务选课平台 真实规模测试数据（确定性生成）")
lines.append(f"-- 生成时间基准 SEED={SEED}；保留 8 个固定测试账号（密码 123456）")
lines.append("-- ============================================================\n")
lines.append("USE `edu_course_selection`;")
lines.append("SET NAMES utf8mb4;")
lines.append("SET FOREIGN_KEY_CHECKS = 0;\n")

# 清空（逆外键序）
tables = ["notification","score","course_selection","selection_batch_class","selection_batch",
          "teaching_class_schedule","teaching_class_grade","teaching_class_major","teaching_class",
          "course","semester","classroom","teacher","student","academic_class","grade_cohort",
          "major","sys_user","training_plan"]
for t in tables:
    lines.append(f"DELETE FROM `{t}`;")
    lines.append(f"ALTER TABLE `{t}` AUTO_INCREMENT = 1;")
lines.append("")

def emit(table, cols, rows, batch=2000):
    if not rows:
        return
    col_sql = ", ".join(cols)
    i = 0
    n = len(rows)
    while i < n:
        chunk = rows[i:i+batch]
        vals = []
        for r in chunk:
            vals.append("(" + ", ".join(
                "NULL" if v is None else (str(v) if isinstance(v, (int, float)) else sql_str(v))
                for v in r) + ")")
        lines.append(f"INSERT INTO `{table}` ({col_sql}) VALUES\n" + ",\n".join(vals) + ";")
        i += batch

emit("sys_user", ["id","username","password_hash","role","status"],
     [(u[0], u[1], PWD_HASH, u[2], 1) for u in sys_users])
emit("major", ["id","major_code","major_name","status"],
     [(m[0], m[1], m[2], 1) for m in majors])
emit("grade_cohort", ["id","grade_name","entry_year","status"],
     [(g[0], g[2], g[1], 1) for g in grades])
emit("academic_class", ["id","class_code","class_name","major_id","grade_id","status"],
     [(c[0], c[1], c[2], c[3], c[4], 1) for c in classes])
emit("classroom", ["id","building_name","room_no","capacity","status"],
     [(c[0], c[1], c[2], c[3], 1) for c in classrooms])
emit("semester", ["id","semester_code","academic_year","term_no","start_date","end_date","max_selection_credit","status"],
     [(s[0], s[1], s[2], s[3], s[4].isoformat(), s[5].isoformat(), s[6], s[7]) for s in semesters])
emit("teacher", ["id","user_id","teacher_no","teacher_name","title","status"],
     [(t[0], t[1], t[2], t[3], t[4], 1) for t in teachers])
emit("student", ["id","user_id","student_no","student_name","major_id","grade_id","class_id","status"],
     [(s[0], s[1], s[2], s[3], s[4], s[5], s[6], 1) for s in students])
emit("course", ["id","course_code","course_name","description","credit","course_type","default_capacity","status"],
     [(c[0], c[1], c[2], c[2], c[3], c[4], c[5], 1) for c in courses])
emit("teaching_class", ["id","class_code","class_name","course_id","semester_id","teacher_id","capacity","status"],
     [(t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7]) for t in teaching_classes])
emit("teaching_class_major", ["teaching_class_id","major_id"], tc_major_rows)
emit("teaching_class_grade", ["teaching_class_id","grade_id"], tc_grade_rows)
emit("teaching_class_schedule", ["id","teaching_class_id","classroom_id","weekday","start_section","end_section","start_week","end_week"],
     [(s[0], s[1], s[2], s[3], s[4], s[5], s[6], s[7]) for s in tc_sched_rows])
emit("selection_batch", ["id","batch_name","semester_id","start_time","end_time","drop_deadline","status"],
     [(b[0], b[1], b[2], b[3].strftime("%Y-%m-%d %H:%M:%S"), b[4].strftime("%Y-%m-%d %H:%M:%S"), b[5].strftime("%Y-%m-%d %H:%M:%S"), b[6]) for b in batches])
emit("selection_batch_class", ["batch_id","teaching_class_id"], sbc_rows)
emit("course_selection", ["id","student_id","teaching_class_id","batch_id","status","waitlist_no","selected_at","promoted_at","withdrawn_at"],
     [(r[0], r[1], r[2], r[3], r[4], r[5],
       r[6].strftime("%Y-%m-%d %H:%M:%S") if r[6] else None,
       r[7].strftime("%Y-%m-%d %H:%M:%S") if r[7] else None,
       r[8].strftime("%Y-%m-%d %H:%M:%S") if r[8] else None) for r in course_selection])
emit("score", ["id","course_selection_id","score_value","status","published_at"],
     [(s[0], s[1], s[2], s[3], s[4].strftime("%Y-%m-%d %H:%M:%S") if s[4] else None) for s in scores])
emit("notification", ["id","notice_type","title","content","receiver_user_id","receiver_role","biz_id","is_read","read_at"],
     [(n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7], n[8]) for n in notifications])
emit("training_plan", ["id","plan_name","major_id","grade_id","total_credit","required_credit","major_elective_credit","public_elective_credit","status"],
     [(t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7], t[8]) for t in training_plans])

# 校正 selected_count（V1.2 并发互斥器）
lines.append("")
lines.append("-- 校正 teaching_class.selected_count 为真实 SELECTED 计数")
lines.append("""UPDATE `teaching_class` tc
SET tc.`selected_count` = (
    SELECT COUNT(*) FROM `course_selection` cs
    WHERE cs.`teaching_class_id` = tc.`id` AND cs.`status` = 'SELECTED')
WHERE EXISTS (
    SELECT 1 FROM `course_selection` cs2
    WHERE cs2.`teaching_class_id` = tc.`id` AND cs2.`status` = 'SELECTED');""")
lines.append("UPDATE `teaching_class` SET `selected_count` = 0 WHERE `selected_count` IS NULL;")
lines.append("")
lines.append("SET FOREIGN_KEY_CHECKS = 1;")

with open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(lines) + "\n")

# 汇总
print("=== 生成完成 ===")
print(f"sys_user         : {len(sys_users)}")
print(f"major            : {len(majors)}")
print(f"grade_cohort     : {len(grades)}")
print(f"academic_class   : {len(classes)}")
print(f"classroom        : {len(classrooms)}")
print(f"semester         : {len(semesters)}")
print(f"teacher          : {len(teachers)}")
print(f"student          : {len(students)}")
print(f"course           : {len(courses)}")
print(f"teaching_class   : {len(teaching_classes)}")
print(f"  _major rows    : {len(tc_major_rows)}")
print(f"  _grade rows    : {len(tc_grade_rows)}")
print(f"  _schedule rows : {len(tc_sched_rows)}")
print(f"selection_batch  : {len(batches)}")
print(f"  _class rows    : {len(sbc_rows)}")
print(f"course_selection : {len(course_selection)}")
sel = sum(1 for r in course_selection if r[4]=='SELECTED')
wd = sum(1 for r in course_selection if r[4]=='WITHDRAWN')
wt = sum(1 for r in course_selection if r[4]=='WAITING')
print(f"    SELECTED={sel} WITHDRAWN={wd} WAITING={wt}")
print(f"score            : {len(scores)}")
print(f"notification     : {len(notifications)}")
print(f"training_plan    : {len(training_plans)}")
print(f"输出文件: {OUT}")
