# YesSteveModel-Unofficial

## 现状分析

### 1.7.10 侧已具备的基础

- 构建环境：仓库根目录使用 GTNH Gradle convention plugin，目标 Minecraft `1.7.10`、Forge `10.13.4.1614`、MCP stable `12`，启用 Mixins、Jabel 和 Jackson shade/relocate。
- 启动入口：`src/main/java/com/fox/ysmu/ysmu.java` 通过 `CommonProxy` / `ClientProxy` 完成配置、模型扫描、网络注册、动画注册、渲染器和按键注册。
- 模型加载：`ServerModelManager.reloadPacks()` 创建 `config/ysmu/custom`、`export`、`cache`，复制内置模型，扫描旧式文件夹与部分 `.ysm` 文件，生成服务端缓存。
- 资源注册：`ClientModelManager` 把模型 JSON、动画 JSON、PNG 贴图注册到当前移植版 GeckoLib cache，并维护默认动画、贴图列表、比例和元数据。
- 网络同步：`NetworkHandler` 基于 Forge 1.7.10 `SimpleNetworkWrapper`，已经有模型文件 MD5 同步、密码发送、缓存命中加载、缺失文件下发、模型/贴图选择、收藏模型、主动播放动画、NPC/Bukkit 相关包。
- 玩家状态：用 `IExtendedEntityProperties` 实现 `ExtendedModelInfo` 和 `ExtendedStarModels`，保存模型、贴图、播放动画和收藏列表。
- 渲染：`ClientEventHandler` 取消 `RenderPlayerEvent.Pre` 的原版渲染，交给 `CustomPlayerRenderer`；第一人称手臂通过 `RenderHandEvent` 与 Angelica 兼容 Mixin 分流。
- 动画经验：已有基础状态优先级、条件动画名分类、主副手/护甲/使用/挥手判断、远端玩家运动状态同步和一批 Molang query/ysm 变量。
- 兼容边界：Backhand、Angelica 调用已经放在 compat 包中，应继续沿用这个边界。

### OpenYSM 侧新增能力

- 源码入口：`OpenYSM/src/main/java/com/elfmcys/yesstevemodel/YesSteveModel.java`，目标 Forge 1.20.1，mod id 为 `yes_steve_model`，版本为 `2.6.5-forge+mc1.20.1`。
- 新格式核心：`resource/RawYsmModel`、`YSMFolderDeserializer`、`YSMBinaryDeserializer`、`YSMBinarySerializer`、`YSMClientMapper` 能解析 `ysm.json` 文件夹结构、新版二进制 `.ysm`、模型包元数据、语言、函数、音效、额外实体和投射物模型。
- 服务端模型管理：OpenYSM 的 `model/ServerModelManager` 支持 `built` 和 `custom` 两类模型目录、内置模型黑名单、`server_index`、新密钥、服务端缓存、分块下发、导出 `.ysm`。
- 新同步协议：基于 `C2SModelSyncPayload` / `S2CModelSyncPayload` 的多阶段密钥握手、服务端模型 hash 列表、客户端缓存验证、缺失模型分块下载。
- 客户端模型组装：`ClientModelManager`、`ModelAssembly`、`ModelAssemblyFactory`、`PlayerModelBundle` 等把 RawYsmModel 组装成运行时模型、贴图、动画控制器、额外资源和 GUI 展示资产。
- 动画与 Molang：新增动画控制器、状态机、blend transition、timeline/on_entry/on_exit、Molang runtime、query 绑定、YSM 函数、物理函数、roaming 变量同步和反馈。
- 渲染范围：除玩家外，还覆盖第一人称背景/手、投射物、载具、护甲、鞘/背包/腰部等定位骨骼、加载状态和调试 overlay。
- 音频与图片：包含 Ogg Vorbis/Opus 解析、音频缓存/播放、JPEG/WebP/AVIF 转 PNG 入口；其中 ImageStream 解码依赖不在当前 `OpenYSM` 源码树内，但本仓库构建层已按上游坐标通过 JitPack 接入，后续只需要在客户端解码路径消费它。
- 现代 Capabilities：模型信息、收藏模型、玩家客户端模型、载具、投射物状态均使用 Forge Capability，1.7.10 必须改写为 EEP 或实体 ID 映射。

## 移植原则

