package com.example.s16_f

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity3 : AppCompatActivity() {
    private var musicback = 0.7f
    private var music = 0.7f
    private var isMusicEnabled = false
    private var isEffectEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)




        musicback = intent.getFloatExtra("musicvolume", 0.7f)
        music = intent.getFloatExtra("effectvolume", 0.7f)
        isMusicEnabled = intent.getBooleanExtra("isMusicEnabled", false)
        isEffectEnabled = intent.getBooleanExtra("isEffectEnabled", false)

        val setbackmusic = findViewById<SeekBar>(R.id.setbackmusic)
        val setmusic = findViewById<SeekBar>(R.id.setmusic)
        val switchback = findViewById<Switch>(R.id.switchback)
        val switchmusic = findViewById<Switch>(R.id.switchmusic)
        val back = findViewById<Button>(R.id.back)


        switchback.isChecked = isMusicEnabled
        switchmusic.isChecked = isEffectEnabled
        setbackmusic.progress = (musicback.coerceIn(0f, 1f) * 100).toInt()
        setmusic.progress = (music.coerceIn(0f, 1f) * 100).toInt()


        setbackmusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                musicback = progress / 100.0f
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        setmusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                music = progress / 100.0f
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        switchback.setOnCheckedChangeListener { _, isChecked ->
            isMusicEnabled = isChecked
        }

        switchmusic.setOnCheckedChangeListener { _, isChecked ->
            isEffectEnabled = isChecked
        }


        back.setOnClickListener {
            settingmusic()
            finish()
        }
    }


    private fun settingmusic() {
        val resultIntent = Intent().apply {
            putExtra("musicvolume", musicback)
            putExtra("effectvolume", music)
            putExtra("isMusicEnabled", isMusicEnabled)  // 返回当前 Switch 状态
            putExtra("isEffectEnabled", isEffectEnabled)  // 返回当前 Switch 状态
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }


    override fun onBackPressed() {
        settingmusic()
        super.onBackPressed()
    }
}