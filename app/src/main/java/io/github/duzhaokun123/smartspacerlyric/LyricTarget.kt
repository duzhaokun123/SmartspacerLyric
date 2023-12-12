package io.github.duzhaokun123.smartspacerlyric

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.util.Log
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon as SSIcon

class LyricTarget : SmartspacerTargetProvider() {
    data class Data(
        val title: String,
        val subtitle: String,
        val image: Icon?,
        val icon: Icon,
    )

    companion object {
        const val TAG = "LyricTarget"
    }

    private val defaultIcon by lazy {
        Icon.createWithResource(provideContext(), R.drawable.outline_audiotrack_24)
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = "Lyric",
            icon = defaultIcon,
            description  = "Lyric using LyricGetter API",
        )
    }

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val r = mutableListOf<SmartspaceTarget>()
        val packages = NotificationListener.data.keys + LyricReceiver.data.keys
        packages.forEach {  packageName ->
            val data = marge(packageName, NotificationListener.data[packageName] ?: NotificationListener.Data(), LyricReceiver.data[packageName] ?: LyricReceiver.Data())
            Log.d(TAG, "getSmartspaceTargets: $packageName $data")
            if (data.image == null) {
                TargetTemplate.Basic(
                    id = packageName,
                    componentName = ComponentName(provideContext(), LyricTarget::class.java),
                    title = Text(data.title),
                    subtitle = Text(data.subtitle, maxLines = 2),
                    icon = SSIcon(data.icon),
                ).create()
            } else {
                TargetTemplate.Image(
                    context = provideContext(),
                    id = packageName,
                    componentName = ComponentName(provideContext(), LyricTarget::class.java),
                    icon = SSIcon(data.icon),
                    title = Text(data.title),
                    subtitle = Text(data.subtitle, maxLines = 2),
                    image = SSIcon(data.image),
                    onClick = null
                ).create()
            }.apply {
                canBeDismissed = false
                r.add(this)
            }
        }
        return r
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        return false
    }

    private fun marge(packageName: String, mediaData: NotificationListener.Data, lyricData: LyricReceiver.Data): Data {
        return Data(
            title = mediaData.title ?: packageName,
            subtitle = if (mediaData.playing || lyricData.playing) lyricData.lyric ?: "Playing" else "Paused",
            image = mediaData.art,
            icon = lyricData.icon ?: mediaData.icon ?: defaultIcon,
        )
    }
}