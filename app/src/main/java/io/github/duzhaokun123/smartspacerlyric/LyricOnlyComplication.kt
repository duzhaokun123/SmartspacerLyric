package io.github.duzhaokun123.smartspacerlyric

import android.graphics.drawable.Icon
import android.util.Log
import cn.lyric.getter.api.data.type.OperateType
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider

class LyricOnlyComplication : SmartspacerComplicationProvider() {
    private val defaultIcon by lazy {
        Icon.createWithResource(provideContext(), R.drawable.outline_audiotrack_24)
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = "Lyric Only",
            icon = defaultIcon,
            description  = "Lyric Only using LyricGetter API",
        )
    }

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val lyricData = LyricReceiver.lastLyricData ?: return emptyList()
        if (lyricData.type == OperateType.STOP) return emptyList()
        val r = mutableListOf<SmartspaceAction>()
        SmartspaceAction(
            id = "lyric_only",
            icon = LyricReceiver.lastLyricIcon.takeIf { lyricData.extraData.customIcon } ?: defaultIcon,
            title = lyricData.extraData.packageName,
            subtitle = lyricData.lyric,
        ).also { r.add(it) }
        Log.d("TAG", "getSmartspaceActions: ")
        return r
    }
}