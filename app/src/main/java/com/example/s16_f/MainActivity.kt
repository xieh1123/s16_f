package com.example.s16_f


import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import  android.content.ContentValues
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


data class RaceResult(
    val id: Long = 0,
    var winner: String,
    var rabbit: Int,
    var turtle: Int,
    var timestamps: String
)

class MainActivity : AppCompatActivity() {
    //SQL
    private lateinit var dbHelper: RaceDatabaseHelper


    private lateinit var spnMatches: Spinner
    private var best0f = 1
    private var rabbitwin = 0
    private var turtlewin = 0
    private lateinit var setting:Button

    private lateinit var adapter: RaceResultAdapter
    private val raceResults = mutableListOf<RaceResult>()
    private lateinit var rvHistory: RecyclerView

    // 建立兩個數值，用於計算兔子與烏龜的進度
    private var progressRabbit = 0
    private var progressTurtle = 0

    //速度調整變數
    private lateinit var sbrabbitspeed: SeekBar
    private lateinit var sbturtlespeed: SeekBar
    private lateinit var tvrabbitspeed: TextView
    private lateinit var tvturulespeed: TextView
    private var turtlespeed = 1
    private var rabbitspeed = 3
    private var rabbitSleepProb = 0.66


    // 建立變數以利後續綁定元件
    private lateinit var btnStart: Button
    private lateinit var sbRabbit: SeekBar
    private lateinit var sbTurtle: SeekBar

    //動畫
    private lateinit var ivRabbit: ImageView
    private lateinit var ivTurtle: ImageView
    private var rabbitAnimator: ObjectAnimator? = null
    private var turtleAnimator: ObjectAnimator? = null

    // 音樂播放相關變數
    private var backgroundMusic: MediaPlayer? = null
    private var winSound: MediaPlayer? = null
    private var startSound: MediaPlayer? = null
    private var musicVolume = 0.7f // 音樂音量 (0.0 - 1.0)
    private var effectVolume = 1.0f // 音效音量 (0.0 - 1.0)
    private var isMusicEnabled = true
    private var isEffectEnabled = true

    private lateinit var tvEmptyState: TextView

    private lateinit var btnCleanHistory: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvEmptyState = findViewById(R.id.tvEmptyState)

        //spinner
        spnMatches = findViewById(R.id.spnMatches)
        setupspn()

        //seekbar
        sbrabbitspeed = findViewById(R.id.sbRabbitSpeed)
        sbturtlespeed = findViewById(R.id.sbTurtleSpeed)
        tvrabbitspeed = findViewById(R.id.tvRabbitSpeedValue)
        tvturulespeed = findViewById(R.id.tvTurtleSpeedValue)
        setupSpeedControls()

        //動畫
        ivRabbit = findViewById(R.id.ivRabbit)
        ivTurtle = findViewById(R.id.ivTurtle)


        //
        rvHistory = findViewById(R.id.rvHistory)
        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = RaceResultAdapter(raceResults)
        rvHistory.adapter = adapter


        //初始畫音樂
        setupMusic()
        setting=findViewById(R.id.setting)

        // 將變數與XML元件綁定
        btnStart = findViewById(R.id.btnStart)
        sbRabbit = findViewById(R.id.sbRabbit)
        sbTurtle = findViewById(R.id.sbTurtle)
        // 對開始按鈕設定監聽器
        btnStart.setOnClickListener {
            setupRaceSeekBar()
            startRace()
        }

        dbHelper = RaceDatabaseHelper(this)
        btnCleanHistory= findViewById(R.id.btnCleanHistory)

