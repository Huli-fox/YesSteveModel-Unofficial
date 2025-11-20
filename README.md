## 如何使用
1.前置Mod：[unimixins](https://github.com/LegacyModdingMC/UniMixins/releases/download/0.1.23/+unimixins-all-1.7.10-0.1.23.jar)、[gtnhlib](https://github.com/GTNewHorizons/GTNHLib/releases/download/0.6.40/gtnhlib-0.6.40.jar)

2.前往[release](https://github.com/Huli-fox/YesSteveModel-Unofficial/releases)下载最新版

3.支持**文件夹形式**或 **.ysm格式**（部分可用）的模型文件。zip请解压为文件夹，确保文件夹结构像这样：
```
<模型名或你喜欢的名字>
├── main.json
├── arm.json
├── main.animation.json
├── arm.animation.json
├── extra.animation.json
└── *.png
```
或者这样：
```
<模型名或你喜欢的名字>
├── animations
├── avatars
├── models
├── textures
└── ysm.json
```
如果您发现模型并没有成功载入，请Alt+Y唤出模型管理页面，在靠近右上角的位置有个刷新图标的按钮，点击以尝试修复模型。
## Issues/Todo
### Major
- [x] 移除除ModelCommand外的所有命令，移除ModelManageGUI、DebugGUI。移除授权功能，现在所有模型都不需要授权
- [ ] 清理代码，提高可读性
- [x] 第一人称手臂实现不完善
- [ ] 第三人称下手上物品不显示
- [ ] 1.7.10服务器中本机似乎无法得知其他玩家的OnGround和飞行状态，导致动画判断出错。现有代码为了图省事将这两个状态写入了玩家元数据，须改用EEP同步
- [x] 新旧版本模型文件结构差异很大，参考Blockbench插件的将旧版本模型转为新版本的方法，反过来将新版模型转为旧版结构
- [ ] 内置的默认模型好像有点问题，也许应该把最新版的内置模型拿过来
### Minor
- [ ] 修复GUI界面光照问题。表现为除**创造模式**物品栏中小人光照正常外，其他GUI界面中（如生存模式物品栏）的模型都偏暗
- [ ] 修复选取皮肤（TextureScreen）界面的模型预览
- [ ] 睡觉时模型立起来
- [x] 爬梯时模型平躺
- [x] 修复多人游戏下，玩家每次进入游戏，跳跃动画在其他玩家眼中不播放，必须先受伤一次才会正常播放跳跃动画
- [ ] 多人游戏下看其他玩家原地跳跃，跳跃动画会播放2次。边走边跳/跑跳没问题
- [ ] 多人游戏下玩家头上名字标签不显示
## 鸣谢
- [酒石酸菌](https://github.com/TartaricAcid) 开发YSM
- [kaixiten](https://github.com/kaixiten) 测试&协助本项目开发
- [ExampleMod1.7.10](https://github.com/GTNewHorizons/ExampleMod1.7.10) MOD模板
- Powered by Gemini 2.5 Pro, Gemini 3 Pro, Lingma, DeepWiki and IntelliJ IDEA
