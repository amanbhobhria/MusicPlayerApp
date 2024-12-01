package com.example.musicplayerapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.OptIn

import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicplayerapp.ui.theme.music.MyMusicActivity

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    private val trackList = listOf(
        Pair("Track 1", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
        Pair("Track 2", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
        Pair("Track 3", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3")
    )
    private var currentTrackIndex = 0
    private val binder = LocalBinder()

    companion object {
        const val CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 1
    }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        player = ExoPlayer.Builder(this).build().apply {

            setMediaItem(MediaItem.fromUri(trackList[currentTrackIndex].second))

            Log.d("music33", "Started song " + trackList[currentTrackIndex].first)
            Log.d("music33", "Is playing1 = " + player?.isPlaying)
            prepare()
            play()
        }
        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(createSessionActivity())
            .build()

        startForeground(NOTIFICATION_ID, createNotification())




        }


    private fun createSessionActivity(): PendingIntent {
        val intent = Intent(this, MyMusicActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    fun updateNotification() {
        startForeground(NOTIFICATION_ID, createNotification())
    }



    fun createNotification(): Notification {
        Log.d("music33","In service player.play is  "+player?.isPlaying )
        val playPauseAction = if (player?.isPlaying == true) {

            NotificationCompat.Action(
                R.drawable.baseline_pause_24,
                "Pause",
                createActionIntent("ACTION_PAUSE")
            )

        } else {
            NotificationCompat.Action(
                R.drawable.baseline_play_arrow_24,
                "Play",
                createActionIntent("ACTION_PLAY")
            )
        }

        val nextAction = NotificationCompat.Action(
            R.drawable.baseline_skip_next_24,
            "Next",
            createActionIntent("ACTION_NEXT")
        )

        val prevAction = NotificationCompat.Action(
            R.drawable.baseline_skip_previous_24,
            "Previous",
            createActionIntent("ACTION_PREVIOUS")
        )

        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession?.sessionCompatToken)
            .setShowActionsInCompactView(0, 1, 2)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Player")
            .setContentText("Now Playing: ${trackList[currentTrackIndex].first}")
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(mediaStyle)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player Notifications",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            "ACTION_PLAY" -> {player?.play()
                Log.d("music33", "in onstartCommand song ACTION PLAY"  )
                startForeground(NOTIFICATION_ID, createNotification())
            }
            "ACTION_PAUSE" ->{ player?.pause()
                Log.d("music33", "in onstartCommand song ACTION PAUSE"  )
                startForeground(NOTIFICATION_ID, createNotification())
            }
            "ACTION_NEXT" -> {skipToNext()
                Log.d("music33", "in onstartCommand song ACTION NEXT"  )
                startForeground(NOTIFICATION_ID, createNotification())
            }
            "ACTION_PREVIOUS" -> {skipToPrevious()
                Log.d("music33", "in onstartCommand song ACTION PREV"  )
                startForeground(NOTIFICATION_ID, createNotification())}
        }

        Log.d("music33", "in onstartCommand song " )
        startForeground(NOTIFICATION_ID, createNotification())

        return START_STICKY
    }

    fun skipToNext() {
        if (currentTrackIndex < trackList.size - 1) {
            currentTrackIndex++
            playTrack(currentTrackIndex)
        }
    }

    fun skipToPrevious() {
        if (currentTrackIndex > 0) {
            currentTrackIndex--
            playTrack(currentTrackIndex)
        }
    }

    private fun playTrack(index: Int) {
        player?.setMediaItem(MediaItem.fromUri(trackList[index].second))
        player?.prepare()
        player?.play()
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }


    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        TODO("Not yet implemented")
    }

    fun getPlayer(): ExoPlayer? = player

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        player?.release()
    }

    fun getCurrentTrackIndex(): Int {
        return currentTrackIndex
    }


    fun getTrackName(index: Int): String {
        return trackList[index].first
    }



}

