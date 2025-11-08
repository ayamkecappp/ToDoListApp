package com.example.todolistapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.graphics.drawable.ColorDrawable
import android.widget.Button
import android.view.Gravity
import android.media.MediaPlayer // PENTING: Tambahkan ini
import android.view.MotionEvent // PENTING: Tambahkan ini

class FlowTimerActivity : AppCompatActivity() {

    private lateinit var tvTaskName: TextView
    private lateinit var tvTimerDisplay: TextView
    private lateinit var btnPlayPause: ImageView
    private lateinit var npHours: NumberPicker
    private lateinit var npMinutes: NumberPicker
    private lateinit var npSeconds: NumberPicker
    private lateinit var layoutSetDuration: LinearLayout
    private lateinit var btnBack: ImageView
    private lateinit var btnSetDurationOK: Button
    private lateinit var rootConstraintLayout: View // Diambil dari ID yang baru di XML

    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var mediaPlayer: MediaPlayer? = null // PENTING: Deklarasi MediaPlayer

    // Durasi awal - PENTING: Prioritas dari task, baru fallback ke default
    private var totalDurationMillis: Long = 30 * 60 * 1000L
    private var timeRemainingMillis: Long = totalDurationMillis

    private val COLOR_DARK_BLUE = Color.parseColor("#283F6D")

    private val MILLIS_IN_HOUR = 60 * 60 * 1000L
    private val MILLIS_IN_MINUTE = 60 * 1000L
    private val MILLIS_IN_SECOND = 1000L

    companion object {
        const val EXTRA_TASK_NAME = "EXTRA_TASK_NAME"
        const val EXTRA_FLOW_DURATION = "EXTRA_FLOW_DURATION"
        const val DEFAULT_FLOW_DURATION = 30 * 60 * 1000L // 30 menit default
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flow_timer)

        // 1. Ambil Nama Tugas dari Intent
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Flow Task"

        // 2. Ambil durasi SPESIFIK dari Task (jika ada)
        val taskFlowDuration = intent.getLongExtra(EXTRA_FLOW_DURATION, 0L)

        // 3. Tentukan durasi: gunakan durasi task jika > 0, jika tidak gunakan default 30 menit
        val initialDuration = if (taskFlowDuration > 0L) {
            taskFlowDuration
        } else {
            DEFAULT_FLOW_DURATION
        }

        totalDurationMillis = initialDuration.coerceAtLeast(MILLIS_IN_SECOND)
        timeRemainingMillis = totalDurationMillis

        // Hubungkan Views
        tvTaskName = findViewById(R.id.tvTaskName)
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        npHours = findViewById(R.id.npHours)
        npMinutes = findViewById(R.id.npMinutes)
        npSeconds = findViewById(R.id.npSeconds)
        layoutSetDuration = findViewById(R.id.layoutSetDuration)
        btnBack = findViewById(R.id.btnBack)
        btnSetDurationOK = findViewById(R.id.btnSetDurationOK)
        // Hubungkan ke ID yang baru ditambahkan di XML
        rootConstraintLayout = findViewById(R.id.root_constraint_layout)

        tvTaskName.text = taskName

        setupNumberPickers()
        updateTimerDisplay()

        // PENTING: Setup Swipe Listener di root layout
        setupSwipeListener()

        tvTimerDisplay.setOnClickListener {
            if (!isTimerRunning) {
                toggleInputVisibility(true)
            }
        }

