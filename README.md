### 构建：

克隆到IDEA，别的IDE应该也可以

### 获取相关文件：

查看release与https://www.curseforge.com/minecraft/mc-mods/geckolib-unofficial-1-7-10

### 使用：

确保你的mods文件夹下有ysmu-0.1.jar、geckolib-unofficial-1.7.10-1.0.3.jar。

解压model.zip至你的.minecraft目录下，形成.minecraft\model\assets\...的结构（如果你开启了版本隔离可能有所不同）。

进入游戏后输入/transform main/night以切换模型，输入/transform clear换回原版模型。

### Todo:

修复游泳动画、骑乘朝向问题

增加受伤/死亡动画

调整物品在手上的渲染

玩家加入记忆上次模型

重构模型文件读取-适配GTNH（目前加载到重新加载资源包100%会无响应）

