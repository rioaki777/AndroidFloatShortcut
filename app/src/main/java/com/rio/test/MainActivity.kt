package com.rio.test

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_OVERLAY_PERMISSION = 1
    }
    private var mSelectApp:InstalledApps? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 権限要求
        if(!checkOverlayPermission()) requestOverlayPermission()

        // インストール済み、起動可能アプリ一覧作成
        val installedApps = CommonUtils.getInstalledAppInfo(this)
        val adapter = AppListAdapter(this, installedApps)
        listView1.adapter = adapter

        // アプリ選択時
        listView1.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            mSelectApp = installedApps[position]
            val iconByteArray: ByteArray = installedApps[position].appIcon ?: return@OnItemClickListener
            sampleapp_icon.setImageBitmap(CommonUtils.byte2Bitmap(iconByteArray))
        }

        buttonServiceStart.setOnClickListener {
            if(checkOverlayPermission()) {
                if(mSelectApp != null){
                    val serviceIntent = Intent(this, SampleService::class.java)
                    serviceIntent.putExtra("appinfo", mSelectApp)
                    startForegroundService(serviceIntent)
                }else{
                    Toast.makeText(applicationContext, "アプリを選択して下さい", Toast.LENGTH_LONG).show()
                }
            }else{
                Toast.makeText(applicationContext, "権限を付与して下さい", Toast.LENGTH_LONG).show()
                requestOverlayPermission()
            }
        }

        buttonServiceStop.setOnClickListener {
            val serviceIntent = Intent(this, SampleService::class.java)
            stopService(serviceIntent)
        }
    }

    private fun Context.checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun Activity.requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${getPackageName()}"))
        this.startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_OVERLAY_PERMISSION -> if (checkOverlayPermission()) {
                // 権限付与済み
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    // アプリ一覧のAdapter
    private class AppListAdapter(context: Context, dataList: List<InstalledApps>) : ArrayAdapter<InstalledApps>(context, R.layout.app_list) {

        private val mInflater: LayoutInflater

        init {
            mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            addAll(dataList)
        }

        override fun getView(position: Int, argConvertView: View?, parent: ViewGroup): View {
            val convertView = argConvertView ?: mInflater.inflate(R.layout.app_list, parent, false)
            val holder: ViewHolder = convertView.tag as? ViewHolder ?: ViewHolder().apply {
                textLabel = convertView.findViewById(R.id.app_name)
                imageIcon = convertView.findViewById(R.id.app_icon)
                convertView.tag = this
            }

            val data = getItem(position)
            // ラベルとアイコンをリストビューに設定
            data?.let {
                holder.textLabel?.text = it.appName ?: "Unknown"
                holder.imageIcon?.setImageBitmap(CommonUtils.byte2Bitmap(it.appIcon ?: ByteArray(0)))
            }

            return convertView
        }
    }

    private class ViewHolder {
        var textLabel: TextView? = null
        var imageIcon: ImageView? = null
    }
}