        btnSetDurationOK.setOnClickListener {
            updateDurationFromPickers()
            toggleInputVisibility(false)
            Toast.makeText(this, "Durasi diatur ke ${tvTimerDisplay.text}", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnPlayPause.setOnClickListener {
            if (isTimerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }
    }

    // PENTING: Setup Swipe Listener
    private fun setupSwipeListener() {
        // Mengatur listener swipe di root layout
        rootConstraintLayout.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeUp() {
                // LOGIKA UTAMA: Berhenti Alarm saat Swipe Up
                if (mediaPlayer?.isPlaying == true) {
                    stopAlarm()
                    Toast.makeText(this@FlowTimerActivity, "Alarm Stopped", Toast.LENGTH_SHORT).show()
                }
            }
            // Mengonsumsi event agar tidak diteruskan ke elemen lain
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                if (mediaPlayer?.isPlaying == true) {
                    // Jika alarm berbunyi, kita ingin event diserap oleh listener ini
                    super.onTouch(view, motionEvent)
                    return true // Penting untuk mengonsumsi event saat alarm berbunyi
                }
                return super.onTouch(view, motionEvent)
            }
        })
    }

    private fun setupNumberPickers() {
        npHours.minValue = 0
        npHours.maxValue = 24
        npMinutes.minValue = 0
        npMinutes.maxValue = 59
        npSeconds.minValue = 0
        npSeconds.maxValue = 59
    }

    private fun updateDurationFromPickers() {
        val hours = npHours.value
        val minutes = npMinutes.value
        val seconds = npSeconds.value
        totalDurationMillis = (hours * MILLIS_IN_HOUR) + (minutes * MILLIS_IN_MINUTE) + (seconds * MILLIS_IN_SECOND)
        timeRemainingMillis = totalDurationMillis
        updateTimerDisplay()
    }

    private fun startTimer() {
        if (layoutSetDuration.visibility == View.VISIBLE) {
            updateDurationFromPickers()
            toggleInputVisibility(false)
        }

        if (timeRemainingMillis <= 0) {
            Toast.makeText(this, "Mohon set durasi lebih dari 00:00:00", Toast.LENGTH_SHORT).show()
            toggleInputVisibility(true)
            return
        }

        countDownTimer = object : CountDownTimer(timeRemainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMillis = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                timerFinishedFlow()
            }
        }.start()

        isTimerRunning = true
        btnPlayPause.setImageResource(R.drawable.ic_pause)
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        btnPlayPause.setImageResource(R.drawable.ic_play)
    }

    private fun updateTimerDisplay() {
        val hours = (timeRemainingMillis / MILLIS_IN_HOUR).toInt()
        val minutes = ((timeRemainingMillis % MILLIS_IN_HOUR) / MILLIS_IN_MINUTE).toInt()
        val seconds = ((timeRemainingMillis % MILLIS_IN_MINUTE) / MILLIS_IN_SECOND).toInt()

        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        tvTimerDisplay.text = timeFormatted
    }

    private fun toggleInputVisibility(showInput: Boolean) {
        if (showInput) {
            val hours = (timeRemainingMillis / MILLIS_IN_HOUR).toInt()
            val minutes = ((timeRemainingMillis % MILLIS_IN_HOUR) / MILLIS_IN_MINUTE).toInt()
            val seconds = ((timeRemainingMillis % MILLIS_IN_MINUTE) / MILLIS_IN_SECOND).toInt()

            npHours.value = hours
            npMinutes.value = minutes
            npSeconds.value = seconds

            layoutSetDuration.visibility = View.VISIBLE
            btnSetDurationOK.visibility = View.VISIBLE
            btnPlayPause.visibility = View.GONE
            tvTimerDisplay.visibility = View.GONE
        } else {
            updateTimerDisplay()
            layoutSetDuration.visibility = View.GONE
            btnSetDurationOK.visibility = View.GONE
            btnPlayPause.visibility = View.VISIBLE
            tvTimerDisplay.visibility = View.VISIBLE
        }
    }

    private fun timerFinishedFlow() {
        isTimerRunning = false
        btnPlayPause.setImageResource(R.drawable.ic_alarm) // Ubah ikon menjadi alarm

        timeRemainingMillis = 0
        updateTimerDisplay()

        startAlarm() // PENTING: Memulai Alarm

        showDoneDialog()
    }

    // PENTING: Fungsi untuk memulai alarm
    private fun startAlarm() {
        // ASUMSI: File alarm_sound.mp3 ada di res/raw
        try {
            // Jika Anda ingin menggunakan suara bawaan, ganti R.raw.alarm_sound dengan
            // RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
            mediaPlayer?.isLooping = true // Ulangi suara alarm
            mediaPlayer?.start()
            Toast.makeText(this, "Alarm Berbunyi! Swipe ke atas untuk mematikan.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memutar alarm. Pastikan file alarm_sound.mp3 ada di folder res/raw.", Toast.LENGTH_LONG).show()
        }
    }

    // PENTING: Fungsi untuk menghentikan alarm
    private fun stopAlarm() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun buildCustomDialog(context: Context, message: String, positiveText: String, negativeText: String, onPositive: () -> Unit, onNegative: () -> Unit): AlertDialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_save_success, null)

        val tvMessage = dialogView.findViewById<TextView>(R.id.tvMessageTitle)
        val btnNo = dialogView.findViewById<TextView>(R.id.btnIgnore)
        val btnYes = dialogView.findViewById<TextView>(R.id.btnView)

        tvMessage.text = message
        tvMessage.setTextColor(COLOR_DARK_BLUE)
        tvMessage.textSize = 18f
        tvMessage.gravity = Gravity.CENTER

        btnNo.text = negativeText
        btnYes.text = positiveText

        btnNo.setTextColor(COLOR_DARK_BLUE)
        btnYes.setTextColor(COLOR_DARK_BLUE)

        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        btnNo.setOnClickListener {
            dialog.dismiss()
            onNegative()
        }
        btnYes.setOnClickListener {
            dialog.dismiss()
            onPositive()
        }
        return dialog
    }

    private fun showDoneDialog() {
        val dialog = buildCustomDialog(
            this,
            "Are you done with your task?",
            "Yes", "No",
            onPositive = {
                stopAlarm() // Pastikan alarm mati sebelum keluar
                Toast.makeText(this, "Tugas Selesai!", Toast.LENGTH_SHORT).show()
                finish()
            },
            onNegative = {
                showAdjustTimeDialog()
            }
        )
        dialog.show()
    }

    private fun showAdjustTimeDialog() {
        val dialog = buildCustomDialog(
            this,
            "Adjust Time?",
            "Yes", "No",
            onPositive = {
                stopAlarm() // Matikan alarm saat pengguna ingin set waktu baru
                showSetNewTimeDialog()
            },
            onNegative = {
                stopAlarm() // Matikan alarm saat pengguna ingin keluar
                Toast.makeText(this, "Kembali ke daftar tugas.", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
        dialog.show()
    }

    private fun showSetNewTimeDialog() {
        Toast.makeText(this, "Atur durasi baru, lalu tekan OK untuk menyimpan.", Toast.LENGTH_LONG).show()

        val timeToShow = if(totalDurationMillis > 0) totalDurationMillis else 30 * MILLIS_IN_MINUTE

        val initialHours = (timeToShow / MILLIS_IN_HOUR).toInt()
        val initialMinutes = ((timeToShow % MILLIS_IN_HOUR) / MILLIS_IN_MINUTE).toInt()
        val initialSeconds = ((timeToShow % MILLIS_IN_MINUTE) / MILLIS_IN_SECOND).toInt()

        npHours.value = initialHours
        npMinutes.value = initialMinutes
        npSeconds.value = initialSeconds

        toggleInputVisibility(true)
    }

    override fun onBackPressed() {
        if (mediaPlayer?.isPlaying == true) {
            // Jika alarm berbunyi, jangan izinkan back press tanpa mematikan
            Toast.makeText(this, "Swipe ke atas untuk mematikan alarm terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        stopAlarm() // Pastikan MediaPlayer dilepaskan saat Activity dihancurkan
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}