package com.lucrecioz.quiettime

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var chipSeg: Chip
    private lateinit var chipTer: Chip
    private lateinit var chipQua: Chip
    private lateinit var chipQui: Chip
    private lateinit var chipSex: Chip
    private lateinit var chipSab: Chip
    private lateinit var chipDom: Chip

    private val PREFS = "quiettime_prefs"
    private val KEY_ATIVO = "quiettime_ativo"

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pedirPermissaoDND()
        pedirPermissaoNotificacao()

        val inicio = findViewById<TimePicker>(R.id.timeInicio)
        val fim = findViewById<TimePicker>(R.id.timeFim)
        val btnAgendar = findViewById<MaterialButton>(R.id.btnAgendar)

        // ===== CHIPS =====
        chipSeg = findViewById(R.id.chipSeg)
        chipTer = findViewById(R.id.chipTer)
        chipQua = findViewById(R.id.chipQua)
        chipQui = findViewById(R.id.chipQui)
        chipSex = findViewById(R.id.chipSex)
        chipSab = findViewById(R.id.chipSab)
        chipDom = findViewById(R.id.chipDom)


        atualizarBotao(btnAgendar)

        btnAgendar.setOnClickListener {

            //  desativar
            if (isQuietTimeAtivo()) {
                desativarQuietTime()
                setQuietTimeAtivo(false)
                atualizarBotao(btnAgendar)

                Toast.makeText(this, "QuieTime desativado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ativar
            val dias = diasSelecionados()
            if (dias.isEmpty()) {
                Toast.makeText(this, "Selecione ao menos um dia!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            agendarHorario(
                getHora(inicio),
                getMinuto(inicio),
                getHora(fim),
                getMinuto(fim)
            )

            setQuietTimeAtivo(true)
            atualizarBotao(btnAgendar)

            Toast.makeText(this, "QuieTime ativado", Toast.LENGTH_SHORT).show()
        }
    }


    private fun atualizarBotao(btn: MaterialButton) {
        if (isQuietTimeAtivo()) {
            btn.text = "Desativar QuieTime"
            btn.setBackgroundColor(
                ContextCompat.getColor(this, R.color.qt_success)
            )
        } else {
            btn.text = "Ativar QuieTime"
            btn.setBackgroundColor(
                ContextCompat.getColor(this, R.color.qt_primary)
            )
        }
    }

    // desativar

    private fun desativarQuietTime() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (dia in 1..7) {

            val inicioIntent = Intent(this, ModoReceiver::class.java)
            val fimIntent = Intent(this, ModoReceiver::class.java)

            val inicioPending = PendingIntent.getBroadcast(
                this,
                dia * 10,
                inicioIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val fimPending = PendingIntent.getBroadcast(
                this,
                dia * 10 + 1,
                fimIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(inicioPending)
            alarmManager.cancel(fimPending)
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

    // prefs

    private fun setQuietTimeAtivo(ativo: Boolean) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ATIVO, ativo)
            .apply()
    }

    private fun isQuietTimeAtivo(): Boolean {
        return getSharedPreferences(PREFS, MODE_PRIVATE)
            .getBoolean(KEY_ATIVO, false)
    }

    // ================== PERMISSÕES ==================

    private fun pedirPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun pedirPermissaoDND() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    // ================== TIME PICKER ==================

    private fun getHora(tp: TimePicker): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) tp.hour else tp.currentHour

    private fun getMinuto(tp: TimePicker): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) tp.minute else tp.currentMinute

    // ================== DIAS ==================

    private fun diasSelecionados(): List<Int> {
        val dias = mutableListOf<Int>()

        if (chipSeg.isChecked) dias.add(Calendar.MONDAY)
        if (chipTer.isChecked) dias.add(Calendar.TUESDAY)
        if (chipQua.isChecked) dias.add(Calendar.WEDNESDAY)
        if (chipQui.isChecked) dias.add(Calendar.THURSDAY)
        if (chipSex.isChecked) dias.add(Calendar.FRIDAY)
        if (chipSab.isChecked) dias.add(Calendar.SATURDAY)
        if (chipDom.isChecked) dias.add(Calendar.SUNDAY)

        return dias
    }

    // agendamentos

    private fun agendarHorario(
        horaInicio: Int,
        minInicio: Int,
        horaFim: Int,
        minFim: Int
    ) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (dia in diasSelecionados()) {

            val inicioCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dia)
                set(Calendar.HOUR_OF_DAY, horaInicio)
                set(Calendar.MINUTE, minInicio)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (before(Calendar.getInstance())) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val inicioIntent = Intent(this, ModoReceiver::class.java)
                .putExtra("modo", "silencioso")

            val inicioPending = PendingIntent.getBroadcast(
                this,
                dia * 10,
                inicioIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    inicioCal.timeInMillis,
                    inicioPending
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    inicioCal.timeInMillis,
                    inicioPending
                )
            }

            val fimCal = Calendar.getInstance().apply {
                timeInMillis = inicioCal.timeInMillis
                set(Calendar.HOUR_OF_DAY, horaFim)
                set(Calendar.MINUTE, minFim)

                if (before(inicioCal)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val fimIntent = Intent(this, ModoReceiver::class.java)
                .putExtra("modo", "som")

            val fimPending = PendingIntent.getBroadcast(
                this,
                dia * 10 + 1,
                fimIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    fimCal.timeInMillis,
                    fimPending
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    fimCal.timeInMillis,
                    fimPending
                )
            }
        }
    }
}
