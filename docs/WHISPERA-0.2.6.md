# whispera 0.2.6 — 2026-10-31

- 保留0.2.5所有功能、粉色爱心启动图标和设置整理。版本号0.2.6，versionCode 8。
- 底部四个图标由Photoshop直接依据用户截图提取轮廓、清除背景并保留透明边缘，替换此前Canvas手绘路径；未选中聊天图标单独制作空心轮廓。PSD和处理脚本在design目录。图标画布改34×30dp，调整图文间距，底栏总高仍58dp。
- 首页在首屏显示且停止滚动500ms后预加载并保留四个标签页，避免反复切换时重新建立通讯录拼音索引、列表和头像。滑动期间不会启动预加载。
- 聊天最后一条消息的JSON解码、预览提取移至IO线程；只读头像不再注册裁剪器、文件选择器和编辑弹窗状态，也不再使用BoxWithConstraints子组合。
- 备份恢复仅在确有pending事务时执行；恢复提交后原数据备份目录的清理延后到后台，正常启动不再同步扫描/删除大批残留文件。后台清理只删除completed目录，保留pending与可能正在导入的preparing目录。
- 发布使用Release构建，启用已有R8优化规则，保留WebView桥；未配置独立发布密钥时沿用此前安装证书。

验证：Release编译、R8优化、发布必要Lint检查通过（build-whispera-026-final.log）；10项PendingRestore测试全部通过。0.2.6/code8，安装包未开启debuggable；签名证书与0.2.5一致，16KB对齐检查通过。五张Photoshop底栏资源在实际APK内逐像素核对一致。APK为124,733,412字节，约118.96 MiB。

首次Release构建的原有Python清理脚本遇到GBK输出特殊符号错误；指定PYTHONIOENCODING=utf-8及PYTHONUTF8=1后构建成功。初次失败日志保留在build-whispera-026.log。

没有连接真机，尚未实测冷启动秒数、滑动帧率及Release所有功能的运行情况。
