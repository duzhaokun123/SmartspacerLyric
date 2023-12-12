package io.github.duzhaokun123.smartspacerlyric

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import cn.lyric.getter.api.data.LyricData
import cn.lyric.getter.api.data.type.OperateType
import cn.lyric.getter.api.tools.Tools
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider

class LyricReceiver : BroadcastReceiver() {
    data class Data(
        var lastLyric: String? = null,
        var lastIcon: Icon? = null,
        var playing: Boolean = false,
    )

    companion object {
        const val TAG = "LyricReceiver"

        val data = mutableMapOf<String, Data>()

        fun getData(packageName: String): Data {
            return data.getOrPut(packageName) { Data() }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return
        val lyricData = intent.getParcelableExtra("Data", LyricData::class.java) ?: return
        when(lyricData.type) {
            OperateType.UPDATE -> {
                val packageName = lyricData.extraData.packageName
                val icon = lyricData.extraData.base64Icon
                    .takeIf { lyricData.extraData.customIcon }
                    ?.let { Icon.createWithAdaptiveBitmap(Tools.base64ToDrawable(lyricData.extraData.base64Icon)) }
                data.getOrPut(packageName) { Data() }.apply {
                    lastLyric = lyricData.lyric
                    lastIcon = icon
                    playing = true
                }
                SmartspacerTargetProvider.notifyChange(context, LyricTarget::class.java, packageName)
            }
            OperateType.STOP -> {
                // FIXME: we can't get packageName this case, we can only remove all
                data.clear()
                SmartspacerTargetProvider.notifyChange(context, LyricTarget::class.java)
            }
        }
    }
}