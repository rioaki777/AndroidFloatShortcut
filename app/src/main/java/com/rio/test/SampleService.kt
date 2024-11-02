package com.rio.test

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton

class SampleService : Service() {
    private var mView: View? = null
    private var mWindowManager: WindowManager? = null
    private var mSelectApp:InstalledApps? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        mSelectApp = intent.getSerializableExtra("appinfo") as InstalledApps
        sendNotification()
        createFloatIconButton()
        return START_STICKY
    }

    private fun sendNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val name = "Float Shortcut App"
        val id = "foreground_service_id"
        val notifyDescription = "app detailed information"

        if (manager.getNotificationChannel(id) == null) {
            val mChannel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            mChannel.apply {
                description = notifyDescription
            }
            manager.createNotificationChannel(mChannel)
        }

        val notification = NotificationCompat.Builder(this,id).apply {
            setSmallIcon(R.drawable.ic_launcher_background)
            setContentTitle("Float Shortcut")
            setContentText("Now shortcut is available")
        }.build()

        startForeground(1, notification)
    }

    private fun createFloatIconButton() {
        mWindowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams()
        val layoutInflater = LayoutInflater.from(this)

        params.let {
            //レイヤー設定
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                it.type = WindowManager.LayoutParams.TYPE_PHONE
            }
            it.format = PixelFormat.RGBA_8888
            it.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            it.gravity = Gravity.LEFT or Gravity.TOP
            it.x = 0
            it.y = 0
            it.width = WindowManager.LayoutParams.WRAP_CONTENT
            it.height = WindowManager.LayoutParams.WRAP_CONTENT
        }

        mView = layoutInflater.inflate(R.layout.overlay, null)
        mWindowManager!!.addView(mView, params)
        val iconButton = mView!!.findViewById(R.id.icon_button) as ImageButton
        iconButton.setImageBitmap(mSelectApp?.appIcon?.let { CommonUtils.byte2Bitmap(it) })

        mView!!.measure(
            View.MeasureSpec.makeMeasureSpec(
                0,
                View.MeasureSpec.UNSPECIFIED
            ), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        // iconボタンの移動
        iconButton.setOnTouchListener { _, event ->
            params.x = event.rawX.toInt() - iconButton.measuredWidth / 2
            params.y = event.rawY.toInt() - iconButton.measuredHeight / 2
            mWindowManager?.updateViewLayout(mView, params)
            false
        }

        // タップされた場合
        iconButton.setOnClickListener {
            mSelectApp?.let {
                val packageName = it.packageName
                val className = it.className
                if (packageName.isNullOrEmpty() || className.isNullOrEmpty()) return@let

                val intent = Intent(Intent.ACTION_MAIN)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.setClassName(packageName, className)
                startActivity(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mWindowManager?.removeView(mView)
        stopSelf()
    }
}