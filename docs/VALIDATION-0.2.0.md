# 0.2.0 安装包验证

- 安装包：releases/WeChatTavern-0.2.0.apk
- 字节数：275219193（262.47 MiB）
- 包名：cn.zhaoxihemo.wechatshell
- versionCode：2；versionName：0.2.0
- 最低 Android 13（API 33）；目标 Android 16（API 36）
- Android apksigner 验签通过；与空壳 0.1.0 的本机签名证书相同，可覆盖安装。
- zipalign 的 16 KiB 页面对齐检查通过。
- 本地调试签名安装包，包含原项目完整依赖，非商店发行签名包。

## 验证结果

最终构建与单元测试成功，详见 build-verified.log。共14个测试，0失败、0错误：原导演备注转换器10个，会话配置隔离1个（含多项断言），钱包URL范围1个，朋友圈媒体/时间规则2个。

本次没有连接真实手机。实际照片授权、不同厂商的相册分组、触摸交互、完整聊天及官网登录流程需要安装后确认；未将编译成功等同于全部功能的真机验证。

## 源码与行为说明

见 docs/WECHAT-CHANGES.md。新增会话身份、导演备注和背景配置均使用现有 ConversationConfig JSON 持久化。旧格式会话首次打开时固定当前可知的角色和备注资料，无法还原以前从未保存过的值。

临时余额测试Key没有写入项目或安装包。不会自动为用户配置服务商Key，需在设置中自行填写。