        btnCleanHistory.setOnClickListener {
            dbHelper.clearAllRecords()
        }
    }


    private fun loadHistoryFromDatabase() {
        try {
            Log.d("MainActivity", "正在從資料庫載入歷史記錄...")

            // 清空本地列表
            raceResults.clear()

            // 從資料庫取得所有記錄
            val dbRecords = dbHelper.getAllRaceRecords()
            raceResults.addAll(dbRecords)

            // 通知轉接器資料已變更
            adapter.notifyDataSetChanged()

            // 更新空狀態顯示
            updateEmptyState()

            Log.d("MainActivity", "歷史記錄載入完成，共載入 ${raceResults.size} 筆記錄")
        } catch (e: Exception) {
            Log.e("MainActivity", "載入歷史記錄時發生錯誤: ${e.message}", e)
            showToast("載入歷史記錄失敗")
        }
    }


    private fun updateEmptyState() {
        if (raceResults.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
        }
    }


    override fun onPause() {
        super.onPause()
        // 暫停背景音樂
        stopBackgroundMusic()
    }

    override fun onResume() {
        super.onResume()
        // 如果比賽正在進行，恢復背景音樂
        if (!btnStart.isEnabled) {  // 比賽進行中的判斷條件
            playBackgroundMusic()
        }
        loadHistoryFromDatabase()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 釋放音樂資源
        try {
            backgroundMusic?.release()
            winSound?.release()
            startSound?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            dbHelper.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupMusic() {
        try {
            // 初始化背景音樂
            backgroundMusic = MediaPlayer.create(this, R.raw.background)
            backgroundMusic?.isLooping = true  // 設定為循環播放

            // 初始化音效
            winSound = MediaPlayer.create(this, R.raw.win)
            startSound = MediaPlayer.create(this, R.raw.start)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun playBackgroundMusic() {
        try {
            backgroundMusic?.let {
                if (!it.isPlaying) {
                    it.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopBackgroundMusic() {
        try {
            backgroundMusic?.let {
                if (it.isPlaying) {
                    it.pause()  // 使用pause而非stop，保持播放位置
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playSound(soundPlayer: MediaPlayer?) {
        try {
            soundPlayer?.let {
                if (it.isPlaying) {
                    it.seekTo(0)  // 如果正在播放，重新開始
                } else {
                    it.start()    // 開始播放
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //音樂聲量
    private fun setMusicVolume(volume: Float) {
        try {
            backgroundMusic?.setVolume(volume, volume)
            musicVolume = volume
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun setEffectVolume(volume: Float) {
        try {
            winSound?.setVolume(volume, volume)
            startSound?.setVolume(volume, volume)
            effectVolume = volume
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun toggleMusic() {
        isMusicEnabled = !isMusicEnabled
        if (!isMusicEnabled) {
            stopBackgroundMusic()
        } else if (!btnStart.isEnabled) {
            playBackgroundMusic()
        }
    }
    fun toggleEffects() {
        isEffectEnabled = !isEffectEnabled
    }






    private fun startRace() {
        // 進行賽跑後按鈕不可被操作
        btnStart.isEnabled = false
        // 初始化兔子的賽跑進度
        progressRabbit = 0
        // 初始化烏龜的賽跑進度
        progressTurtle = 0
        // 初始化兔子的SeekBar進度
        sbRabbit.progress = 0
        // 初始化烏龜的SeekBar進度
        sbTurtle.progress = 0

        //重製emoji位置
        resetanimalPosition()
        //開始按鈕動畫
        animaReStartButton()


        //開始播放音樂
        playSound(startSound)
        playBackgroundMusic()

        Handler(Looper.getMainLooper()).postDelayed({
            //兔子起跑
            runRabbit()
            // 烏龜起跑
            runTurtle()
        }, 1000)
    }

    private fun resetanimalPosition() {
        ivRabbit.translationX = 0f
        ivTurtle.translationX = 0f
        ivRabbit.scaleX = 1f
        ivRabbit.scaleY = 1f
        ivTurtle.scaleX = 1f
        ivTurtle.scaleY = 1f
    }

    private fun animaReStartButton() {
        val scaleX = ObjectAnimator.ofFloat(btnStart, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(btnStart, "scaleY", 1f, 1.2f, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.interpolator = BounceInterpolator()
        animatorSet.start()

    }

    private fun animateRabbit(progress: Int) {
        val maxWidth = resources.displayMetrics.widthPixels - ivRabbit.width
        val targetX = (progress / 100f) * maxWidth

        rabbitAnimator?.cancel()
        rabbitAnimator =
            ObjectAnimator.ofFloat(ivRabbit, "translationX", ivRabbit.translationX, targetX)
        rabbitAnimator?.duration = 200
        rabbitAnimator?.interpolator = AccelerateDecelerateInterpolator()

        val jumpY = ObjectAnimator.ofFloat(ivRabbit, "translationY", 0f, -20f, 0f)
        jumpY.duration = 200
        jumpY.interpolator = AccelerateDecelerateInterpolator()

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(rabbitAnimator, jumpY)
        animatorSet.start()

        if (Math.random() < rabbitSleepProb) {
            Handler(Looper.getMainLooper()).postDelayed({
                animateSleeping()
            }, 300)

        }
    }

    private fun animateSleeping() {
        val sleepAnimator = ObjectAnimator.ofFloat(ivRabbit, "rotation", 0f, -15f, 0f)
        sleepAnimator.duration = 600
        sleepAnimator.repeatCount = 2
        sleepAnimator.start()
    }

    private fun animateTurtle(progress: Int) {
        val maxWidth = resources.displayMetrics.widthPixels - ivTurtle.width
        val targetX = (progress / 100f) * maxWidth

        turtleAnimator?.cancel()
        turtleAnimator =
            ObjectAnimator.ofFloat(ivTurtle, "translationX", ivTurtle.translationX, targetX)
        turtleAnimator?.duration = 300
        turtleAnimator?.interpolator = AccelerateDecelerateInterpolator()
        turtleAnimator?.start()

        val wiggle = ObjectAnimator.ofFloat(ivTurtle, "rotation", -2f, 2f, -2f)
        wiggle.duration = 400
        wiggle.start()
    }


    private fun setupspn() {
        val matchOption = arrayOf("1戰1勝", "3戰2勝", "5戰3勝", "7戰4勝")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, matchOption)
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        spnMatches.adapter = adapter
        spnMatches.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                best0f = when (position) {
                    0 -> 1
                    1 -> 3
                    2 -> 5
                    3 -> 7
                    else -> 1
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                best0f = 1
            }
        }

    }

    private fun resetWinCount() {
        rabbitwin = 0
        turtlewin = 0
        showToast("以調整至${best0f}")
    }


    private fun setupSpeedControls() {
        //兔子SeekBar顏色
        sbrabbitspeed.progressDrawable.setColorFilter(
            Color.RED,
            android.graphics.PorterDuff.Mode.SRC_IN
        )
        sbrabbitspeed.thumb.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN)
        //烏龜SeekBar顏色
        sbturtlespeed.progressDrawable.setColorFilter(
            Color.GREEN,
            android.graphics.PorterDuff.Mode.SRC_IN
        )
        sbturtlespeed.thumb.setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN)
        //speed
        sbrabbitspeed.max = 9//最大值為10(0~9+1)
        sbrabbitspeed.progress = 2//初始值3
        sbturtlespeed.max = 9//最大值為10(0~9+1)
        sbturtlespeed.progress = 0////初始值1

        rabbitspeed = 3
        turtlespeed = 1
        tvrabbitspeed.text = "$rabbitspeed"
        tvturulespeed.text = "$turtlespeed"


        //Seekbar for rabbit
        sbrabbitspeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                rabbitspeed = p1 + 1
                tvrabbitspeed.text = "$rabbitspeed"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })

        sbturtlespeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                turtlespeed = p1 + 1
                tvturulespeed.text = "$turtlespeed"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })
    }

    private fun setupRaceSeekBar() {
        sbRabbit.progressDrawable.setColorFilter(
            Color.RED,
            android.graphics.PorterDuff.Mode.SRC_IN
        )
        sbRabbit.thumb.setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN)

        sbTurtle.progressDrawable.setColorFilter(
            Color.GREEN,
            android.graphics.PorterDuff.Mode.SRC_IN
        )
        sbTurtle.thumb.setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN)
    }


    // 建立 showToast 方法顯示Toast訊息
    private fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // 建立 Handler 變數接收訊息
    private val handler = Handler(Looper.getMainLooper()) { msg ->
        // 判斷編號，並更新兔子的進度
        if (msg.what == 1) {
            // 更新兔子的進度
            sbRabbit.progress = progressRabbit
            //執行兔子移動
            animateRabbit(progressRabbit)
            // 若兔子抵達，則顯示兔子勝利
            if (progressRabbit >= 100 && progressTurtle < 100) {
                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                dateFormat.timeZone = java.util.TimeZone.getTimeZone("Asia/Taipei")
                val currentTime = dateFormat.format(Date())
                recordResult("兔子", progressRabbit, progressTurtle, currentTime)
                btnStart.isEnabled = true
                showToast("兔子獲勝")
                animateWinner("兔子")
            }

        } else if (msg.what == 2) {
            // 更新烏龜的進度
            sbTurtle.progress = progressTurtle
            //執行兔子移動
            animateTurtle(progressTurtle)
            // 若烏龜抵達，則顯示烏龜勝利
            if (progressTurtle >= 100 && progressRabbit < 100) {
                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                dateFormat.timeZone = java.util.TimeZone.getTimeZone("Asia/Taipei")
                val currentTime = dateFormat.format(Date())
                recordResult("烏龜", progressRabbit, progressTurtle, currentTime)
                btnStart.isEnabled = true
                showToast("烏龜獲勝")
                animateWinner("烏龜")
            }
        }
        true
    }

    // 用 Thread 模擬兔子移動
    private fun runRabbit() {
        Thread {
            // 兔子有三分之二的機率會偷懶
            val sleepProbability = arrayOf(true, true, false)
            while (progressRabbit < 100 && progressTurtle < 100) {
                try {
                    Thread.sleep(100) // 延遲0.1秒更新賽況
                    if (sleepProbability.random())
                        Thread.sleep(300) // 兔子偷懶0.3秒
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                progressRabbit += rabbitspeed // 每次跑三步

                val msg = Message() // 建立Message物件
                msg.what = 1  // 加入編號
                handler.sendMessage(msg) // 傳送兔子的賽況訊息
            }
        }.start() // 啟動 Thread
    }

    // 用 Thread 模擬烏龜移動
    private fun runTurtle() {
        Thread {
            while (progressTurtle < 100 && progressRabbit < 100) {
                try {
                    Thread.sleep(100) // 延遲0.1秒更新賽況
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                progressTurtle += turtlespeed // 每次跑一步

                val msg = Message() // 建立Message物件
                msg.what = 2 // 加入編號
                handler.sendMessage(msg) // 傳送烏龜的賽況訊息
            }
        }.start() // 啟動 Thread
    }


    private fun animateWinner(winner: String) {
        val winnerView = if (winner == "兔子") ivRabbit else
            ivTurtle
        //停止背景音樂
        stopBackgroundMusic()
        playSound(winSound)

        val scaleX = ObjectAnimator.ofFloat(winnerView, "scaleX", 1f, 1.5f, 1.2f)
        val scaleY = ObjectAnimator.ofFloat(winnerView, "scaleY", 1f, 1.5f, 1.2f)
        val rotation = ObjectAnimator.ofFloat(winnerView, "rotation", 0f, 360f)

        val victorySet = AnimatorSet()
        victorySet.playTogether(scaleX, scaleY, rotation)
        victorySet.duration = 1000
        victorySet.interpolator = BounceInterpolator()
        victorySet.start()

        val blinkAnimation = ObjectAnimator.ofFloat(winnerView, "alpha", 1f, 0.3f, 1f)
        blinkAnimation.duration = 300
        blinkAnimation.repeatCount = 5
        blinkAnimation.startDelay = 1000  // 延遲1秒開始
        blinkAnimation.start()
    }


    private fun recordResult(
        winner: String,
        rabbittimes: Int,
        turtletimes: Int,
        timestamp: String
    ) {
        try {

            val record = RaceResult(
                winner = winner,
                rabbit = rabbittimes,
                turtle = turtletimes,
                timestamps = timestamp
            )

            raceResults.add(0, record) // 加在最上面
            adapter.notifyItemInserted(0)
            rvHistory.scrollToPosition(0)
            updateWinCount(winner)

            val recordId = dbHelper.insertRaceRecord(record)

            if (recordId != -1L) {
                // 建立包含ID的記錄用於本地列表
                val recordWithId = record.copy(id = recordId)

                // 增加記錄到列表開頭（最新記錄在最上方）
                raceResults.add(0, recordWithId)

                // 通知轉接器資料變更
                adapter.notifyItemInserted(0)

                // 滾動到頂部顯示最新記錄
                rvHistory.smoothScrollToPosition(0)

                // 更新空狀態顯示
                updateEmptyState()
            } else {
                showToast("記錄儲存失敗")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "記錄比賽結果時發生錯誤: ${e.message}", e)
            showToast("記錄比賽結果失敗")
        }
    }


    private fun clearRaceHistory() {
        raceResults.clear()
        adapter.notifyDataSetChanged()
    }



    private fun updateWinCount(winner: String) {
        when (winner) {
            "兔子" -> rabbitwin++
            "烏龜" -> turtlewin++
        }

        val winThreadholds = (best0f / 2) + 1
        showToast("目前比分 - 兔子:$rabbitwin, 烏龜:$turtlewin")

        if (rabbitwin >= winThreadholds || turtlewin >= winThreadholds) {
            val intent = Intent(this, MainActivity2::class.java).apply {
                putExtra("RABBIT_WINS", rabbitwin)
                putExtra("TURTLE_WINS", turtlewin)
                putExtra("BEST_OF", best0f)
                putExtra("FINAL_WINNER", if (rabbitwin > turtlewin) "兔子" else "烏龜")
            }
            startActivity(intent)
            resetWinCount()
            clearRaceHistory()
        }

    }
}

class RaceDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "race_history.db"
        private const val DATABASE_VERSION = 1

        // 資料表名稱和欄位
        const val TABLE_RACE_HISTORY = "race_history"
        const val COLUMN_ID = "id"
        const val COLUMN_WINNER = "winner"
        const val COLUMN_RABBIT_PROGRESS = "rabbit_progress"
        const val COLUMN_TURTLE_PROGRESS = "turtle_progress"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            Log.d("RaceDatabaseHelper", "正在建立資料庫...")
            val createTableSQL = """
            CREATE TABLE $TABLE_RACE_HISTORY (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_WINNER TEXT NOT NULL,
                $COLUMN_RABBIT_PROGRESS INTEGER NOT NULL,
                $COLUMN_TURTLE_PROGRESS INTEGER NOT NULL,
                $COLUMN_TIMESTAMP TEXT NOT NULL
            )
        """.trimIndent()

            db.execSQL(createTableSQL)
            Log.d("RaceDatabaseHelper", "資料庫建立成功")
        } catch (e: Exception) {
            Log.e("RaceDatabaseHelper", "建立資料庫失敗: ${e.message}", e)
        }

    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            Log.d("RaceDatabaseHelper", "升級資料庫從版本 $oldVersion 到 $newVersion")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_RACE_HISTORY")
            onCreate(db)
        } catch (e: Exception) {
            Log.e("RaceDatabaseHelper", "升級資料庫失敗: ${e.message}", e)
        }
    }

    fun insertRaceRecord(record: RaceResult): Long {
        var db: SQLiteDatabase? = null
        return try {
            Log.d("RaceDatabaseHelper", "正在插入記錄: ${record.winner} 贏了")

            db = this.writableDatabase

            val values = ContentValues().apply {
                put(COLUMN_WINNER, record.winner)
                put(COLUMN_RABBIT_PROGRESS, record.rabbit)
                put(COLUMN_TURTLE_PROGRESS, record.turtle)
                put(COLUMN_TIMESTAMP, record.timestamps)
            }

            val id = db.insert(TABLE_RACE_HISTORY, null, values)

            if (id == -1L) {
                Log.e("RaceDatabaseHelper", "插入記錄失敗")
                -1L
            } else {
                Log.d("RaceDatabaseHelper", "插入記錄成功，ID: $id")
                id
            }
        } catch (e: Exception) {
            Log.e("RaceDatabaseHelper", "插入記錄時發生錯誤: ${e.message}", e)
            -1L
        } finally {
            db?.close()
        }
    }


    fun getAllRaceRecords(): List<RaceResult> {
        val records = mutableListOf<RaceResult>()
        var db: SQLiteDatabase? = null

        return try {
            Log.d("RaceDatabaseHelper", "正在查詢所有記錄...")

            db = this.readableDatabase

            val cursor = db.query(
                TABLE_RACE_HISTORY,
                null,
                null,
                null,
                null,
                null,
                "$COLUMN_ID DESC" // 按ID倒序排列，最新的記錄在前
            )

            cursor.use {
                while (it.moveToNext()) {
                    val record = RaceResult(
                        id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                        winner = it.getString(it.getColumnIndexOrThrow(COLUMN_WINNER)),
                        rabbit = it.getInt(it.getColumnIndexOrThrow(COLUMN_RABBIT_PROGRESS)),
                        turtle = it.getInt(it.getColumnIndexOrThrow(COLUMN_TURTLE_PROGRESS)),
                        timestamps = it.getString(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    )
                    records.add(record)
                }
            }

            Log.d("RaceDatabaseHelper", "記錄查詢完成，共 ${records.size} 筆")
            records
        } catch (e: Exception) {
            Log.e("RaceDatabaseHelper", "查詢記錄時發生錯誤: ${e.message}", e)
            emptyList()
        } finally {
            db?.close()
        }
    }

    fun clearAllRecords(): Boolean {
        var db: SQLiteDatabase? = null
        return try {
            Log.d("RaceDatabaseHelper", "正在清空所有記錄...")

            db = this.writableDatabase
            val deletedRows = db.delete(TABLE_RACE_HISTORY, null, null)

            Log.d("RaceDatabaseHelper", "已刪除 $deletedRows 筆記錄")
            true
        } catch (e: Exception) {
            Log.e("RaceDatabaseHelper", "清空記錄時發生錯誤: ${e.message}", e)
            false
        } finally {
            db?.close()
        }
    }
}


