# XRMChaJian · 玄锐暮插件

> 暮澜纪元 Minecraft MMORPG 服务端插件集 —— 技能 / 属性 / 天赋 / 任务 / 乐器 / 驭空术 / 组队，领域驱动设计的完整 MMO 玩法引擎；配套登录服插件与共享基础设施模块，全模块高度可测试。

[![Stars](https://img.shields.io/github/stars/XuanRuiMu/XRMChaJian?style=flat&logo=github)](https://github.com/XuanRuiMu/XRMChaJian/stargazers)
[![Forks](https://img.shields.io/github/forks/XuanRuiMu/XRMChaJian?style=flat&logo=github)](https://github.com/XuanRuiMu/XRMChaJian/forks)
[![Last Commit](https://img.shields.io/github/last-commit/XuanRuiMu/XRMChaJian)](https://github.com/XuanRuiMu/XRMChaJian/commits/main)
[![Issues](https://img.shields.io/github/issues/XuanRuiMu/XRMChaJian)](https://github.com/XuanRuiMu/XRMChaJian/issues)
[![Repo Size](https://img.shields.io/github/repo-size/XuanRuiMu/XRMChaJian)](https://github.com/XuanRuiMu/XRMChaJian)
[![Minecraft](https://img.shields.io/badge/Minecraft-Purpur%2026.2-brightgreen)](https://purpurmc.org/)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://www.oracle.com/java/)
[![Stack](https://img.shields.io/badge/stack-Gradle%20KotlinDSL%20%2B%20Guice%20%2B%20MySQL-blue)](https://github.com/XuanRuiMu/XRMChaJian)

---

## 这是什么？

**XRMChaJian（玄锐暮插件）** 是国产 MMORPG 服务器「**暮澜纪元**」的整套插件代码库。它不是一个"小玩具插件"，而是一套**领域驱动设计**的 MMO 玩法引擎：

```text
xrmm-root（根工程）
├── XRM        → MMORPG 核心玩法引擎（最大的模块）
├── XRMdenglu  → 登录服插件（职业选择 / 重生 / 传送 / 聊天管制）
└── xrm-common → 共享基础设施（翻译 / 配置 / 文本格式化，独立发布到本地 Maven）
```

核心设计理念：**领域层不依赖 Bukkit API**，全部业务以纯 Java 领域模型 + 服务接口表达，基础设施层再通过 Guice 装配并适配到 Minecraft——这让整个引擎可以被**单元测试直接测试**，而不是只能"上线试错"。

---

## XRM —— 核心玩法引擎

### 领域模型（`领域层/`）

| 子域 | 内容 |
| --- | --- |
| ⚔️ 战斗 | 伤害管线（多阶段处理器）、伤害上下文、战斗状态、角色类型 |
| 🎯 技能 | 技能定义 / 上下文、参数注册表 / 读取器、按键绑定、施法类型 |
| ✨ 效果 | 效果定义 / 实例 / 处理器（可叠加、可调度）|
| 📊 属性 | 属性注册表、修饰器、派生属性计算、快照 |
| 🌟 天赋 | 天赋图 / 节点 / 选择 / 类型（树状成长体系）|
| 📜 任务 | 任务定义 / 分类 / 目标 / 进度 / 奖励 |
| 🎵 乐器 | 完整音乐系统：音高方块映射、滑音 / 颤音、弯音调制、和弦预设、力度档、延音、量化、钢琴卷帘编辑器、MIDI / NBS 导入导出 |
| 🦅 驭空术 | 飞行状态 / 配置（坐骑式飞行玩法）|
| 👥 组队 | 队伍 / 组队操作结果 |
| 👤 玩家 | 玩家会话 / 快照 / 位置 |
| 💧 资源 | 资源类型 / 定义（血量、法力等派生资源）|

### 业务与表现层

- **业务层**：技能注册 / 释放 / 冷却、效果调度 / 注册、天赋管理、属性计算、组队、乐器、任务管理、目标选择、伤害计算、吸血、仇恨、驭空术、资源变更、公共冷却显示……接口 + 实现分离，方便替换；
- **表现层**：命令处理器（技能 / 属性 / 天赋 / 任务 / 组队 / 乐器 / 驭空术 / 生命条缩放 / 战斗日志 / 总菜单 / 管理）、事件监听器（技能按键、装备切换、移动打断、效果生命周期、反伤、岩浆免疫、末影人 / 猪灵 / 蜘蛛控制等）、计分板、BossBar；
- **基础设施层**：内存实现（测试友好）、YAML 配置 / 翻译加载、Bukkit 适配、数据库连接池（HikariCP + MySQL）、MIDI / NBS / mod 输入通道、NoteBlockAPI 适配、资源包管理器。

---

## XRMdenglu —— 登录服插件

登录服独立成插件，负责玩家进场到进游戏前的全部环节：

- 🎭 **职业选择 GUI**：职业（角色类型 / 世界维度）选择界面，含职业描述颜色规则；
- 📍 **重生点管理**：登录服 / 重生点配置、重生事件处理；
- 🌀 **传送管理**：传送门区域（坐标自动排序）、传送门监听、传送命令；
- 🛡️ **登录管制**：聊天阻止（进游戏前禁言）、冒险模式强制、玩家数据 / 数据库连接管理；
- 🌐 **翻译输出**：所有玩家可见文本走翻译文件。

---

## xrm-common —— 共享基础设施

- 🌐 **翻译服务**：YAML 翻译加载器、语言代码别名（zh / en）、服务端初始化；
- 📝 **文本域**：关键词解析器（技能名 / 关键词染色）、文本格式化器；
- 🧩 **配置**：YAML 配置加载器（类型安全加载）。

---

## 工程亮点

- **领域驱动分层**：`领域层 / 业务层 / 表现层 / 基础设施层` 四层严格分离，领域模型零 Bukkit 依赖；
- **Guice 依赖注入**：模块通过 Guice 装配，接口可自由替换为内存实现；
- **翻译文件驱动**：玩家可见文本全部走 `翻译/*.yml`，杜绝硬编码（见 `sync_manifest.py` 清单同步）；
- **零硬编码**：数值 / 配置 / 消息全部外置为 YAML；
- **测试天花板**：JUnit 5 + Mockito，3 个模块各配独立测试集，覆盖领域层到全流程文本输出（100+ 测试类）；
- **严格编译**：`-Xlint:all -Werror` 把警告当错误，`-Xlint:-deprecation` 拦截弃用 API。

---

## 环境要求

| 依赖 | 版本 |
| --- | --- |
| Minecraft 服务端 | Purpur / Paper（compileOnly `purpur-api:26.2.build.+`）|
| Java | 25（Gradle toolchain）|
| MySQL | 8.x（HikariCP 连接池）|
| 构建 | Gradle 9.x（Kotlin DSL）|

可选软依赖：ProtocolLib、EliteMobs、Citizens、NoteBlockAPI（软件运行时按需加载）。

---

## 构建与测试

```bash
# 构建全部模块并部署到服务端 plugins 目录
./gradlew build

# 运行测试（XRM / XRMdenglu 默认关闭跑测试，显式开启）
./gradlew -PrunTests=true test

# 快速运行单个测试类（ConsoleLauncher）
./gradlew -PrunTests=true runTests -PtestClass=com.example.SomeTest

# 安装 xrm-common 到本地 Maven（供 XRM/XRMdenglu 引用）
./gradlew -p xrm-common publishToMavenLocal
```

> 每个模块的 `gradle.properties` 使用日期版本号（`yyyy.M.d`）；`tasks.jar` 自动 fat-jar 打包运行时依赖。

---

## 项目结构

```text
XRMChaJian/
├── settings.gradle.kts         # 根工程：includeBuild XRM / XRMdenglu / xrm-common
├── xrm-common/                 # 共享：翻译 / 配置 / 文本（独立 Maven 库）
├── XRM/                        # MMORPG 核心引擎
│   └── src/main/java/mljy/
│       ├── 领域层/              # 战斗·技能·效果·属性·天赋·任务·乐器·驭空术·组队…
│       ├── 业务层/              # 服务接口 + 实现（技能释放·效果调度·伤害计算…）
│       ├── 表现层/              # 命令·监听器·计分板·BossBar
│       └── 基础设施层/          # 内存实现·YAML·数据库·Bukkit适配·乐器文件格式
├── XRMdenglu/                  # 登录服插件（职业选 GUI·重生·传送·管制）
│   └── src/main/java/暮澜纪元/
│       ├── 职业/ 配置/ 菜单/ 命令/ 监听器/ 数据/
└── 开发需求文档/               # 各模块的测试报告与开发文档（随模块内）
```

---

## 许可证

本仓库仅作项目开源展示。**Made with ❤️ —— 用工程化手法，做真正的 MMO 插件。**