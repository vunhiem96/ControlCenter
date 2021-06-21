package com.example.controlcenter.services

import abak.tr.com.boxedverticalseekbar.BoxedVertical
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.example.controlcenter.R
import com.example.controlcenter.scenes.ControlCenterGroupView
import com.example.controlcenter.utils.Utils
import java.io.File


class ControlCenterService : NotificationListenerService() {
    private var windowManager: WindowManager? = null
    private var viewBottom: ControlCenterGroupView? = null
    private var viewControl: ControlCenterGroupView? = null
    private var viewTimeOut: ControlCenterGroupView? = null
    private var bottomParams: WindowManager.LayoutParams? = null
    private var controlParams: WindowManager.LayoutParams? = null
    private var timeoutParams: WindowManager.LayoutParams? = null
    private lateinit var rlControl: RelativeLayout
    private lateinit var lnBottom: LinearLayout
    private lateinit var lnTimeOut: LinearLayout
    private lateinit var animUp: Animation
    private lateinit var animDown: Animation
    private lateinit var animLeft: Animation
    private lateinit var animRight: Animation
    private lateinit var animClick: Animation
    private lateinit var tbWifi: ToggleButton
    private lateinit var tbPlane: ToggleButton
    private lateinit var tbNetwork: ToggleButton
    private lateinit var tbRotate: ToggleButton
    private lateinit var tbBluetooth: ToggleButton
    private lateinit var tbMute: ToggleButton
    private lateinit var btnTimeOut: Button
    private lateinit var sbLight: BoxedVertical
    private lateinit var sbVolume: BoxedVertical
    private lateinit var tbFlashLight: ToggleButton
    private lateinit var btnClock: Button
    private lateinit var btnCalculator: Button
    private lateinit var btnCamera: Button
    private lateinit var btnPrevious: Button
    private lateinit var tbPlay: ToggleButton
    private lateinit var btnNext: Button
    private lateinit var tvMusicName: TextView
    private lateinit var imgBg: ImageView
    private lateinit var imgTimeOutBg: ImageView
    private var y: Int = 0
    private var touchY: Float = 0.0f
    private var x: Int = 0
    private var touchX: Float = 0.0f
    private var touchToMove: Boolean = false
    private var wifiManager: WifiManager? = null
    private var camera: Camera? = null
    private lateinit var mediaSessionManager: MediaSessionManager
    private var mediaController: MediaController? = null
    private var online: Boolean = false
    private val MEDIA_ACTION: String = "com.example.controlcenter.services.MEDIA_ACTION"
    private val MEDIA_UPDATE: String = "com.example.controlcenter.services.MEDIA_UPDATE"
    private val componentName: ComponentName =
        ComponentName(
            "com.example.controlcenter",
            "com.example.controlcenter.services.ControlCenterService"
        )
    private var objCameraManager: CameraManager? = null
    private var mCameraId: String? = null

    var currentlyPlaying: Boolean = false
    var currentArt: Bitmap? = null
    var currentArtist: String = ""
    var currentSong: String = ""
    var currentAlbum: String = ""
     var meta: MediaMetadata?=null
    var isFlashOn: Boolean? = null
    var context = this
    private var notification: Notification? = null
    private val NOTIFICATION_ID = 144
    val CHANNEL_ID = "1"


