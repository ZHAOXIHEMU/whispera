# 编译 whispera

使用 JDK 21、Android SDK（compileSdk 37，targetSdk 36，minSdk 33）、NDK/CMake（精确版本见gradle.properties）、Python 3.12，以及web-ui构建所需Node.js/pnpm。Gradle使用仓库自带wrapper。

1. 在本机local.properties中设置sdk.dir，或配置ANDROID_HOME。
2. 安装Python 3.12。默认命令是python3；Windows可使用 -Pwhispera.buildPython="C:/path/to/python.exe" 指定解释器。build-tools脚本还使用python3命令，请确保PATH可找到它。
3. 在web-ui目录执行 pnpm install --frozen-lockfile 及 pnpm run build。没有pnpm的环境可能会跳过此构建；这会影响Web服务器的网页端，所以完整构建应安装pnpm。
4. Windows PowerShell先设置输出编码，再编译：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:PATH = $env:JAVA_HOME + '\bin;' + $env:PATH
$env:PYTHONIOENCODING = 'utf-8'
$env:PYTHONUTF8 = '1'
.\gradlew.bat :app:assembleRelease -Pwhispera.buildPython='C:/Python312/python.exe'
```

Release产物位于app/build/outputs/apk/release/。Release使用已有R8优化规则；没有本地release签名配置时使用本机debug证书，**这不等于调试构建**。自行构建的证书通常与仓库发布APK不同，不能据此保证覆盖安装。正式分发请自行配置并妥善保存签名密钥，切勿提交私钥或local.properties。

备份恢复定向测试：

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests 'me.rerere.rikkahub.data.sync.PendingRestoreTest'
```

0.2.6发布前10项恢复测试通过；完整测试套件存在此前记录的5项ConversationConfigTest失败，详见0.2.5记录。编译和单元测试不能替代手机上对启动、滑动、播放器、导入与AI服务的实测。

本次开源整理只把机器专属Python路径改为Gradle参数，格式检查基准改为HEAD，排除缓存和旧网页构建重复目录；应用实现对应已发布0.2.6。构建Python包时需要网络，依赖未完全锁定，无法保证不同日期构建结果逐字节一致。