1. 继续使用 `ysmu` 作为 1.7.10 mod id，保留 `com.fox.ysmu` 根包和现有公开类名，避免破坏已有服务器、配置与资源路径。
2. 以现有 1.7.10 运行框架承载新版功能，不整体替换为 OpenYSM 的 1.20.1 架构。
3. 先移植格式、缓存和同步，再扩展动画控制器，最后处理复杂渲染层、载具、投射物、音频和高级 GUI。
4. 保留旧版文件夹和旧版 `.ysm` 的兼容路径；新版 `ysm.json` 与 format 32 `.ysm` 作为新增路径并行接入。
5. 不重编号现有网络包 id。新增协议包只能追加 id，并在 `NetworkHandler` 中清楚标注方向。
6. 任何重 IO、解密、压缩、模型解析都放入 `ThreadTools.THREAD_POOL`，只在必须注册模型或贴图时回到 Minecraft 客户端线程。
7. 所有可选 Mod 调用继续通过 compat 包隔离。OpenYSM 中对 1.20.1 物品标签、姿态、双持和鞘/背包定位的判断，要映射到 1.7.10 原版能力或现有 Backhand 能力。
8. 产物仍面向 JVM 8。Jabel 只解决语法，不解决 Java 9+ 标准库 API；OpenYSM 中的 `record`、`Files.readString/writeString`、`List.of`、`Objects.requireNonNullElse*`、`VarHandle` 等需要替换或封装。

## 模块映射

| OpenYSM 模块 | 1.7.10 落点 | 处理策略 |
| --- | --- | --- |
| `YesSteveModel` 入口和 Forge config | `ysmu.java`、`CommonProxy`、`ClientProxy`、`Config` | 保留 1.7.10 生命周期；把新版配置字段折叠进 `Config` 和现有配置 GUI。 |
| `model/ServerModelManager` | `model/ServerModelManager` | 分阶段吸收 `built/custom/cache/server_index/blacklist/export`，保留旧缓存路径兼容。 |
| `resource/*`、`resource/pojo/*` | 新增到 `com.fox.ysmu.model.resource` 或相邻包 | 优先移植 `RawYsmModel`、文件夹解析、二进制解析、序列化；避免把 1.20.1 客户端类带入公共解析层。 |
| `rip.ysm.security`、`rip.ysm.algorithms`、`rip.ysm.zstd` | 新增 vendored 包或迁移到 `com.fox.ysmu.vendor` | 作为格式/缓存基础，先单元测试 varint、zstd、hash、加解密，再接入网络。 |
| `client/ClientModelManager` | `client/ClientModelManager` | 先做新版模型到当前 GeckoLibCache 的桥接；后续再引入 ModelAssembly。 |
| `client/model/*` | `client/model` | 与当前 `CustomPlayerModel` 并行，不一次性替换。Binary `.ysm` 的 baked geometry 需要单独桥接或移植 OpenYSM mesh renderer。 |
| `geckolib3/*` | `software/bernie/geckolib3` | 当前已有 1.7.10 移植版；只窄幅补齐动画控制器/Molang/mesh 能力，避免整包覆盖。 |
| `capability/*` | `eep/*`、必要时实体 ID 映射 | 玩家模型和收藏用 EEP；载具/投射物可先用客户端映射，成熟后再按实体扩展属性落地。 |
| `network/message/*` | `network/message/*` | 把现代 payload 编码拆成 1.7.10 `IMessage`；大文件分块、重试和握手状态写成独立类。 |
| `client/renderer/*`、`client/event/*` | `client/renderer`、`ClientEventHandler`、`mixin` | 玩家主渲染先复用现有取消原版事件方式；第一人称和特殊层按 1.7.10 渲染 API 重写。 |
| `audio/*` | 新增 `client/audio` | 先禁用或只支持短 OGG/Vorbis；Opus、seek、缓存池和 timeline 音效作为后续里程碑。 |
| `assets/yes_steve_model/builtin` | `assets/ysmu/custom` 或新增 `assets/ysmu/builtin` | 先用转换脚本生成兼容内置模型；完整新版包结构稳定后再引入 `built` 目录解包流程。 |

## 分阶段移植计划

### 阶段 0：建立基线和样本

目标是确认当前 1.7.10 移植没有被后续工作破坏，并固定用于验收的模型样本。

- 保留当前 `src` 作为可运行基线，记录旧式文件夹、旧式 `.ysm`、默认模型、多人同步和第一人称手臂的当前行为。
- 从 `OpenYSM/src/main/resources/assets/yes_steve_model/builtin` 选取至少三个样本：默认包、一个只含玩家模型的 `ysm.json` 包、一个带动画控制器或额外资源的包。
- 用 `tools/convert_new_ysm.py --dry-run` 验证这些样本能否退化为旧结构，作为阶段 2 之前的临时可用路径。
- 需要用户在仓库根目录运行 `.\gradlew.bat build` 和 `.\gradlew.bat runClient`，保存输出和截图作为基线。当前环境不运行 Gradle。