    override fun onCreate() {
        // sử lý phần play music
        registerReceiver(button, IntentFilter(MEDIA_ACTION))
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
            var controllers: List<MediaController> =
                mediaSessionManager.getActiveSessions(componentName)
            mediaController = this!!.pickController(controllers)!!
            if (mediaController != null) {
                mediaController!!.registerCallback(callback)
                meta = mediaController!!.metadata!!
                updateMetadata()

            }
            online = true
        } catch (e: Exception) {
            println("loi co on create")

        }
        // sử lý phần hiển thị notification, giữ cho control center luôn hoạt động, hiển thị icon
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createNotificationChannel()
            val CHANNEL_ID = "1"
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Control Center active")
                .setOnlyAlertOnce(true)
            notification = builder.build()
            with(NotificationManagerCompat.from(context)) {
                notify(
                    NOTIFICATION_ID,
                    notification!!
                )
            }
            startForeground(NOTIFICATION_ID, notification)
            Log.d("chan", "Start the foreground")
        }


    }

    private fun createNotificationChannel() {
        // sử lý hiển thị notification

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val name = "floating_window_noti_channel"
            val descriptionText = "A cool channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // sử lý phần play music
        if (mediaController == null) {
            try {
                var controllers: List<MediaController> =
                    mediaSessionManager.getActiveSessions(componentName)
                mediaController = this!!.pickController(controllers)!!
                if (mediaController != null) {
                    mediaController!!.registerCallback(callback)
                    meta = mediaController!!.getMetadata()!!
                    updateMetadata()
                }
            } catch (e: Exception) {
                println(e)

            }
        }
        initView()
        return START_STICKY

    }

    var callback: MediaController.Callback = object : MediaController.Callback() {
        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            mediaController = null!!
            meta = null!!
        }

        override fun onSessionEvent(event: String, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            updateMetadata()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            if (state != null) {
                currentlyPlaying = state.state == PlaybackState.STATE_PLAYING
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            meta = metadata!!
            updateMetadata()
        }

        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {
            super.onQueueChanged(queue)
        }

        override fun onQueueTitleChanged(title: CharSequence?) {
            super.onQueueTitleChanged(title)
        }

        override fun onExtrasChanged(extras: Bundle?) {
            super.onExtrasChanged(extras)
        }

        override fun onAudioInfoChanged(info: MediaController.PlaybackInfo?) {
            super.onAudioInfoChanged(info)
        }

    }


    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    // sử lý phần cập nhập thông tin nhạc
    fun updateMetadata() {
        try {
            if (mediaController != null && mediaController!!.playbackState != null) {
                currentlyPlaying =
                    mediaController!!.playbackState!!.state == PlaybackState.STATE_PLAYING
            }
            if (meta == null) return

                currentArt = meta!!.getBitmap(MediaMetadata.METADATA_KEY_ART)
                if (currentArt == null) {
                    currentArt = meta!!.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                }
                if (currentArt == null) {
                    currentArt = meta!!.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                }
                currentArtist = meta!!.getString(MediaMetadata.METADATA_KEY_ARTIST)
                currentSong = meta!!.getString(MediaMetadata.METADATA_KEY_TITLE)
                if (currentSong == null) {
                    currentSong = meta!!.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                }
                // currentAlbum = meta.getString(MediaMetadata.METADATA_KEY_ALBUM)

                if (currentArtist == null) {
                    currentArtist = meta!!.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                }
                if (currentArtist == null) {
                    currentArtist = meta!!.getString(MediaMetadata.METADATA_KEY_AUTHOR)
                }
                if (currentArtist == null) {
                    currentArtist = meta!!.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                }
                if (currentArtist == null) {
                    currentArtist = meta!!.getString(MediaMetadata.METADATA_KEY_WRITER)
                }
                if (currentArtist == null) {
                    currentArtist = meta!!.getString(MediaMetadata.METADATA_KEY_COMPOSER)
                }
                if (currentArtist == null) currentArtist = ""
                if (currentSong == null) currentSong = ""
                if (currentAlbum == null) currentAlbum = ""
                sendBroadcast(Intent(MEDIA_UPDATE))

        } catch (e: Exception) {
        }
    }

    fun pickController(controllers: List<MediaController>): MediaController? {
        for (i in 0 until controllers.size) {
            val mc: MediaController = controllers.get(i)
            if (mc.playbackState != null && mc.playbackState!!.state == PlaybackState.STATE_PLAYING) {
                return mc
            }
        }
        if (controllers.size > 0) return controllers.get(0)
        return null
    }

    var sessionListener: MediaSessionManager.OnActiveSessionsChangedListener =
        object : MediaSessionManager.OnActiveSessionsChangedListener {
            override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
                if (mediaController != null) {
                    mediaController = controllers?.let { pickController(it) }!!
                    if (mediaController == null) return
                    mediaController!!.registerCallback(callback)
                    try {
                        meta = mediaController!!.metadata!!
                        updateMetadata()
                    } catch (e: Exception) {
                        println("mediaController" + mediaController)
                        println("controllers" + controllers)
                        println("meta" + meta)
                    }


                }

            }

        }

    var button: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            //Play is 0, next is 1, previous is 2
            var action: Int = intent!!.getIntExtra("type", -1)
            if (mediaController != null && action == 0) {
                mediaController!!.dispatchMediaButtonEvent(
                    KeyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    )
                )
                mediaController!!.dispatchMediaButtonEvent(
                    KeyEvent(
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    )
                )
            } else if (mediaController != null && action == 1) {
                mediaController!!.dispatchMediaButtonEvent(
                    KeyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_MEDIA_NEXT
                    )
                )
                mediaController!!.dispatchMediaButtonEvent(
                    KeyEvent(
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_MEDIA_NEXT
                    )
                )
            } else if (mediaController != null && action == 2) {
                mediaController!!.dispatchMediaButtonEvent(
                    KeyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS
                    )
                )
                mediaController!!.dispatchMediaButtonEvent(
                    KeyEvent(
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS
                    )
                )
            } else if (action == 3) {
                val m: PackageManager = application.packageManager
                if (mediaController == null) {
                    val p: SharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    val pack: String? = p.getString("appLaunch", "").toString()
                    if (!pack.equals("")) {
                        startActivity((pack?.let { m.getLaunchIntentForPackage(it) }))
                    } else {
                        startActivity(m.getLaunchIntentForPackage(mediaController!!.getPackageName()))
                    }
                }
            }
        }

    }


    private fun initView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createIconView()
        createControlView()
        createTimeOut()
        showIcon()
        initAnimation()
    }

    // khởi tạo Animation
    private fun initAnimation() {
        animUp = AnimationUtils.loadAnimation(this, R.anim.anim_up)
        animDown = AnimationUtils.loadAnimation(this, R.anim.anim_down)
        animLeft = AnimationUtils.loadAnimation(this, R.anim.anim_left)
        animRight = AnimationUtils.loadAnimation(this, R.anim.anim_right)
        animClick = AnimationUtils.loadAnimation(this, R.anim.anim_click)

    }

    // hiển thị thanh nhỏ nhỏ ở bottom
    private fun showIcon() {
        try {
            windowManager!!.removeView(viewControl)
        } catch (e: Exception) {
            println("Bugs")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            windowManager!!.addView(viewBottom, bottomParams)

        } else {
            windowManager!!.addView(viewBottom, bottomParams)
        }


    }

    // hiển thị bảng Control
    private fun showControl() {
        try {
            windowManager!!.removeView(viewBottom)

        } catch (e: Exception) {
            println("Bugs")
        }

        windowManager!!.addView(viewControl, controlParams)

    }

    // hiển thị bảng chọn thời gian tắt màn hình
    private fun showTimeOut() {
        try {
            windowManager!!.removeView(viewControl)

        } catch (e: Exception) {
            println("Bugs")
        }
        windowManager!!.addView(viewTimeOut, timeoutParams)
    }

    // tạo các widget trong phần control
    private fun createControlView() {
        viewControl = ControlCenterGroupView(this)
        val view: View = View.inflate(this, R.layout.control_layout, viewControl)
        controlParams = WindowManager.LayoutParams()
        controlParams!!.width = WindowManager.LayoutParams.MATCH_PARENT
        controlParams!!.height = WindowManager.LayoutParams.MATCH_PARENT
        controlParams!!.gravity = Gravity.BOTTOM
        controlParams!!.format = PixelFormat.TRANSLUCENT
        controlParams!!.type = WindowManager.LayoutParams.TYPE_PHONE
        viewControl!!.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar

                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar

                    or View.SYSTEM_UI_FLAG_IMMERSIVE
        )
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            bottomParams!!.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
//            PixelFormat.TRANSLUCENT
//        } else {
//            bottomParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//            PixelFormat.TRANSLUCENT
//        }
        controlParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        //-------------- Ánh xạ các view trong control view
        imgBg = view.findViewById(R.id.img_bg)
        rlControl = view.findViewById(R.id.ln_control)
        Glide.with(this).load(R.drawable.img_control_bg).into(imgBg)
        tbWifi = view.findViewById(R.id.tb_wifi)
        tbPlane = view.findViewById(R.id.tb_plane)
        tbNetwork = view.findViewById(R.id.tb_network)
        tbBluetooth = view.findViewById(R.id.tb_bluetooth)
        tbRotate = view.findViewById(R.id.tb_rotate)
        tbMute = view.findViewById(R.id.tb_mute)
        btnTimeOut = view.findViewById(R.id.btn_time_out)
        sbLight = view.findViewById(R.id.sb_light)
        sbVolume = view.findViewById(R.id.sb_volume)
        tbFlashLight = view.findViewById(R.id.tb_flash_light)
        btnCalculator = view.findViewById(R.id.btn_calculator)
        btnCamera = view.findViewById(R.id.btn_camera)
        btnClock = view.findViewById(R.id.btn_clock)
        btnPrevious = view.findViewById(R.id.btn_previous)
        tbPlay = view.findViewById(R.id.tb_play)
        btnNext = view.findViewById(R.id.btn_next)
        tvMusicName = view.findViewById(R.id.tv_music_name)


    }

    // khởi tạo window manager của phần time out
    private fun createTimeOut() {
        viewTimeOut = ControlCenterGroupView(this)
        val view: View = View.inflate(this, R.layout.time_out_layout, viewTimeOut)
        timeoutParams = WindowManager.LayoutParams()
        timeoutParams!!.width = WindowManager.LayoutParams.MATCH_PARENT
        timeoutParams!!.height = WindowManager.LayoutParams.MATCH_PARENT
        timeoutParams!!.gravity = Gravity.BOTTOM
//        timeoutParams!!.format = PixelFormat.TRANSLUCENT
//        timeoutParams!!.type = WindowManager.LayoutParams.TYPE_PHONE
//        timeoutParams!!.flags =
//            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        timeoutParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        viewTimeOut!!.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar

                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar

                    or View.SYSTEM_UI_FLAG_IMMERSIVE
        )
        lnTimeOut = view.findViewById(R.id.ln_time_out)
        imgTimeOutBg = view.findViewById(R.id.img_time_out_bg)
        Glide.with(this).load(R.drawable.img_control_bg).into(imgTimeOutBg)
        lnTimeOut.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {

                }
                MotionEvent.ACTION_UP -> {
                    Log.d("test", "control_DOWN")
                    windowManager!!.removeView(viewTimeOut)
                    showControl()

                }
            }
            return@OnTouchListener true
        })
        var cb15s: CheckBox = view.findViewById(R.id.btn_15s)
        var cb30s: CheckBox = view.findViewById(R.id.btn_30s)
        var cb1p: CheckBox = view.findViewById(R.id.btn_1p)
        var cb2p: CheckBox = view.findViewById(R.id.btn_2p)
        var cb10p: CheckBox = view.findViewById(R.id.btn_10p)
        var cb30p: CheckBox = view.findViewById(R.id.btn_30p)
        val textColorRed = Color.RED
        val textColorWhite = Color.WHITE
        // sử lý sự kiện check time our rồi đổi màu của button set time
        if (getTimeOut() == 15000) {
            cb15s.isChecked = true
        }
        if (getTimeOut() == 30000) {
            cb30s.isChecked = true
        }
        if (getTimeOut() == 60000) {
            cb1p.isChecked = true
        }
        if (getTimeOut() == 120000) {
            cb2p.isChecked = true
        }
        if (getTimeOut() == 600000) {
            cb10p.isChecked = true
        }
        if (getTimeOut() == 1800000) {
            cb30p.isChecked = true
        }
        // đổi màu khi người dùng click vào button thời gian nào đó
        cb15s.setOnClickListener {
            setTimeOut(15000)
            Toast.makeText(this, "thời gian khóa màn hình là 15s,", Toast.LENGTH_SHORT).show()
            cb15s.isChecked = true
            cb30s.isChecked = false
            cb1p.isChecked = false
            cb2p.isChecked = false
            cb10p.isChecked = false
            cb30p.isChecked = false
            windowManager!!.removeView(viewTimeOut)
            showControl()
            rlControl.animation = animUp
            rlControl.animation.start()
        }
        cb30s.setOnClickListener {
            setTimeOut(30000)
            Toast.makeText(this, "thời gian khóa màn hình là 30s,", Toast.LENGTH_SHORT).show()
            cb30s.isChecked = true
            cb15s.isChecked = false
            cb1p.isChecked = false
            cb2p.isChecked = false
            cb10p.isChecked = false
            cb30p.isChecked = false
            windowManager!!.removeView(viewTimeOut)
            showControl()
            rlControl.animation = animUp
            rlControl.animation.start()
        }
        cb1p.setOnClickListener {
            setTimeOut(60000)
            Toast.makeText(this, "thời gian khóa màn hình là 1p,", Toast.LENGTH_SHORT).show()
            cb1p.isChecked = true
            cb15s.isChecked = false
            cb30s.isChecked = false
            cb2p.isChecked = false
            cb10p.isChecked = false
            cb30p.isChecked = false
            windowManager!!.removeView(viewTimeOut)
            showControl()
            rlControl.animation = animUp
            rlControl.animation.start()
        }
        cb2p.setOnClickListener {
            setTimeOut(120000)
            Toast.makeText(this, "thời gian khóa màn hình là 2p,", Toast.LENGTH_SHORT).show()
            cb2p.isChecked = true
            cb15s.isChecked = false
            cb30s.isChecked = false
            cb1p.isChecked = false
            cb10p.isChecked = false
            cb30p.isChecked = false
            windowManager!!.removeView(viewTimeOut)
            showControl()
            rlControl.animation = animUp
            rlControl.animation.start()
        }
        cb10p.setOnClickListener {
            setTimeOut(600000)
            Toast.makeText(this, "thời gian khóa màn hình là 10p,", Toast.LENGTH_SHORT).show()
            cb15s.isChecked = false
            cb30s.isChecked = false
            cb1p.isChecked = false
            cb2p.isChecked = false
            cb30p.isChecked = false
            cb10p.isChecked = true
            windowManager!!.removeView(viewTimeOut)
            showControl()
            rlControl.animation = animUp
            rlControl.animation.start()
        }
        cb30p.setOnClickListener {
            setTimeOut(1800000)
            Toast.makeText(this, "thời gian khóa màn hình là 30p,", Toast.LENGTH_SHORT).show()
            cb15s.isChecked = false
            cb30s.isChecked = false
            cb1p.isChecked = false
            cb2p.isChecked = false
            cb10p.isChecked = false
            cb30p.isChecked = true
            windowManager!!.removeView(viewTimeOut)
            showControl()
            rlControl.animation = animUp
            rlControl.animation.start()
        }


    }

    // fun set thời gian chờ của màn hình
    fun setTimeOut(miliseconds: Int) {
        android.provider.Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            miliseconds
        )
    }

    // fun lấy thời gian chờ của màn hình
    fun getTimeOut(): Int {
        var i: Int = android.provider.Settings.System.getInt(
            contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT
        )
        return i
    }


    //kiểm tra các trạng thái của hệ thống và sử lý các sự kiện của trạng thái đó
    private fun setState() {
        checkWifi()
        checkPlane()
        checkNetwork()
        checkBluetooth()
        checkRotateScreens()
        checkAudioSystem()
        timeOut()
        setLight()
        setVolume()
        flashLight()
        clock()
        caculator()
        openCamera()
        touchOutControl()
        playMusic()
    }


    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        super.onNotificationPosted(sbn, rankingMap)
        Log.e("NotificationPosted", "Posted")
    }

    // sử lý sự kiên play nhạc play music
    private fun playMusic() {
        if (mediaController == null) {
            val intentMusic: Intent = Intent(this, ControlCenterService::class.java)
            tbPlay.setOnClickListener {
                val intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                showIcon()
                startActivity(intent)
                stopService(intentMusic)
                startService(intentMusic)
            }

//            tbPlay.setOnCheckedChangeListener { buttonView, isChecked ->
//                if (isChecked == true) {
//
//                } else {
//                    tvMusicName.text = "Open music player"
//                }
//            }

        }

        if (currentlyPlaying == true) {
            tbPlay.isChecked = true
        } else {
            tbPlay.isChecked = false
        }

        if (mediaController != null) {
            updateMetadata()
            tvMusicName.text = currentSong
            btnPrevious.setOnClickListener {
                if (windowManager != null) {
                    val transportControls = mediaController!!.transportControls
                    transportControls.skipToPrevious()
                    updateMetadata()
                    tvMusicName.text = "Playing"
                    if (tbPlay.isChecked == false) {
                        tbPlay.isChecked = true
                    }
                    windowManager!!.updateViewLayout(viewControl, controlParams)
                }
            }
            tbPlay.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked == true) {
                    val transportControls = mediaController!!.transportControls
                    transportControls.play()
                    tvMusicName.text = "Playing"

                } else {
                    val transportControls = mediaController!!.transportControls
                    transportControls.pause()
                    tvMusicName.text = "Pause"
                    windowManager!!.updateViewLayout(viewControl, controlParams)
                }
            }
            btnNext.setOnClickListener {
                if (windowManager != null) {
                    updateMetadata()
                    val transportControls = mediaController!!.transportControls
                    transportControls.skipToNext()
                    updateMetadata()
                    tvMusicName.text = "Playing"
                    if (tbPlay.isChecked == false) {
                        tbPlay.isChecked = true
                    }
                    windowManager!!.updateViewLayout(viewControl, controlParams)

                }

            }


        }
    }

    //kiểm tra và thiết lập wifi
    private fun checkWifi() {
        // check xem wifi on hay off rồi set vào switch
        wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        if (Utils.CheckWifi(this) == true) {

            tbWifi.isChecked = true
        } else {

            tbWifi.isChecked = false
        }
        // sự kiện  khi nhấn wifi
        tbWifi.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked == true) {
                wifiManager!!.isWifiEnabled = true
            } else {
                wifiManager!!.isWifiEnabled = false
            }
        }
        tbWifi.setOnLongClickListener {
            var intent: Intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            showIcon()
            return@setOnLongClickListener true
        }

    }

    // kiểm tra và thiết lập chế độ máy bay
    private fun checkPlane() {
        // check xem chế độ máy bay on hay off rồi set vào switch
        if (Utils.CheckPlane(this) == true) {
            tbPlane.isChecked = true

        } else {
            tbPlane.isChecked = false

        }
        // sự kiện  khi nhấn vào máy bay
        tbPlane.setOnClickListener {
            var intent: Intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            showIcon()
        }
    }

    // kiểm tra và thiết lập mạng
    private fun checkNetwork() {
        if (Utils.CheckNetwork(this) == true) {
            tbNetwork.isChecked = true
        } else {
            tbNetwork.isChecked = false
        }
        tbNetwork.setOnClickListener {
            var intent: Intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            showIcon()
        }
    }

    // kiểm tra và thiết lập bluetooth
    private fun checkBluetooth() {

        var mBtAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (Utils.CheckBluetooth(this)) {
            tbBluetooth.isChecked = true

        } else {
            tbBluetooth.isChecked = false

        }
        tbBluetooth.setOnCheckedChangeListener { buttonView, isChecked ->

            if (isChecked == true) {
                mBtAdapter.enable()

            } else {
                mBtAdapter.disable()
            }
        }
        tbBluetooth.setOnLongClickListener {
            var intent: Intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            showIcon()
            return@setOnLongClickListener true
        }

    }

    // kiểm tra và thiết lậpchế độ xoay màn hình của điện thoại
    private fun checkRotateScreens() {
        if (Utils.checkRotate(this) == 1) {
            tbRotate.isChecked = true
        } else {
            tbRotate.isChecked = false
        }
        tbRotate.setOnCheckedChangeListener { buttonView, isChecked ->

            if (isChecked == true) {
                android.provider.Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION,
                    1
                )

            } else {

                android.provider.Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION,
                    0
                )

            }
        }

    }

    // kiểm tra và thiết lậpchế độ điện thoại, đang yên lặng hay bình thường
    private fun checkAudioSystem() {
        // check và set state của chế độ rung Vibrate
        if (Utils.checkAudio(this) == 1) {
            tbMute.isChecked = false
        } else {
            tbMute.isChecked = true
        }
        val audioManager: AudioManager
        audioManager = baseContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        tbMute.setOnCheckedChangeListener { buttonView, isChecked ->

            if (isChecked == true) {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL)

            } else {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT)

            }
        }
    }

    // sử lý ảnh sáng light
    private fun setLight() {
        var data = Utils.getLight(this@ControlCenterService)
        sbLight.value = data
        sbLight.setOnBoxedPointsChangeListener(object : BoxedVertical.OnValuesChangeListener {
            override fun onPointsChanged(boxedPoints: BoxedVertical, value: Int) {
                var brightness = value
                Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    value
                )
            }

            override fun onStartTrackingTouch(boxedPoints: BoxedVertical) {

            }

            override fun onStopTrackingTouch(boxedPoints: BoxedVertical) {

            }
        })

    }

    // sử ký âm thanh volume
    private fun setVolume() {
        var audioManager: AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sbVolume.value = Utils.getVolume(this)
        sbVolume.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        sbVolume.setOnBoxedPointsChangeListener(object : BoxedVertical.OnValuesChangeListener {
            override fun onPointsChanged(boxedPoints: BoxedVertical, value: Int) {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    value, 0
                )
            }

            override fun onStartTrackingTouch(boxedPoints: BoxedVertical) {

            }

            override fun onStopTrackingTouch(boxedPoints: BoxedVertical) {

            }
        })

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    // sử lý đèn pin flash
    private fun flashLight() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            isFlashOn = false
            objCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mCameraId = objCameraManager!!.cameraIdList[0]
            }
            tbFlashLight.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked == true) {
                    turnOnFlash()
                    isFlashOn = true
                } else {
                    turnOffFlash()
                    isFlashOn = true

                }
            }
        } else {
//            tbFlashLight.setOnCheckedChangeListener { buttonView, isChecked ->
//                if (isChecked == true) {
//                    if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
//                        turnOnFlash()
//                        isFlashOn = true
//                    }
//                } else {
//                    turnOffFlash()
//                    isFlashOn = true
//
//                }
//            }
        }


