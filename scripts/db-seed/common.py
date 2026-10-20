# -*- coding: utf-8 -*-
"""db-seed 公共工具：通过 mysql.exe 子进程执行 SQL / 读取 TSV。

不引入第三方依赖（环境无 pymysql 且无外网），全部走官方 mysql CLI。
"""
import subprocess
import os

MYSQL = r"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe"
HOST = "100.83.74.68"
PORT = "3306"
USER = "root"
PWD = "123456"
DB = "edu_course_selection"
BASE_ARGS = [
    "-h" + HOST, "-P" + PORT, "-u" + USER, "-p" + PWD,
    "-D", DB, "--default-character-set=utf8mb4",
]

_HERE = os.path.dirname(os.path.abspath(__file__))
GEN_DIR = os.path.join(_HERE, "_gen")


def run_sql(sql_text, echo_err=True):
    """执行任意 SQL 文本，返回 stdout 文本；失败抛异常。"""
    proc = subprocess.run(
        [MYSQL] + BASE_ARGS,
        input=sql_text.encode("utf-8"),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if proc.returncode != 0:
        err = proc.stderr.decode("utf-8", "replace")
        if echo_err:
            print("[SQL-ERROR]\n" + err[:4000])
        raise RuntimeError("mysql failed rc=%s" % proc.returncode)
    return proc.stdout.decode("utf-8", "replace")


def run_file(path):
    """执行一个 .sql 文件（UTF-8）。"""
    with open(path, "r", encoding="utf-8") as fh:
        return run_sql(fh.read())


def query(sql_text):
    """执行查询，返回 list[list[str|None]]；NULL -> None。"""
    proc = subprocess.run(
        [MYSQL, "-N", "-B"] + BASE_ARGS,
        input=sql_text.encode("utf-8"),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if proc.returncode != 0:
        err = proc.stderr.decode("utf-8", "replace")
        print("[QUERY-ERROR]\n" + err[:4000])
        raise RuntimeError("mysql query failed rc=%s" % proc.returncode)
    out = proc.stdout.decode("utf-8", "replace")
    rows = []
    for line in out.splitlines():   # 兼容 Windows CRLF
        if line == "":
            continue
        parts = line.split("\t")
        rows.append([None if p == "NULL" else p for p in parts])
    return rows


def query_ints(sql_text):
    """查询并把所有单元格转 int（用于 COUNT/ID 等）。"""
    return [[int(c) if c is not None else None for c in r] for r in query(sql_text)]


def sql_escape(value):
    """转义 SQL 字符串字面量内容。"""
    if value is None:
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def write_sql(filename, statements, header=""):
    """把语句列表拼成 SQL 文件写入 _gen 目录，返回文件路径。"""
    os.makedirs(GEN_DIR, exist_ok=True)
    path = os.path.join(GEN_DIR, filename)
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("SET NAMES utf8mb4;\n")
        fh.write("SET SESSION sql_mode = 'NO_AUTO_VALUE_ON_ZERO';\n")
        if header:
            fh.write("-- " + header.replace("\n", "\n-- ") + "\n")
        for stmt in statements:
            fh.write(stmt.rstrip())
            if not stmt.rstrip().endswith(";"):
                fh.write(";")
            fh.write("\n")
    return path


def batch_insert(table, columns, rows, chunk=500):
    """生成多行 INSERT 语句列表（每 chunk 行一条）。"""
    col_sql = ",".join("`%s`" % c for c in columns)
    stmts = []
    for i in range(0, len(rows), chunk):
        seg = rows[i:i + chunk]
        values = []
        for row in seg:
            vals = []
            for v in row:
                if v is None:
                    vals.append("NULL")
                elif isinstance(v, bool):
                    vals.append("1" if v else "0")
                elif isinstance(v, (int, float)):
                    vals.append(repr(v) if isinstance(v, float) else str(v))
                else:
                    vals.append(sql_escape(v))
            values.append("(" + ",".join(vals) + ")")
        stmts.append("INSERT INTO `%s` (%s) VALUES\n%s;" % (table, col_sql, ",\n".join(values)))
    return stmts


def table_counts():
    """返回当前关键表行数字典。"""
    tables = [
        "semester", "selection_batch", "selection_batch_class", "course",
        "teaching_class", "teaching_class_major", "teaching_class_grade",
        "teaching_class_schedule", "course_selection", "score", "student",
        "sys_user", "notification", "semester_archive", "data_audit_issue",
        "stat_semester_overview", "stat_course_selection", "stat_credit_progress",
        "stat_teacher_workload", "stat_classroom_usage",
    ]
    parts = ["SELECT '%s',COUNT(*) FROM `%s`" % (t, t) for t in tables]
    rows = query(" UNION ALL ".join(parts))
    return {r[0]: int(r[1]) for r in rows}