验收标准：当前 release 能加载旧式模型；转换脚本能处理选定 OpenYSM 文件夹样本；后续任何阶段都不能破坏旧式模型加载。

### 阶段 1：移植纯格式与安全基础

目标是在不碰渲染的情况下，让 1.7.10 代码能读写 OpenYSM 的底层数据。

- 移植 `rip.ysm.security.YSMByteBuf`、`YsmCrypt`、`YSMClientCache`、`rip.ysm.algorithms`、`rip.ysm.zstd` 和必要 legacy 工具。
- 替换 Java 17/9+ API：`record` 改普通 final 类，`Files.readString/writeString` 改 `Files.readAllBytes`/`Files.write`，`List.of` 改 Guava 或数组列表，`Objects.requireNonNullElse*` 改显式分支，移除 `VarHandle`。
- 为 `YSMByteBuf`、hash、zstd、`.ysm` 文件头、server/client cache 文件名生成增加 JUnit 5 测试。测试资源使用小型 fixture，不依赖 Minecraft 启动。
- 确认新代码不引入 Java 9+ runtime API，符合 JVM 8。

验收标准：`.\gradlew.bat test` 能跑过格式基础测试；这些类不引用 1.20.1 Minecraft/Forge 类型。

### 阶段 2：统一模型中间表示

目标是让服务端能扫描新版 `ysm.json` 文件夹和新版 `.ysm`，并生成一个与渲染无关的 RawYsmModel。

- 新增 `RawYsmModel` 与 `YSMFolderDeserializer`，支持 `ysm.json.files.player.model/texture/animation/animation_controllers`、metadata、properties、lang、functions、sounds、vehicles、projectiles。
- 继续支持 legacy folder fallback：没有 `ysm.json` 时读取 `main.json`、`arm.json`、`*.png`、`*.animation.json`。
- 新增 `YSMBinaryDeserializer` 和 `YSMBinarySerializer`，先覆盖 format 32；旧 format 只保留能安全识别和报错的路径，后续再扩展。
- 改造 `ServerModelManager.reloadPacks()`：新增 `BUILT` 目录、`server_index`、黑名单、内置资源解包；暂时保留 `CUSTOM` 下旧式扫描。
- 保持 `ModelIdUtil` 作为磁盘名称到 ResourceLocation 的唯一规范化工具。

验收标准：服务端启动时能扫描 `config/ysmu/custom/<new model>/ysm.json`；无效模型给出清晰日志；旧式模型仍出现在服务端模型列表。

### 阶段 3：客户端注册桥接

目标是先把新版文件夹模型渲染出来，而不是一次性移植 OpenYSM 全套 ModelAssembly。

- 对 `ysm.json` 文件夹模型，优先读取其引用的 `models/main.json`、`models/arm.json`、玩家贴图和主要动画文件，转换为当前 `ModelData`，注册到现有 `ClientModelManager` / `GeckoLibCache`。
- 把 metadata、作者、license、height_scale、width_scale、extra animation 名称映射到当前 `ClientModelManager.EXTRA_INFO`、`SCALE_INFO` 和 `EXTRA_ANIMATION_NAME`。
- 对新版 binary `.ysm`，先通过 RawYsmModel 还原 main/arm 几何。这里有两个可选实现路径：第一，把 RawYsmModel 的几何转换为当前 GeckoLib 可消费的 RawGeoModel/GeoBone；第二，移植 OpenYSM 的 baked mesh 渲染器。优先做最小桥接，只有当转换丢失关键几何信息时再进入渲染器移植。
- 对 JPEG/WebP/AVIF 贴图先保留降级开关：PNG 必须支持；虽然 ImageStream 依赖已经接入构建，但在解码/注册路径完成前，其他格式仍应日志提示并跳过，而不是静默失败。

验收标准：OpenYSM 内置默认包和至少一个 `misc` 包能在 1.7.10 客户端模型列表中显示并渲染；旧式模型和转换脚本产物继续可用。

### 阶段 4：新版模型同步协议

目标是把 OpenYSM 的 hash/cache/分块同步能力接入 1.7.10 多人环境。

