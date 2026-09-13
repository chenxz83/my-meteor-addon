# 水晶光环+ (Crystal Aura Plus)

基于 Meteor Client 的 Minecraft Fabric 附加模块（Addon）。对 Meteor Client 自带的**水晶光环 (Crystal Aura)** 进行增强，适用于 Minecraft **1.21.11**。

## 功能

模块名为 **水晶光环+**（内部 id：`crystal-aura-plus`），出现在 Meteor Client 的 **Combat** 分类中。它继承了原版 Crystal Aura 的全部能力，并额外新增：

- **目标模式 (Target Mode)**
  - `Multi`（默认）：对范围内所有目标计算伤害总和，与原版一致。
  - `Single`：只聚焦单个目标，按优先级选择目标后仅计算该目标的伤害。
- **目标优先级 (Target Priority)**（单目标模式下生效）
  - `Nearest`：优先选择最近的目标。
  - `LowestHealth`：优先选择血量最低的目标。
  - `MostDamage`：优先选择可造成最高伤害的目标。
- **智能放置 (Smart Place)**
  - 开启后，跳过处于无敌帧（hit invulnerability）的目标，避免在目标无法受伤时浪费水晶。
- **目标信息显示 (HUD)**
  - `show-damage`：在模块 HUD 信息中显示对最佳目标的预计伤害。
  - `show-health`：在模块 HUD 信息中显示最佳目标的当前血量。

## 构建

使用 Gradle Wrapper 构建：

```bash
./gradlew build
# Windows: gradlew.bat build
```

生成的 jar 位于 `build/libs/crystal-aura-plus-1.0.0.jar`。

## 安装

1. 安装 [Minecraft 1.21.11 + Fabric Loader](https://fabricmc.net/)。
2. 将 [Meteor Client 1.21.11](https://meteorclient.com/) 放入 `mods` 目录。
3. 将本模块的 jar（`crystal-aura-plus-1.0.0.jar`）也放入 `mods` 目录。
4. 启动游戏，在 Meteor Client 的 Combat 分类中找到 **水晶光环+**。

## 技术说明

- 依赖坐标：`meteordevelopment:meteor-client:1.21.11-SNAPSHOT`
- Minecraft 映射：Yarn `1.21.11+build.3`
- Fabric Loader：`0.18.2`
- Java：21
- 协议：GPL-3.0（基于 Meteor Client 源码衍生）

## 许可

本项目基于 Meteor Client（GPL-3.0）的 Crystal Aura 源码衍生，因此同样采用 GPL-3.0 协议。
