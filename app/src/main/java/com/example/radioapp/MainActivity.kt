package com.example.radioapp

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RadioStationAdapter
    private lateinit var tvCurrentStation: TextView
    private lateinit var tvStationDescription: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnPlayStop: FloatingActionButton
    private lateinit var btnPrevious: FloatingActionButton
    private lateinit var btnNext: FloatingActionButton
    private lateinit var btnSleepTimer: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var seekBarVolume: SeekBar
    private lateinit var tvVolumePercent: TextView
    private lateinit var tvSleepTimer: TextView

    private lateinit var favoriteManager: FavoriteManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var radioDataManager: RadioDataManager

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var currentStation: RadioStation? = null
    private var isPlaying = false
    private var currentStationIndex: Int = -1

    private var sleepTimerHandler: Handler? = null
    private var sleepTimerRunnable: Runnable? = null
    private var sleepTimerEndTime: Long = 0

    private var radioConfig: RadioConfig? = null
    private var currentCategory: Category? = null
    private val NOTIFICATION_PERMISSION_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Manager'ları başlat
        favoriteManager = FavoriteManager(this)
        preferencesManager = PreferencesManager(this)
        radioDataManager = RadioDataManager(this)

        // View'ları bağla
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        recyclerView = findViewById(R.id.recyclerViewStations)
        tvCurrentStation = findViewById(R.id.tvCurrentStation)
        tvStationDescription = findViewById(R.id.tvStationDescription)
        tvStatus = findViewById(R.id.tvStatus)
        btnPlayStop = findViewById(R.id.btnPlayStop)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        btnSleepTimer = findViewById(R.id.btnSleepTimer)
        progressBar = findViewById(R.id.progressBar)
        seekBarVolume = findViewById(R.id.seekBarVolume)
        tvVolumePercent = findViewById(R.id.tvVolumePercent)
        tvSleepTimer = findViewById(R.id.tvSleepTimer)

        // Bildirim izni iste (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }

        // MediaController'ı bağla
        initializeController()

        // Radyo verilerini yükle
        loadRadioData()

        // Buton listeners
        btnPlayStop.setOnClickListener {
            currentStation?.let { station ->
                if (isPlaying) {
                    stopPlayback()
                } else {
                    playStation(station)
                }
            }
        }

        btnPrevious.setOnClickListener {
            playPreviousStation()
        }

        btnNext.setOnClickListener {
            playNextStation()
        }

        btnSleepTimer.setOnClickListener {
            showSleepTimerDialog()
        }

        // Ses kontrolü
        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val volume = progress / 100f
                    mediaController?.volume = volume
                    tvVolumePercent.text = "$progress%"
                    preferencesManager.saveVolume(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        updateUI()
    }

    private fun loadRadioData() {
        lifecycleScope.launch {
            tvStatus.text = "Radyo listesi yükleniyor..."
            tvStatus.visibility = View.VISIBLE

            val config = radioDataManager.getRadioData()

            if (config != null && config.categories.isNotEmpty()) {
                radioConfig = config
                setupCategories(config.categories)
                tvStatus.visibility = View.GONE
            } else {
                tvStatus.text = "Radyo listesi yüklenemedi"
                // Fallback: Eski sistem
                setupOldSystem()
            }
        }
    }

    private fun setupCategories(categories: List<Category>) {
        chipGroupCategories.removeAllViews()

        categories.forEach { category ->
            val chip = Chip(this).apply {
                text = category.name
                isCheckable = true
                setOnClickListener {
                    showCategoryStations(category)
                }
            }
            chipGroupCategories.addView(chip)
        }

        // İlk kategoriyi seç
        if (categories.isNotEmpty()) {
            (chipGroupCategories.getChildAt(0) as Chip).isChecked = true
            showCategoryStations(categories[0])
        }
    }

    private fun showCategoryStations(category: Category) {
        currentCategory = category
        setupRecyclerView(category.stations)
    }

    private fun setupRecyclerView(stations: List<RadioStation>) {
        val sortedStations = stations.sortedByDescending { station ->
            favoriteManager.isFavorite(station.id)
        }

        adapter = RadioStationAdapter(
            stations = sortedStations,
            favoriteManager = favoriteManager,
            onStationClick = { station ->
                currentStation = station
                tvCurrentStation.text = station.name
                tvStationDescription.text = station.description

                currentStationIndex = sortedStations.indexOfFirst { it.id == station.id }
                playStation(station)
                updateUI()
            },
            onFavoriteClick = { station ->
                favoriteManager.toggleFavorite(station.id)
                currentCategory?.let { showCategoryStations(it) }

                currentStation?.let {
                    currentStationIndex = adapter.stations.indexOfFirst { s -> s.id == it.id }
                }
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupOldSystem() {
        // Eski RadioStations sistemi
        tvStatus.visibility = View.GONE
        chipGroupCategories.visibility = View.GONE
        setupRecyclerView(RadioStations.stations)
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            this,
            ComponentName(this, RadioService::class.java)
        )

        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()

            val currentVolume = (mediaController?.volume ?: 1f) * 100
            seekBarVolume.progress = currentVolume.toInt()
            tvVolumePercent.text = "${currentVolume.toInt()}%"

            mediaController?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> showLoading()
                        Player.STATE_READY -> {
                            hideLoading()
                            if (mediaController?.playWhenReady == true) {
                                showPlaying()
                            }
                        }
                        Player.STATE_ENDED, Player.STATE_IDLE -> {
                            hideLoading()
                            showStopped()
                        }
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    if (playing) {
                        showPlaying()
                    } else {
                        showStopped()
                    }
                }
            })

            loadLastStation()
        }, MoreExecutors.directExecutor())
    }

    private fun playStation(station: RadioStation) {
        mediaController?.apply {
            stop()
            clearMediaItems()

            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(station.name)
                .setArtist(station.description)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(station.url)
                .setMediaMetadata(mediaMetadata)
                .build()

            setMediaItem(mediaItem)
            prepare()
            play()
        }

        preferencesManager.saveLastStation(station.id)
    }

    private fun stopPlayback() {
        mediaController?.stop()
        adapter.clearSelection()
        currentStation = null
        tvCurrentStation.text = "Radyo seçiniz"
        tvStationDescription.text = ""
        isPlaying = false
        showStopped()
    }

    private fun playPreviousStation() {
        if (currentStationIndex > 0) {
            currentStationIndex--
            val station = adapter.stations[currentStationIndex]
            currentStation = station
            tvCurrentStation.text = station.name
            tvStationDescription.text = station.description
            playStation(station)
            adapter.setSelectedStation(station.id)
            recyclerView.smoothScrollToPosition(currentStationIndex)
            updateUI()
        }
    }

    private fun playNextStation() {
        if (currentStationIndex < adapter.stations.size - 1) {
            currentStationIndex++
            val station = adapter.stations[currentStationIndex]
            currentStation = station
            tvCurrentStation.text = station.name
            tvStationDescription.text = station.description
            playStation(station)
            adapter.setSelectedStation(station.id)
            recyclerView.smoothScrollToPosition(currentStationIndex)
            updateUI()
        }
    }

    private fun loadLastStation() {
        val lastStationId = preferencesManager.getLastStationId()
        if (lastStationId != -1) {
            // Tüm kategorilerde ara
            radioConfig?.categories?.forEach { category ->
                val station = category.stations.find { it.id == lastStationId }
                station?.let {
                    currentStation = it
                    tvCurrentStation.text = it.name
                    tvStationDescription.text = it.description

                    val lastVolume = preferencesManager.getLastVolume()
                    seekBarVolume.progress = lastVolume
                    tvVolumePercent.text = "$lastVolume%"

                    if (preferencesManager.isAutoPlayEnabled()) {
                        playStation(it)
                    }

                    updateUI()
                    return@forEach
                }
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Yükleniyor..."
        tvStatus.visibility = View.VISIBLE
        btnPlayStop.isEnabled = false
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnPlayStop.isEnabled = true
    }

    private fun showPlaying() {
        tvStatus.text = "▶ Çalıyor"
        tvStatus.visibility = View.VISIBLE
        btnPlayStop.setImageResource(android.R.drawable.ic_media_pause)
        updateUI()
    }

    private fun showStopped() {
        tvStatus.visibility = View.GONE
        btnPlayStop.setImageResource(android.R.drawable.ic_media_play)
        updateUI()
    }

    private fun updateUI() {
        val hasStation = currentStation != null
        btnPlayStop.isEnabled = hasStation

        btnPrevious.isEnabled = hasStation && currentStationIndex > 0
        btnNext.isEnabled = hasStation && currentStationIndex < adapter.stations.size - 1
    }

    private fun showSleepTimerDialog() {
        val dialog = SleepTimerDialog(this) { minutes ->
            if (minutes > 0) {
                startSleepTimer(minutes)
            } else {
                cancelSleepTimer()
            }
        }
        dialog.show()
    }

    private fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()

        sleepTimerEndTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        sleepTimerHandler = Handler(Looper.getMainLooper())

        sleepTimerRunnable = object : Runnable {
            override fun run() {
                val remainingTime = sleepTimerEndTime - System.currentTimeMillis()

                if (remainingTime <= 0) {
                    stopPlayback()
                    cancelSleepTimer()
                } else {
                    val remainingMinutes = (remainingTime / 1000 / 60).toInt()
                    val remainingSeconds = ((remainingTime / 1000) % 60).toInt()
                    tvSleepTimer.text = "⏰ Kalan: ${remainingMinutes}:${String.format("%02d", remainingSeconds)}"
                    tvSleepTimer.visibility = View.VISIBLE
                    sleepTimerHandler?.postDelayed(this, 1000)
                }
            }
        }

        sleepTimerRunnable?.let { sleepTimerHandler?.post(it) }
    }

    private fun cancelSleepTimer() {
        sleepTimerRunnable?.let { sleepTimerHandler?.removeCallbacks(it) }
        sleepTimerHandler = null
        sleepTimerRunnable = null
        sleepTimerEndTime = 0
        tvSleepTimer.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                showSettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val checkBoxAutoPlay = dialogView.findViewById<CheckBox>(R.id.checkBoxAutoPlay)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseSettings)

        checkBoxAutoPlay.isChecked = preferencesManager.isAutoPlayEnabled()

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        checkBoxAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setAutoPlay(isChecked)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, RadioService::class.java).apply {
            action = RadioService.ACTION_HIDE_NOTIFICATION
        }
        startService(intent)
    }

    override fun onStop() {
        super.onStop()
        if (isPlaying) {
            val intent = Intent(this, RadioService::class.java).apply {
                action = RadioService.ACTION_SHOW_NOTIFICATION
            }
            startService(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelSleepTimer()
        mediaController?.release()
        MediaController.releaseFuture(controllerFuture!!)
    }
}