- 在现有 `NetworkHandler` 中追加新包 id，不重排旧 id。建议新增 `C2SModelSyncPayload17`、`S2CModelSyncPayload17`、`C2SVersionCheck17`、`S2CVersionCheck17`、`C2SCompleteFeedback17`。
- 把 OpenYSM 的 `PlayerSyncState`、packet 01/02/03/04/05 握手和 chunk 逻辑移到 1.7.10 包处理器，但所有加解密、读写缓存、序列化都在后台线程执行。
- 保留旧版 MD5/AES 同步作为 fallback，直到新版协议在服务端、局域网和单人内置服务器都稳定。
- 1.7.10 没有现代 `Connection.channel().unsafe().outboundBuffer()` 路径时，不照搬 OpenYSM 的可靠发送实现；改用分块大小、发送队列、ack 或节流任务来控制带宽。
- 配置中加入 `ThreadCount`、`BandwidthLimit`、`PlayerSyncTimeout`、`LowBandwidthUsage`、`AcceptSoundFX` 等服务端字段。

验收标准：专用服务端中，客户端首次进服会下载缺失模型，第二次进服命中客户端缓存；中途切服/退服会清理连接状态；旧版客户端路径不会崩溃。

### 阶段 5：动画控制器与 Molang

目标是让 OpenYSM 的控制器、blend、timeline 和模型自定义函数逐步可用。

- 先补齐当前 `AnimationRegister` 的状态名和 1.7.10 映射：`climb/climbing` 映射到 1.7.10 ladder 行为；双持依赖 Backhand；1.7.10 无对应原版能力的现代状态保持不可触发。
- 移植 `AnimationControllerFile`、controller state、blend transition、on_entry/on_exit、sound_effects 的数据结构，但先只在玩家主模型路径启用。
- 选择一个 Molang 运行时策略：要么扩展当前 `software.bernie.geckolib3.core.molang`，要么并行移植 OpenYSM 的 `molang/runtime` 和 `geckolib3/core/molang`。不要在同一阶段同时替换解析器和渲染器。
- 将 OpenYSM 的 query/ysm/ctrl/fn 函数按 1.7.10 能力分级：基础玩家/世界/物品 query 优先，方块标签、附魔等级、相对方块、天气、维度名、输入检测、物理函数后置。
- 用 `ConditionManager` 继续分类 `hold_*`、`swing_*`、`use_*`、armor 条件动画，新增 `vehicle`、`passenger` 分类时必须有 1.7.10 行为验收。

验收标准：带 `animation_controllers` 的 OpenYSM 模型能自动切换 idle/walk/run/sneak/use/swing；额外动画轮盘仍可播放和停止；Molang 表达式错误不会导致渲染线程崩溃。

### 阶段 6：玩家渲染增强

目标是在不破坏当前玩家替换渲染的基础上补齐 OpenYSM 的显示效果。

- 保留 `RenderPlayerEvent.Pre` 取消原版渲染的方式，继续使用 `CustomPlayerRenderer` 作为唯一玩家渲染入口。
- 先迁移玩家层：手持物、护甲、披风/鞘/定位骨骼、第一人称手臂、睡觉/骑乘/潜行/受伤颜色。
- OpenYSM 的 `PoseStack`、`RenderType`、`MultiBufferSource` 逻辑必须改写为 1.7.10 的 `GlStateManager`、`Tessellator`、纹理绑定和当前 GeoLayerRenderer 接口。
- Angelica shader hand renderer 路径继续通过 `AngelicaCompat` 和现有 `MixinItemRenderer` 分流。
- `SpecialPlayerRenderEvent` 要保持兼容，方便 NPC/Bukkit/其他 Mod 覆盖模型和贴图。

验收标准：第三人称玩家、GUI 预览玩家、第一人称手臂、护甲和手持物在默认模型与 OpenYSM 样本模型上位置合理；原版皮肤 fallback 仍可用。

### 阶段 7：GUI、配置、元数据与导出

目标是让用户能完整选择、收藏、查看和导出新版模型。

- 扩展 `PlayerModelScreen`、`PlayerTextureScreen`、`AnimationRouletteScreen`，显示模型包名、作者、license、tips、默认贴图、额外动画按钮和加载状态。
- 把 OpenYSM 的多语言 JSON 转换为 1.7.10 `.lang` key，新增用户可见文本时同步更新 `en_US.lang` 和 `zh_CN.lang`。
- 加入 `ShowModelIdFirst`、`DisableProjectileModel`、`DisableVehicleModel`、`DisableLoadingStateScreen`、`LoadingStatePosition`、`SoundVolume` 等配置，并更新 `ConfigScreen`。
- 实现服务端和客户端缓存导出 `.ysm`，导出路径保持 `config/ysmu/export`。

