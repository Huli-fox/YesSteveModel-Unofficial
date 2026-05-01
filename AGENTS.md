# AGENTS.md

本文件给后续维护者和代码代理使用。项目是将 Yes Steve Model 思路移植到 Minecraft Forge 1.7.10 的 Java Mod，目标是用自定义 Geckolib 模型替换玩家模型，并在多人环境中同步模型、材质和动画状态。

## ExecPlans

When writing complex features or significant refactors, use an ExecPlan (as described in PLANS.md) from design to implementation.

## 项目概况

- Mod ID 是 `ysmu`，入口类是 `com.fox.ysmu.ysmu`。
- 目标版本固定为 Minecraft `1.7.10` / Forge `10.13.4.1614`，构建基于 GTNH 的 RFG/Convention 模板。
- 源码主要在 `src/main/java/com/fox/ysmu`。`software/bernie/geckolib3`、`com/eliotlash/mclib`、`net/geckominecraft` 是为 1.7.10 适配/内嵌的渲染、动画和兼容代码，不能按高版本 upstream 代码直接覆盖。
- 资源在 `src/main/resources/assets/ysmu`，内置模型放在 `assets/ysmu/custom/*`，运行时会复制到 `config/ysmu/custom`。
- 运行时生成的 `run/`、`build/`、`config/`、缓存和日志都不应提交。

## 构建和运行

- Windows 优先使用 `.\gradlew.bat build`、`.\gradlew.bat runClient`、`.\gradlew.bat runServer`、`.\gradlew.bat test`。
- Gradle Wrapper 使用 Gradle `8.13`。`gradle.properties` 启用了 `enableModernJavaSyntax=true`，可写较新的 Java 语法，但最终仍面向 JVM 8/1.7.10 生态。
- `disableSpotless=true`、`disableCheckstyle=true` 当前是项目状态，不要在无关改动里打开或重排全仓库格式。
- Access Transformer 在 `src/main/resources/META-INF/geckolib_at.cfg` 和 `ysmu_at.cfg`，当前 `ysmu_at.cfg` 暴露了 `net.minecraft.client.renderer.ItemRenderer *`。
- 运行/打包依赖见 `dependencies.gradle`。玩家侧 README 要求前置 `UniMixins` 和 `GTNHLib`；开发环境还包含 NEI、Nashorn、Backhand、Et Futurum 等可选/运行时依赖。

## 核心结构

- `CommonProxy`：`preInit` 读取配置并调用 `ServerModelManager.reloadPacks()`；`init` 注册网络；`serverStarting` 注册 `/ysm reload`。
- `ClientProxy`：注册动画状态和 Molang 变量，创建 `CustomPlayerRenderer`，注册替换渲染器和按键。
- `CommonEventHandler`：通过 GTNHLib `@EventBusSubscriber` 自动订阅公共事件，注册玩家 EEP、登录同步、追踪同步和服务端 tick 同步。
- `ClientEventHandler`：客户端事件入口。取消原版 `RenderPlayerEvent.Pre` 后调用自定义渲染器；处理第一人称手臂、HUD 小人、贴图 stitch 后的默认模型加载。
- `network/NetworkHandler`：使用 1.7.10 的 `SimpleNetworkWrapper`，通道名 `ysmu_network`。新增消息时必须给唯一 packet id，并明确 `Side.CLIENT` 或 `Side.SERVER`。
- `model/ServerModelManager`：管理 `config/ysmu/custom`、`auth`、`export`、`cache/server`、`cache/client` 和 `PASSWORD`。
- `client/ClientModelManager`：客户端把同步来的模型注册到 `GeckoLibCache`，并维护 `MODELS`、`SCALE_INFO`、`EXTRA_INFO`、`EXTRA_ANIMATION_NAME` 等运行时缓存。

## 模型和同步流程

1. `ServerModelManager.reloadPacks()` 创建目录，复制内置模型，初始化/读取 `PASSWORD`，扫描 `custom` 和 `auth`。
2. `FolderFormat` 支持文件夹模型，至少需要 `main.json`、`arm.json` 和一个 `.png`。缺失 `main.animation.json`、`arm.animation.json`、`extra.animation.json` 时会回退到默认模型动画。
3. `YsmFormat` 支持 `.ysm` 文件，要求内部包含 `main.json`、`arm.json` 和至少一个 `.png`。
4. 服务端把模型打包为 `ModelData`，经 `EncryptTools.assembleEncryptModels()` 压缩、AES 加密并按 MD5 写入 `cache/server`。
5. 玩家登录或 `/ysm reload` 后，服务端请求客户端上报 `cache/client` 中已有 MD5。
6. 客户端发送 `SyncModelFiles`；服务端发送加密密码和缺失模型文件，或让客户端加载已存在缓存。
7. 客户端 `RequestLoadModel.loadModel()` 等待 `ClientModelManager.PASSWORD`，解密模型后通过 `Minecraft.func_152344_a` 回到客户端主线程注册资源。

## 渲染和动画