//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//
//
////            tbFlashLight.setOnCheckedChangeListener { buttonView, isChecked ->
////                if (isChecked == true) {
////
////                } else {
////
////                }
////            }
//        } else {
//            // sử lý sự kiện khi nhấn vào button Flash Light
//            tbFlashLight.setOnCheckedChangeListener { buttonView, isChecked ->
//                if (isChecked == true) {
//                    if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
//                        camera = Camera.open()
//                        var p = camera!!.getParameters()
//                        p!!.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
//                        camera!!.setParameters(p)
//                        camera!!.startPreview()
//                    }
//                } else {
//                    camera.stopPreview()
//                    camera.release()
//
//                }
//            }
//        }


    }

    // tắt đèn pin flash
    private fun turnOffFlash() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                objCameraManager!!.setTorchMode(mCameraId!!, false)
            } catch (e: Exception) {
                Log.i("tag", "loi")
            }

        } else {

            if (camera != null) {
                camera!!.stopPreview()
                camera!!.release()

            }

        }
    }

    // mở đèn pin flash
    private fun turnOnFlash() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                objCameraManager!!.setTorchMode(mCameraId!!, true)
            } catch (e: Exception) {
                Log.i("tag", "loi")
            }

        } else {
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                if (camera == null)

                    camera = Camera.open()
                var p = camera!!.getParameters()
                p!!.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
                camera!!.setParameters(p)
                camera!!.startPreview()
            }
        }
    }

    // mở đồng hồ clock
    private fun clock() {
        // sử lý sự kiện khi nhấn vào button đồng hồ
        btnClock.setOnClickListener {
            var packageManager: PackageManager = application.packageManager
            var intent: Intent =
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            windowManager!!.removeView(viewControl)
            showIcon()


        }
    }

    // mở caculator máy tính
    private fun caculator() {
        // sử lý sự kiện khi nhấn vào button máy tính
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            btnCalculator.setOnClickListener {
                val intent = Intent()
                intent.setAction(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_APP_CALCULATOR)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                windowManager!!.removeView(viewControl)
                showIcon()
            }
        } else {
            btnCalculator.setOnClickListener {
                val intent: Intent = Intent()
                intent.setClassName("com.android.calculator2", "com.android.calculator2.Calculator")
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                windowManager!!.removeView(viewControl)
                showIcon()

            }
        }


    }

    // mở camera máy ảnh
    private fun openCamera() {

        // sử lý sự kiện khi nhấn vào button Camera
        btnCamera.setOnClickListener {
            val intent = Intent("android.media.action.IMAGE_CAPTURE")
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            windowManager!!.removeView(viewControl)
            showIcon()


        }
    }

    private fun timeOut() {
        // sử lý sự kiện set time out - cài đặt thời gian chờ màn hình
        btnTimeOut.setOnClickListener {
            showTimeOut()
        }
    }

    // sử lý sự kiện khi nhấn ra ngoài vùng control 4
    private fun touchOutControl() {
        // sử lý sự kiện khi nhấn vào phần control để out ra khỏi nó
        rlControl.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {


                }
                MotionEvent.ACTION_UP -> {
                    lnBottom.animation = animUp
                    lnBottom.animation.start()
                    showIcon()
                }
            }
            return@OnTouchListener true
        })
    }

    // tạo các widget trong phần icon bottom -- cái thanh dài dài nhỏ nhỏ ý :>>
    private fun createIconView() {
        viewBottom = ControlCenterGroupView(this)
        var view: View
        bottomParams = WindowManager.LayoutParams()
        bottomParams!!.width = WindowManager.LayoutParams.WRAP_CONTENT
        bottomParams!!.height = WindowManager.LayoutParams.WRAP_CONTENT

//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
//            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//            PixelFormat.TRANSLUCENT
//
//        } else {
//            bottomParams!!.format = PixelFormat.TRANSLUCENT
//            bottomParams!!.type = WindowManager.LayoutParams.TYPE_PHONE
//        }
        bottomParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        if (Utils.getPosition(this) == 1) {
            view = View.inflate(this, R.layout.left_layout, viewBottom)
            bottomParams!!.gravity = Gravity.LEFT
            lnBottom = view.findViewById(R.id.ln_Bottom)
            lnBottom.layoutParams.height = Utils.getSize(this)
            moveControlLeft()

        }
        if (Utils.getPosition(this) == 2) {
            view = View.inflate(this, R.layout.right_layout, viewBottom)
            bottomParams!!.gravity = Gravity.RIGHT
            lnBottom = view.findViewById(R.id.ln_Bottom)
            lnBottom.layoutParams.height = Utils.getSize(this)
            moveControlRight()

        }
        if (Utils.getPosition(this) == 3) {
            view = View.inflate(this, R.layout.bottom_layout, viewBottom)
            bottomParams!!.gravity = Gravity.BOTTOM
            lnBottom = view.findViewById(R.id.ln_Bottom)
            lnBottom.layoutParams.width = Utils.getSize(this)
            moveControlUP()
        }
    }

    // sử lý sự kiện khi vuốt sang phải icon bottom
    private fun moveControlLeft() {
        lnBottom.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = bottomParams!!.x
                    touchX = motionEvent.rawX
                    touchToMove = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val delX = motionEvent.rawX - touchY
                    bottomParams!!.x = (x - delX).toInt()
                    windowManager!!.updateViewLayout(viewBottom, bottomParams)
                    if (delX * delX > 1) {
                        bottomParams!!.y = 0
                        windowManager!!.updateViewLayout(viewBottom, bottomParams)
                    }

                    if (delX * delX > 200) {
                        touchToMove = true
                        bottomParams!!.y = 0
                        windowManager!!.updateViewLayout(viewBottom, bottomParams)
                    }

                }
                MotionEvent.ACTION_UP -> {
                    if (touchToMove == true) {
                        rlControl.animation = animLeft
                        rlControl.animation.start()
                        showControl()
                        setState()
                    }
                }
            }
            return@OnTouchListener true
        })
    }

    // sử lý sự kiện khi vuốt sang trái icon bottom
    private fun moveControlRight() {
        lnBottom.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = bottomParams!!.x
                    touchX = motionEvent.rawX
                    touchToMove = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val delX = motionEvent.rawX - touchY
                    bottomParams!!.x = (x - delX).toInt()
                    windowManager!!.updateViewLayout(viewBottom, bottomParams)
                    if (delX * delX > 1) {
                        bottomParams!!.y = 0
                        windowManager!!.updateViewLayout(viewBottom, bottomParams)
                    }

                    if (delX * delX > 200) {
                        touchToMove = true
                        bottomParams!!.y = 0
                        windowManager!!.updateViewLayout(viewBottom, bottomParams)
                    }

                }
                MotionEvent.ACTION_UP -> {
                    if (touchToMove == true) {
                        rlControl.animation = animRight
                        rlControl.animation.start()
                        showControl()
                        setState()
                    }
                }
            }
            return@OnTouchListener true
        })
    }

    // sử lý sự kiện khi vuốt sang phải icon bottom
    private fun moveControlUP() {
        lnBottom.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    y = bottomParams!!.y
                    touchY = motionEvent.rawY
                    touchToMove = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val delY = motionEvent.rawY - touchY
                    bottomParams!!.y = (y - delY).toInt()
                    windowManager!!.updateViewLayout(viewBottom, bottomParams)
                    if (delY * delY > 1) {
                        bottomParams!!.y = 0
                        windowManager!!.updateViewLayout(viewBottom, bottomParams)
                    }

                    if (delY * delY > 200) {
                        touchToMove = true
                        bottomParams!!.y = 0
                        windowManager!!.updateViewLayout(viewBottom, bottomParams)
                    }

                }
                MotionEvent.ACTION_UP -> {
                    if (touchToMove == true) {
                        rlControl.animation = animUp
                        rlControl.animation.start()
                        showControl()
                        setState()
                    }
                }
            }
            return@OnTouchListener true
        })
    }

    // xử lý sự kiện destroy service
    override fun onDestroy() {
        unregisterReceiver(button)
        if (mediaController != null) {
            mediaController = null
            online = false
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        }
        windowManager!!.removeView(viewBottom)
    }
}