## 中文

一个按个人习惯定制的 Android AI 聊天客户端，基于 **RikkaHub 的华灯分支**开发，使用 Kotlin + Jetpack Compose。外层采用微信风格，聊天阅读界面保留华灯的配色、排版和消息操作。

**这是个人定制项目，按项目所有者的需求持续修改。想使用或调整功能，建议让自己的 coding agent 阅读代码并适配。** 本项目独立于微信，不接入微信聊天网络。

主要功能：

- 角色卡和聊天会话分离，同一角色可以有多段聊天；独立标题、角色名称/头像快照、聊天背景。
- 导演备注区分全局默认与会话配置；新建会话复制快照，旧会话保留自己的设置。
- PNG/JSON角色卡导入导出、华灯备份追加导入、全部导出、聊天置顶与隐藏。
- 从指定的本地图片/视频文件夹生成朋友圈风格内容，使用已导入角色的身份。
- DeepSeek余额查看、AI供应商配置，以及沿用上游的消息工具、分支等能力。

当前安装包：**0.2.6 Release / versionCode 8，约119 MiB，Android 13–16，arm64-v8a**。从 [Releases](https://github.com/ZHAOXIHEMU/whispera/releases) 下载 APK；安装包不包含个人聊天、角色库或 API Key。

已完成编译、10项备份恢复测试、签名和16KB对齐检查；尚未连接真机进行完整功能与性能验证。旧版扩大测试中的已知失败记录保留在更新日志中，未宣称全套测试通过。

- [全部更新记录](CHANGELOG.md)
- [编译方法](docs/BUILD.md)
- [定制实现与上游关系](docs/ARCHITECTURE.md)
- [0.2.6改动与验证范围](docs/WHISPERA-0.2.6.md)

## Source and license / 来源与许可

- Primary upstream: [HuaDeng / rikkahub-sillytavern-android](https://github.com/MiaoWuNYA/rikkahub-sillytavern-android)
- Original project: [RikkaHub](https://github.com/rikkahub/rikkahub)
- Material Color Utilities sources are included with their original license notices.

Distributed under the existing **GNU AGPL v3** license; see [LICENSE](LICENSE). Upstream copyright and third-party license notices are retained. The UI is inspired by WeChat; this project is not affiliated with Tencent. Bundled third-party files retain their respective notices.

个人密钥、签名私钥、构建缓存、个人资料和私有素材没有上传。设置中的供应商、网页和扩展仍可能按用户配置联网；本项目不承诺所有上游可选服务都已完成真机验证。
