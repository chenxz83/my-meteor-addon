# 水晶光环+ (Crystal Aura Plus)

基于 **Meteor Client** 的 Minecraft **Fabric** 附加模组（Addon）。对 Meteor Client 自带的水晶光环（Crystal Aura）进行增强，并附带一整套 2b2t / 水晶 PVP 实用模块。

## 支持版本

| 目录 | Minecraft | Meteor Client |
|------|-----------|---------------|
| `crystal-aura-plus/` | **1.21.11** | 1.21.11 |
| `crystal-aura-plus-1.21.8/` | **1.21.8** | 1.21.8 |

## 模块列表

### Combat 战斗

| 模块 | 说明 |
|------|------|
| **水晶光环+ (Crystal Aura Plus)** | 增强版水晶光环，见下方功能清单 |
| **Auto Pearl（自动珍珠）** | 血量过低或坠落时自动扔末影珍珠逃生 |
| **Backstab Warning（背刺警报）** | 玩家从背后快速接近时警报 |
| **Crystal Incoming（水晶来袭预警）** | 附近有会伤到你的水晶时提前警报 |
| **Anti-Piston（防活塞陷阱）** | 检测并高亮附近的活塞床/活塞水晶陷阱 |

### Render 渲染

| 模块 | 说明 |
|------|------|
| **Crystal ESP** | 末地水晶穿墙高亮 |
| **Item ESP（物品高亮）** | 穿墙高亮贵重掉落物（图腾/水晶/金苹果等，可自定义列表） |
| **Explosion Preview（爆炸预览）** | 渲染附近水晶/床/重生锚/TNT 的爆炸伤害范围 |

### Misc 杂项

| 模块 | 说明 |
|------|------|
| **Kill Msg（击杀播报）** | 击杀时自动发送自定义消息（StarScript） |
| **Death Announcer（死亡播报）** | 任何玩家死亡都播报名字+坐标 |
| **Combat Log（战斗日志）** | 击杀/死亡/爆图腾/进出服记录到 `combat-log.txt` |
| **Auto Reply（自动回复）** | 聊天含关键词时自动回复（StarScript） |
| **Totem Alert（图腾警报）** | 图腾/水晶余量不足时提醒 |

### World 世界

| 模块 | 说明 |
|------|------|
| **AutoSign+（自动告示牌+）** | 放置告示牌自动写入自定义文本（StarScript，可显示日期时间） |

### Movement 移动

| 模块 | 说明 |
|------|------|
| **Highway Helper（高速助手）** | 前进时自动把方向修正到选定方向：X+/Z+/X-/Z-/X+Z+/X+Z-/X-Z+/X-Z- 共 8 个方向（按 Minecraft 坐标系与偏航角换算）；支持配合自动走路/平飞挂机（不用按 W）；可选自动避障：前方 N 格内有障碍物时自动按 A/D 绕开 |

### HUD

- **水晶光环+ 状态**：显示水晶数量、当前目标、击杀/爆图腾统计。

## 水晶光环+ 相比原版的增强

- 忽略好友 / 忽略自伤（总开关）
- 自动放置黑曜石（背包自动取用、独立范围/延迟/切回设置）
- 目标模式（Multi 全体 / Single 聚焦）+ 目标优先级（最近/最低血量/最高伤害）
- 智能放置（跳过无敌帧目标）
- 狂暴模式快捷键（一键：无视自伤+强制脸贴+最低伤害 1.5）
- 手动激活键（按住才攻击）
- 反蹲坑（目标头顶有方块自动脸贴）
- 爆图腾自我暂停、击杀/爆图腾播报
- 目标 3D 信息渲染（名字/血量/预计伤害）
- 全部设置中英双语说明

## 安装

1. 安装 Minecraft（对应版本）+ Fabric Loader
2. 安装对应版本的 Meteor Client
3. 把 `crystal-aura-plus-*.jar` 放入 `.minecraft/mods`
4. 启动游戏，模块出现在 Meteor GUI 的对应分类中

## 下载

- **GitHub Actions**：每次推送自动构建，到仓库的 Actions 页下载最新 jar
- **Releases**：推送 `v*` 标签自动发布

## 构建

```bash
./gradlew build
# 产物位于各项目的 build/libs/
```

## 作者

**chenxz_Minecraft**

## 许可

GPL-3.0（基于 Meteor Client 源码衍生）
