package io.github.duzhaokun123.smartspacerlyric

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider

class NotificationListener : NotificationListenerService() {
    data class Data(
        var title: String? = null,
        var icon: Icon? = null,
        var art: Icon? = null,
        var playing: Boolean = false,
    )

    companion object {
        const val TAG = "NotificationListener"

        val data = mutableMapOf<String, Data>()

        private val mediaNotificationIcons = mutableMapOf<String, Icon>()
    }

    private val mediaSessionManager by lazy {
        getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    private var mediaControllerCallbacks = mutableMapOf<String, MediaController.Callback>()

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager.addOnActiveSessionsChangedListener({ onActiveSessionsChanged(it) }, ComponentName(this, NotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification
        if (notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION).not()) return
        mediaNotificationIcons[packageName] = notification.smallIcon
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
    }

    private fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
        controllers ?: return
        controllers.forEach { controller ->
            val packageName = controller.packageName
            val callback = mediaControllerCallbacks.getOrPut(packageName) {
                object : MediaController.Callback() {
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        metadata ?: return
                        data.getOrPut(packageName) { Data() }.apply {
                            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                            icon = mediaNotificationIcons[packageName]
                            art = loadArt(metadata)?.let { Icon.createWithBitmap(it) }
                            Log.d(TAG, "onMetadataChanged: $packageName $this")
                        }
                        SmartspacerTargetProvider.notifyChange(this@NotificationListener, LyricTarget::class.java, packageName)
                    }

                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        state ?: return
                        data.getOrPut(packageName) { Data() }.apply {
                            playing = state.state == PlaybackState.STATE_PLAYING
                            Log.d(TAG, "onPlaybackStateChanged: $packageName $this")
                        }
                        SmartspacerTargetProvider.notifyChange(this@NotificationListener, LyricTarget::class.java, packageName)
                    }
                }
            }
            controller.registerCallback(callback)
        }
        val alivePackages = controllers.map { it.packageName }.toSet()
        data.keys.filter { it !in alivePackages }.forEach {
            data.remove(it)
            LyricReceiver.data.remove(it)
            Log.d(TAG, "onSessionUnactive: $it")
        }
        Log.d(TAG, "onActiveSessionsChanged: $alivePackages")
        SmartspacerTargetProvider.notifyChange(this, LyricTarget::class.java)
    }

    private fun loadArt(metaData: MediaMetadata): Bitmap? {
        return metaData.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metaData.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metaData.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    }
}