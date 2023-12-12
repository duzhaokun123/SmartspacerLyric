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
                        }
                        SmartspacerTargetProvider.notifyChange(this@NotificationListener, LyricTarget::class.java, null)
                    }

                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        state ?: return
                        data.getOrPut(packageName) { Data() }.apply {
                            playing = state.state == PlaybackState.STATE_PLAYING
                        }
                        SmartspacerTargetProvider.notifyChange(this@NotificationListener, LyricTarget::class.java, null)
                    }
                }
            }
            controller.registerCallback(callback)
        }

        val alivePackages = controllers.map { it.packageName }
        data.filterKeys { it !in alivePackages }.forEach { (k, _) ->
            data.remove(k)
            LyricReceiver.data.remove(k)
        }
        SmartspacerTargetProvider.notifyChange(this, LyricTarget::class.java)
    }

    private fun loadArt(metaData: MediaMetadata): Bitmap? {
        var art = metaData.getBitmap(MediaMetadata.METADATA_KEY_ART)
        if (art == null) {
            art = loadBitmapFromUri(metaData.getString(MediaMetadata.METADATA_KEY_ART_URI))
        }
        if (art == null) {
            art = metaData.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        }
        if (art == null) {
            art = loadBitmapFromUri(metaData.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI))
        }
        return art
    }

    private fun loadBitmapFromUri(uri: String?): Bitmap? {
        uri ?: return null
        return try {
            contentResolver.openInputStream(Uri.parse(uri))?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from $uri", e)
            null
        }
    }
}