验收标准：用户能在 GUI 中选择 OpenYSM 模型/贴图、收藏模型、查看 metadata、播放额外动画、导出缓存模型；配置保存后重启仍生效。

### 阶段 8：投射物、载具、音效和高级资源

目标是补齐 OpenYSM 相比旧版 YSM 最大的新功能面，但这些不应阻塞玩家模型 MVP。

- 投射物：先处理箭、钓鱼浮标、药水等 1.7.10 原版实体；跨版本不存在的三叉戟、弩等保持禁用，除非后续新增明确的独立兼容层。
- 载具：先处理船、矿车、猪/马骑乘视觉；OpenYSM 的 passenger locator 需要和 1.7.10 骑乘偏移分别验收。
- 音效：先支持短 OGG/Vorbis timeline 音效；Opus、seek、音量配置、对象池清理后续补齐。
- 额外图片：PNG 为必选；JPEG/WebP/AVIF 取决于 ImageStream 在 1.7.10 客户端纹理路径中的实际接入效果，必要时再回退到替代解码方案。
- overlay：加载状态和调试信息按 1.7.10 `RenderGameOverlayEvent` 重写。

验收标准：禁用投射物/载具配置有效；启用时不会影响无对应实体的环境；音效失败只记录日志，不影响模型渲染。

### 阶段 9：清理、兼容与发布准备

目标是把临时桥接变成可维护实现。

- 移除重复的解析路径和调试 `System.out.println`，统一日志到 `ysmu.LOG`。
- 检查 shaded/relocated 依赖，避免和其他 1.7.10 Mod 冲突。
- 为每个新增网络包记录 id、方向、用途和兼容策略。
- 更新 `tools/convert_new_ysm.py`：当原生 `ysm.json` 支持稳定后，把它定位为兼容旧版本的工具，而不是必需步骤。
- 更新 release notes、README 使用说明和已知限制。

验收标准：`.\gradlew.bat build` 成功；单人、局域网、专用服务器和带/不带可选兼容 Mod 的环境都通过基本模型加载与渲染检查。

## 关键风险与处理

- **不要整包覆盖 GeckoLib。** 当前 `software/bernie/geckolib3` 已为 1.7.10 做了大量适配；OpenYSM 的 GeckoLib 管线依赖现代渲染 API。应以补丁方式移植缺失能力。
- **新版 binary `.ysm` 几何不是简单旧 JSON。** OpenYSM 反序列化后得到 baked face 数据。若不能无损转换为当前 GeoModel，就必须单独移植 OpenYSM mesh renderer 的核心，而不是继续堆转换脚本。
- **Capabilities 必须重写。** 1.7.10 不存在现代 Capability 生命周期；玩家持久状态用 EEP，客户端临时状态用 manager/map，实体扩展按需求拆分。
- **Java 17 语法和 Java 9+ API 要分开处理。** Jabel 可以保留部分现代语法，但不能在 JVM 8 上调用不存在的标准库方法。
- **OpenYSM ImageStream 依赖不在当前源码树中，但已在本仓库构建层通过 JitPack 声明。** 在确认 1.7.10 客户端能稳定加载这些 ImageIO 插件前，非 PNG 图片仍应作为降级能力处理。
- **网络协议不能阻塞 Netty/主线程。** 新版加密、zstd、模型解析和大文件 IO 都必须异步，并在客户端线程只做最终资源注册。
- **1.20.1 物品标签无法直接搬到 1.7.10。** `data/yes_steve_model/tags/items` 只能作为分类参考，实际判断要使用 1.7.10 item、ore dictionary 或兼容 Mod 包装。

## 验证命令

本仓库的 Gradle 需要宿主机缓存和网络访问，自动化代理环境不要直接运行 Gradle。需要构建、测试或启动时，请在仓库根目录手动运行并粘贴输出：

```powershell
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat runClient
.\gradlew.bat runServer
```

阶段性验收建议：

- 格式基础：`.\gradlew.bat test`，重点看 `YSMByteBuf`、`YsmCrypt`、`YSMFolderDeserializer`、`YSMBinaryDeserializer` fixture。
- 客户端渲染：`.\gradlew.bat runClient`，放入旧式模型、转换后的 OpenYSM 文件夹、原生 `ysm.json` 文件夹，逐个检查模型列表和渲染。
- 多人同步：一个 `runServer` 加两个客户端，第一次进服应下载模型，第二次应命中缓存。
- 兼容环境：分别在无可选 Mod、带 Backhand、带 Angelica 的客户端验证主副手、鞘/特殊物品、第一人称手臂和 shader hand path。
