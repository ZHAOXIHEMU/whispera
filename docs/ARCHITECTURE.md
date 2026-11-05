# 定制实现

主界面：app/src/main/java/me/rerere/rikkahub/ui/pages/wechat/。WeChatHome提供四个标签页；CharacterDirectory提供角色导入/管理与拼音排序，ConversationSettings提供会话信息设置；TransferPage使用追加式迁移，重新映射ID，保留现有角色与聊天。

聊天会话：Conversation、ConversationConfig及ConversationSession区分原始角色卡和会话快照。修改会话经ChatService更新最新状态，不用旧数据库快照覆盖正在生成的内容。角色卡变化不应自动改掉旧会话的身份配置。

导演备注：全局配置初始化新会话，会话保存自己的备注内容、开关与注入参数。发送请求按当前Conversation ID构建提示词。保留原有注入深度、角色、频率及顺序。

朋友圈：MomentsStore及MomentsPages使用手机指定文件夹生成本地媒体组合；没有导入角色时不填造角色身份。视频播放器使用Media3。这里没有朋友圈服务器。

性能：0.2.6保留已预加载标签页；聊天预览JSON解析转到IO；只读头像避免构建编辑器；大备份提交后的清理移到后台。使用Release/R8构建。尚未真机验证所有性能改善。

上游功能：ai、speech、search、web、workspace、mediagen及聊天消息工具等继续复用华灯/RikkaHub。上游设计差异文档放在docs/UPSTREAM-DIVERGENCE*.md；它们描述上游背景，不是本分支全部能力的验收承诺。

来源：项目以用户提供的rikkahub-sillytavern-android-huadeng.zip为起点，没有该ZIP对应的原始Git历史，因此本仓库以源码快照发布；没有伪造上游提交历史。原始许可保留。
