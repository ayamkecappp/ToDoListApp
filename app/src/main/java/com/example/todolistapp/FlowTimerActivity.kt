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
import android.widget.Button // FIX: Import Button

class FlowTimerActivity : AppCompatActivity() {

    private lateinit var tvTaskName: TextView
    private lateinit var tvTimerDisplay: TextView
    private lateinit var btnPlayPause: ImageView
    private lateinit var npMinutes: NumberPicker
    private lateinit var npSeconds: NumberPicker
    private lateinit var layoutSetDuration: LinearLayout
    private lateinit var btnBack: ImageView
    private lateinit var btnSetDurationOK: Button // Variabel untuk tombol OK

    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var totalDurationMillis: Long = 30 * 60 * 1000L // Default 30 minutes
    private var timeRemainingMillis: Long = totalDurationMillis

    // Warna untuk dialog
    private val COLOR_DARK_BLUE = Color.parseColor("#283F6D")

    companion object {
        const val EXTRA_TASK_NAME = "EXTRA_TASK_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flow_timer)

        // Hubungkan Views
        tvTaskName = findViewById(R.id.tvTaskName)
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        npMinutes = findViewById(R.id.npMinutes)
        npSeconds = findViewById(R.id.npSeconds)
        layoutSetDuration = findViewById(R.id.layoutSetDuration)
        btnBack = findViewById(R.id.btnBack)
        btnSetDurationOK = findViewById(R.id.btnSetDurationOK)

        // 1. Ambil Nama Tugas dari Intent
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Flow Task"
        tvTaskName.text = taskName

        // 2. Setup Number Pickers
        setupNumberPickers()

        // Atur tampilan awal
        updateTimerDisplay()

        // Listener untuk menampilkan NumberPicker saat display timer diklik (untuk setting waktu)
        tvTimerDisplay.setOnClickListener {
            if (!isTimerRunning) {
                // Pastikan nilai NumberPicker sesuai dengan waktu saat ini sebelum ditampilkan
                npMinutes.value = (timeRemainingMillis / 60000).toInt()
                npSeconds.value = ((timeRemainingMillis % 60000) / 1000).toInt()
                toggleInputVisibility(true)
            }
        }

        // Listener Tombol OK (Untuk set durasi secara manual)
        btnSetDurationOK.setOnClickListener {
            // 1. Ambil nilai baru, set total/timeRemainingMillis
            updateDurationFromPickers()
            // 2. Sembunyikan input dan tampilkan timer/play button
            toggleInputVisibility(false)
            Toast.makeText(this, "Durasi diatur ke ${tvTimerDisplay.text}", Toast.LENGTH_SHORT).show()
        }

        // Listener Tombol
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

    // --- FUNGSI UTAMA TIMER ---

    private fun setupNumberPickers() {
        npMinutes.minValue = 0
        npMinutes.maxValue = 99
        npSeconds.minValue = 0
        npSeconds.maxValue = 59
    }

    private fun updateDurationFromPickers() {
        val minutes = npMinutes.value
        val seconds = npSeconds.value
        totalDurationMillis = (minutes * 60 * 1000L) + (seconds * 1000L)
        timeRemainingMillis = totalDurationMillis
        updateTimerDisplay()
    }

    private fun startTimer() {
        // PERHATIAN: Jika input durasi terlihat, set nilai baru lalu sembunyikan sebelum mulai
        if (layoutSetDuration.visibility == View.VISIBLE) {
            updateDurationFromPickers()
            toggleInputVisibility(false)
        }

        if (timeRemainingMillis <= 0) {
            Toast.makeText(this, "Mohon set durasi lebih dari 00:00", Toast.LENGTH_SHORT).show()
            toggleInputVisibility(true)
            return
        }

        countDownTimer = object : CountDownTimer(timeRemainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMillis = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                // PENTING: Panggil alur dialog.
                timerFinishedFlow()
            }
        }.start()

        isTimerRunning = true
        btnPlayPause.setImageResource(R.drawable.ic_cancel) // Ikon Pause placeholder
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        btnPlayPause.setImageResource(R.drawable.baseline_arrow_forward_ios_24) // Ikon Play
    }

