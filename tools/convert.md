如果您的模型文件夹结构形如：
```
<模型名>
├── animations
├── avatars
├── models
├── textures
├── ...（其他文件夹）
└── ysm.json
```
那么您需要使用[python脚本](https://github.com/Huli-fox/YesSteveModel-Unofficial/blob/master/tools/convert_new_ysm.py)进行转换。
```
python convert_new_ysm.py 模型文件夹路径
```
它会在输入目录下创建 ysmu_convert，并把每个新版模型转换成：
```
<模型名>
├── main.json
├── arm.json
├── main.animation.json
├── arm.animation.json
├── extra.animation.json
└── *.png
```

- 支持输入目录本身就是一个新版模型，也支持输入目录下有多个模型子目录。
- 从 ysm.json.files.player 读取 main/arm 模型、main/arm/extra 动画和 player 贴图路径。
- 将 metadata.name、metadata.tips、metadata.license.type、metadata.authors[*].name 写入 main.json 的 description.ysm_extra_info。
- 额外保留了新版 properties.height_scale / width_scale 到旧版 ysm_height_scale / ysm_width_scale。
- 默认不覆盖已有输出；重复转换可加 --overwrite。
- 可用 --dry-run 只验证不写文件。