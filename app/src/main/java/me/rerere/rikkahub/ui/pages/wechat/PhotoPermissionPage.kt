package me.rerere.rikkahub.ui.pages.wechat
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
@Composable fun WeChatPhotoPermission(){val context=LocalContext.current;WxPage("照片读取权限"){WxRow("打开系统照片权限设置"){context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+context.packageName)))}}}
