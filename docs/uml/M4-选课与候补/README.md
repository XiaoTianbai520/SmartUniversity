# M4 选课与候补 · 建模图源文件

负责人：苏俊毅（04241618） ｜ 课程：软件建模技术 26-27-1
项目：高校智慧教务选课平台 ｜ 模块：选课与候补

## 文件清单（对应 9 项大作业）

| 文件 | 对应交付物 | 大作业 |
| --- | --- | --- |
| `01-用例图.puml` | 用例图 | 2 |
| `02-概念层类图.puml` | 分析模型（静态）：概念层类图 | 3 |
| `03-简单交互图-顺序图.puml` | 分析模型（动态）：简单交互图（基本路径） | 4 |
| `03-简单交互图-协作图.puml` | 分析模型（动态）：简单交互图（协作图） | 4 |
| `04-说明层类图.puml` | 设计模型（静态）：说明层类图 | 5 |
| `05-详细交互图-顺序图-选课并发.puml` | 设计模型（动态）：详细交互图（并发抢课） | 6 |
| `05-详细交互图-顺序图-退课递补.puml` | 设计模型（动态）：详细交互图（退课→递补） | 6 |
| `05-详细交互图-协作图.puml` | 设计模型（动态）：详细交互图（协作图，与选课并发顺序图等价） | 6 |
| `06-状态图.puml` | 状态图（对象：选课记录） | 7 |
| `07-活动图-选课校验链.puml` | 活动图（泳道：选课 13 步校验） | 7 |
| `07-活动图-退课与候补递补.puml` | 活动图（泳道：退课→候补递补） | 7 |
| `08-组件图.puml` | 实现模型：组件图 | 8 |
| `09-部署图.puml` | 实现模型：部署图 | 8 |

> 大作业9 集成打包时，上述图按「需求模型 / 分析模型 / 设计模型 / 实现模型」四组归并。

## 一键导出 PNG

双击 `render-png.bat`，PNG 输出到 `export\` 子目录，直接贴进 Word。

前置条件：

1. 已安装 Java（`java -version` 可用）；
2. `plantuml.jar` 位于 `%USERPROFILE%\.local\share\plantuml\plantuml.jar`
   （从 [PlantUML Releases](https://github.com/plantuml/plantuml/releases/latest) 下载 `plantuml-x.yyyy.z.jar` 后改名）。

## 手工渲染 / 单文件渲染

```bash
# 渲染单个文件
java -jar plantuml.jar -charset UTF-8 -tpng 01-用例图.puml

# 渲染本目录全部（输出到 export）
java -jar plantuml.jar -charset UTF-8 -tpng -configfile ../plantuml.config.puml -o export *.puml
```

VS Code 用户可装 `jebbs.plantuml` 插件，`Alt+D` 预览、右键导出 PNG。

## 渲染注意事项

- 本机**未安装 GraphViz**，因此类图 / 用例图 / 状态图 / 组件图 / 部署图里都带了 `!pragma layout smetana`
  （PlantUML 自带的纯 Java 布局引擎），**不要删除这一行**，否则这些图渲染会失败。
- 顺序图、活动图不依赖 GraphViz，无需该 pragma。
- 若中文渲染成方块，检查 `../plantuml.config.puml` 里的 `skinparam defaultFontName` 是否为本机存在的字体。

## 与其它模块的同步约定

- 概念层类图 ↔ 简单交互图（03）必须一致；
- 说明层类图（04）↔ 详细交互图（05）必须一致；
- 分析模型（02+03）↔ 设计模型（04+05+06+07）必须一致；
- 外部实体（学生 / 教学班 / 课程 / 通知）只在图中**引用**，属性以
  `docs/模块分工与建模任务清单.md` 第 2 节冻结的公共契约为准。