- `CustomPlayerRenderer` 继承内嵌 Geckolib 的 `GeoReplacedEntityRenderer`，根据玩家 `ExtendedModelInfo` 选择主模型和材质；NPC 数据由 `NPCData` 覆盖。
- `CustomPlayerModel` 提供模型、材质、动画 `ResourceLocation`，并在 `setLivingAnimations` 中写入 Molang 查询变量、处理头部旋转和第一人称定位。
- `CustomPlayerEntity` 注册多个动画控制器：预并行、主状态、主/副手持有、挥手、使用、并行、盔甲槽位、额外播放控制器。
- `AnimationRegister` 定义主状态优先级和 Molang 变量。优先级从 `Priority.HIGHEST` 到 `Priority.LOWEST`，最后兜底 `idle`。
- `ConditionManager` 根据动画名建立 swing/use/hold/armor 条件索引；客户端重新同步模型时会清空并重建。
- 1.7.10 远程玩家缺少可靠的 `onGround` 和飞行状态，项目用 `DataWatcher` id `28` 的 bit flags 同步：`ON_GROUND=0x01`、`IS_FLYING=0x02`。改这里前必须检查是否与其他 mod 冲突。

## 客户端/服务端边界

- 公共代码不要直接引用 `Minecraft.getMinecraft()`、`RenderManager`、`GL11`、`TextureManager` 等客户端类。客户端逻辑放在 `client` 包、`ClientProxy` 或 `@SideOnly(Side.CLIENT)` 方法里。
- 网络 handler 里不要在后台线程直接改渲染资源、GUI 或世界对象。需要改客户端状态时，尽量切回主线程，当前项目已用 `Minecraft.func_152344_a` 处理模型注册。
- EEP 使用 1.7.10 的 `IExtendedEntityProperties`，不是高版本 Capability。新增玩家持久状态时按 `ExtendedModelInfo`、`ExtendedAuthModels`、`ExtendedStarModels` 的注册和 NBT 模式做。
- Forge 1.7.10 没有高版本的一些 API，常见替代是 `DataWatcher`、`SimpleNetworkWrapper`、`NBTTagCompound`、`ResourceLocation`、`Tessellator.instance`、`GL11`。

## 兼容层

- Backhand 相关逻辑必须通过 `compat/BackhandCompat`，不要在业务代码里直接依赖 Backhand API。
- Et Futurum Requiem 相关逻辑必须通过 `compat/EtfuturumCompat`，包括鞘翅飞行、旁观者和鞘翅旋转。
- `compat/Utils.j2l` 用于 JOML `Quaternionf` 到 LWJGL `Quaternion` 的转换，渲染旋转代码已有依赖。

## 模型资源约定

- 模型目录名和 `.ysm` 文件名会成为 `ResourceLocation` 路径，必须通过 `Utils.isValidResourceLocation`。
- 纹理 id 形如 `ysmu:<model>/<texture.png>`；主模型 id 形如 `ysmu:<model>/main`；手臂模型 id 形如 `ysmu:<model>/arm`，统一通过 `ModelIdUtil` 生成。
- Geckolib 模型 JSON 当前只处理 `FormatVersion.VERSION_1_12_0`，解析入口是 `Converter.fromJsonString`。
- README 和 `.lang` 文件按 UTF-8 读取；PowerShell 查看中文时使用 `Get-Content -Encoding UTF8`。

## 已知风险和待核查点

- README 中列出若干未修问题：多人 onGround/飞行状态、默认模型、GUI 光照、TextureScreen 预览、多人原地跳跃动画重复、玩家名牌不显示。
- 授权模型逻辑目前看起来尚未严格生效：`ModelData.isAuth()` 返回 `false`，`ServerModelInfo` 构造器忽略 `needAuth` 参数，`ExtendedAuthModels.containModel()` 返回 `true`。改授权功能前先修正并补测服务端校验。
- `CommonEventHandler.updateData()` 使用同一个 `oldData` 计算两个 bit，若同 tick 同时改变 onGround 和 flying，需要确认不会互相覆盖。
- `ThreadTools.THREAD_POOL` 是全局 0-10 线程池，当前用于网络文件发送、等待世界/密码、延迟加载。新增后台任务要避免无限等待和直接触碰客户端渲染状态。
- `SendModelFile` 用 `message.data.length == 48` 判断密码包，依赖 `EncryptTools.PASSWORD_SIZE` 加密后的长度假设。调整密码格式或加密方式时必须同步改协议。
- 二进制模型包有 `EncryptTools.HEAD` 和 `VERSION`。任何格式变更都应 bump version，并保留旧缓存兼容或明确清理策略。

## 维护建议

- 优先做窄改动，不要顺手重排内嵌 Geckolib/mclib 大片代码。
- 新增网络消息时同步更新 packet id、序列化/反序列化、线程边界和发送方向。
- 新增模型格式或缓存字段时，同时检查 `FolderFormat`、`YsmFormat`、`ModelData`、`EncryptTools`、`ClientModelManager.registerAll()`。
- 新增动画状态时在 `AnimationRegister.registerAnimationState()` 选择合适优先级，并确认对应动画名不存在时的兜底行为。
- 修改渲染矩阵、光照、blend、cull、depth 后要成对恢复 OpenGL 状态，优先参考现有 `pushMatrix/popMatrix` 和 `RenderHelper` 调用方式。
- 修改 UI/按键时检查 `client/input`、`client/gui` 和语言文件 `assets/ysmu/lang/*.lang`。
- 提交前至少运行 `.\gradlew.bat build`；如果只改文档，可说明未运行构建。
