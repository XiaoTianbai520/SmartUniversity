@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem  M4 选课与候补 —— 一键导出 PNG（用于贴进 Word 文档）
rem  依赖：Java 8+ ；plantuml.jar
rem ============================================================

set "HERE=%~dp0"
set "JAR=%USERPROFILE%\.local\share\plantuml\plantuml.jar"
set "CONFIG=%HERE%..\plantuml.config.puml"

if not exist "%JAR%" (
  echo [错误] 未找到 plantuml.jar：%JAR%
  echo 请下载 https://github.com/plantuml/plantuml/releases/latest 的 plantuml-x.yyyy.z.jar
  echo 重命名为 plantuml.jar 后放到上述路径，或修改本脚本中的 JAR 变量。
  pause
  exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
  echo [错误] 未检测到 java，请先安装 JDK/JRE 并加入 PATH。
  pause
  exit /b 1
)

echo 正在导出 PNG ...
java -Djava.awt.headless=true -jar "%JAR%" -charset UTF-8 -tpng -failfast2 -configfile "%CONFIG%" -o export "%HERE%*.puml"

if errorlevel 1 (
  echo.
  echo [失败] 有图渲染出错，请查看上方报错（行号对应 .puml 文件）。
) else (
  echo.
  echo [完成] PNG 已输出到：%HERE%export\
)
pause
