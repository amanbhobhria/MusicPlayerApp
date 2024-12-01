package com.example.musicplayerapp.ui.theme.music

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat


import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicplayerapp.PlaybackService
import com.example.musicplayerapp.R

import com.example.musicplayerapp.databinding.ActivityMyMusicBinding

@OptIn(UnstableApi::class)
class MyMusicActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyMusicBinding
    private var player: ExoPlayer? = null
    private var playbackService: PlaybackService? = null
    private var isServiceBound = false
    private val handler = Handler(Looper.getMainLooper())

    private var currentTrackIndex = 0

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            playbackService = (service as PlaybackService.LocalBinder).getService()
            player = playbackService?.getPlayer()
            isServiceBound = true
            setupPlayerListener()
            updateTrackInfo()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            player = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()
        bindToPlaybackService()
        setupUI()
    }

    private fun bindToPlaybackService() {
        val intent = Intent(this, PlaybackService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setupUI() {
        binding.btnPlayPause.setOnClickListener {

            if (player?.isPlaying == true) {
                player?.pause()
                binding.btnPlayPause.setImageResource(R.drawable.baseline_play_arrow_24)

            } else {
                player?.play()
                binding.btnPlayPause.setImageResource(R.drawable.baseline_pause_24)
            }

            playbackService?.updateNotification()
        }

        binding.btnNext.setOnClickListener { playNextTrack()
            playbackService?.updateNotification()}
        binding.btnPrevious.setOnClickListener { playPreviousTrack()
            playbackService?.updateNotification()}

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player?.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        playbackService?.updateNotification()
    }

    private fun setupPlayerListener() {
        player?.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {

                if (playbackState == Player.STATE_READY) {
                    updateTrackInfo()
                } else if (playbackState == Player.STATE_ENDED) {
                    playNextTrack()
                }



            }
        })

        handler.post(updateSeekBarRunnable)
    }

    private fun updateTrackInfo() {

        playbackService?.let {
            currentTrackIndex = it.getCurrentTrackIndex()
            binding.tvTrackName.text = it.getTrackName(currentTrackIndex)
            val duration = player?.duration ?: 0L
            if (duration > 0) {
                binding.tvTotalDuration.text = formatTime(duration)
                binding.seekBar.max = duration.toInt()
            } else {
                binding.tvTotalDuration.text = "00:00"
                binding.seekBar.max = 0
            }


        }
    }

    private fun playNextTrack() {
        playbackService?.skipToNext()
        updateTrackInfo()
    }

    private fun playPreviousTrack() {
        playbackService?.skipToPrevious()
        updateTrackInfo()
    }

    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            player?.let {
                if (it.isPlaying) {
                    binding.seekBar.progress = it.currentPosition.toInt()
                    binding.tvElapsedTime.text = formatTime(it.currentPosition)
                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PlaybackService.CHANNEL_ID,
                "Music Player Notifications",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBarRunnable)
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}