    private fun updateTimerDisplay() {
        val minutes = (timeRemainingMillis / 1000) / 60
        val seconds = (timeRemainingMillis / 1000) % 60
        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        tvTimerDisplay.text = timeFormatted
    }

    private fun toggleInputVisibility(showInput: Boolean) {
        if (showInput) {
            layoutSetDuration.visibility = View.VISIBLE
            btnSetDurationOK.visibility = View.VISIBLE // Tampilkan tombol OK
            btnPlayPause.visibility = View.GONE
            tvTimerDisplay.visibility = View.GONE
        } else {
            // Set waktu display ke durasi baru setelah di-set
            updateTimerDisplay()
            layoutSetDuration.visibility = View.GONE
            btnSetDurationOK.visibility = View.GONE // Sembunyikan tombol OK
            btnPlayPause.visibility = View.VISIBLE
            tvTimerDisplay.visibility = View.VISIBLE
        }
    }

    // --- LOGIKA DIALOG BERURUTAN ---

    private fun timerFinishedFlow() {
        // Hentikan state dan reset tombol visual
        isTimerRunning = false
        btnPlayPause.setImageResource(R.drawable.baseline_arrow_forward_ios_24) // Reset ke Play

        // Atur display ke 00:00
        timeRemainingMillis = 0
        updateTimerDisplay()

        // Panggil dialog pertama
        showDoneDialog()
    }

    private fun buildCustomDialog(context: Context, message: String, positiveText: String, negativeText: String, onPositive: () -> Unit, onNegative: () -> Unit): AlertDialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_save_success, null)

        val tvMessage = dialogView.findViewById<TextView>(R.id.tvAddReminder)
        val btnNo = dialogView.findViewById<TextView>(R.id.btnIgnore)
        val btnYes = dialogView.findViewById<TextView>(R.id.btnView)

        tvMessage.text = message
        tvMessage.setTextColor(COLOR_DARK_BLUE)
        tvMessage.textSize = 18f

        btnNo.text = negativeText
        btnYes.text = positiveText

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


    // Dialog 1: Are you done with your task?
    private fun showDoneDialog() {
        val dialog = buildCustomDialog(
            this,
            "Are you done with your task?",
            "Yes", "No",
            onPositive = {
                // Yes: Berhenti dan kembali ke TaskActivity
                Toast.makeText(this, "Tugas Selesai!", Toast.LENGTH_SHORT).show()
                finish()
            },
            onNegative = {
                // No: Lanjutkan ke Dialog 2
                showAdjustTimeDialog()
            }
        )
        dialog.show()
    }

    // Dialog 2: Adjust Time?
    private fun showAdjustTimeDialog() {
        val dialog = buildCustomDialog(
            this,
            "Adjust Time?",
            "Yes", "No",
            onPositive = {
                // Yes: Lanjutkan ke Dialog 3 (Set Time)
                showSetNewTimeDialog()
            },
            onNegative = {
                // No: Langsung kembali ke TaskActivity
                Toast.makeText(this, "Kembali ke daftar tugas.", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
        dialog.show()
    }

    // Dialog 3: Set Time (Menggunakan NumberPicker bawaan Activity)
    private fun showSetNewTimeDialog() {
        Toast.makeText(this, "Atur durasi baru, lalu tekan OK untuk menyimpan.", Toast.LENGTH_LONG).show()

        // Set nilai NumberPicker sesuai sisa waktu (jika ada) atau total durasi
        val timeToShow = if(totalDurationMillis > 0) totalDurationMillis else 30 * 60 * 1000L

        val initialMinutes = (timeToShow / 60000).toInt()
        val initialSeconds = ((timeToShow % 60000) / 1000).toInt()

        npMinutes.value = initialMinutes
        npSeconds.value = initialSeconds

        // Tampilkan input durasi
        toggleInputVisibility(true)
    }


    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}