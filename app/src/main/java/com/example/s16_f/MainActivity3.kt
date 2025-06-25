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
    @SuppressLint("MissingInflatedId")

    private var musicback=0.0f
    private var music=0.0f
    private var isMusicEnabled = true
    private var isEffectEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {



        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main3)

        var musicVolume = intent.getFloatExtra("musicVolume", 0.0F)
        var effectVolume = intent.getFloatExtra("effectVolume", 0.0F)
        musicback=musicVolume
        music=effectVolume
        val setbackmusic = findViewById<SeekBar>(R.id.setbackmusic)
        val setmusic = findViewById<SeekBar>(R.id.setmusic)
        val switchback = findViewById<Switch>(R.id.switchback)
        val switchmusic = findViewById<Switch>(R.id.switchmusic)
        val back = findViewById<Button>(R.id.back)

        setbackmusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                musicVolume= (musicVolume+ 0.1).toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        setmusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                effectVolume= (effectVolume+ 0.1).toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        back.setOnClickListener {
            settingmusic()
        }






    }
    private fun settingmusic(){
        val intent= Intent(this,MainActivity::class.java).apply {
            putExtra("musicvolume",musicback)
            putExtra("effectVolume",music)
        }
        startActivity(intent)

    }
}