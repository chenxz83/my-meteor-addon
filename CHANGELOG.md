# 更新日志 (Changelog)

## v1.0.0

### 模块（15 个）
- 水晶光环+（增强版 Crystal Aura）
- Crystal ESP（末地水晶高亮）
- Kill Msg（击杀播报，StarScript）
- AutoSign+（自动告示牌，StarScript）
- Totem Alert（图腾/水晶余量警报）
- Auto Pearl（自动珍珠）
- Item ESP（贵重掉落物高亮）
- Backstab Warning（背刺警报）
- Crystal Incoming（水晶来袭预警）
- Death Announcer（死亡播报）
- Combat Log（战斗日志）
- Auto Reply（自动回复，StarScript）
- Highway Helper（高速助手，8 方向可选：X+/Z+/X-/Z-/X+Z+/X+Z-/X-Z+/X-Z-，支持配合自动走路/平飞挂机，可选自动避障按 A/D 绕开）
- Anti-Piston（防活塞陷阱）
- Explosion Preview（爆炸范围预览）

### HUD
- 水晶光环+ 状态（水晶数量/目标/击杀统计）

### 其他
- 主界面作者名染色（插件名深蓝 0,0,170，作者名青色 §b，Mixin 实现）
- GitHub Actions 自动构建 + Releases（v* 标签）
- Meteor 更新检测（getRepo + commit.txt 注入 GITHUB_SHA）
- 全部设置中英双语说明
- 作者：chenxz_Minecraft
- 许可证：GPL-3.0

---

# 构建备忘 (Build Notes)

> ⚠️ 以后加新模块/功能时，务必同步更新 README.md 和本文件的模块列表，并 git commit。

## 项目结构
- `crystal-aura-plus/` = Minecraft **1.21.11** 版本
- `crystal-aura-plus-1.21.8/` = Minecraft **1.21.8** 版本
- 改完 1.21.11 后把 Java 文件复制到 1.21.8 项目，注意版本差异

## 两个版本的代码差异
- 1.21.11 yarn 用 `getEntityPos()`，1.21.8 yarn 用 `getPos()`（同步到 1.21.8 时要替换）

## 关键构建配置（不要乱动）
- Loom 插件必须用 `fabric-loom`（旧 ID），不能用 `net.fabricmc.fabric-loom`（否则 mappings 无法解析）
- loom = 1.14-SNAPSHOT，mappings 用 `mappings("net.fabricmc:yarn:<ver>:v2")` 字符串形式
- `enableTransitiveAccessWideners = false`（meteor-client 的 intermediary 访问加宽无法处理）
- 自有访问加宽文件：`crystal-aura-plus.accesswidener`（sendSequencedPacket + PlayerInteractEntityC2SPacket$InteractType）
- `modCompileOnly(libs.meteor.client)`（必须 mod 配置才能 remap intermediary→yarn）
- `compileOnly(libs.mixin)` = sponge-mixin 0.15.5+mixin.0.8.7
- Gradle 分发走腾讯镜像 `mirrors.cloud.tencent.com`（本机 github 被 SteamTools 劫持）
- Java 21（C:\Program Files\Zulu\zulu-21）

## 构建命令
```bash
$env:JAVA_HOME = 'C:\Program Files\Zulu\zulu-21'
$env:GRADLE_USER_HOME = 'D:\dsh-workspace\better-cpvp\.gradle-home'
./gradlew.bat build --no-daemon --console=plain
```

## GitHub 推送（本机 git 被 SteamTools 劫持 22 端口）
- 仓库：https://github.com/chenxz83/crystal-aura-plus
- 远程走 443 端口：`ssh://git@ssh.github.com:443/chenxz83/crystal-aura-plus.git`
- 推送：`git push -u origin master`（在用户自己电脑上执行；CI 会因 GITHUB_SHA 自动注入 commit.txt）

## 沙箱注意事项
- read 工具默认有行数上限，读大文件要分块（offset/limit 循环）
- 批量改写大文件后要校验行数和文件结尾，防止截断
