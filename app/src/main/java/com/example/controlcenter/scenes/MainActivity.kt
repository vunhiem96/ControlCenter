package com.example.controlcenter.scenes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import android.widget.Toast
import com.example.controlcenter.R
import com.example.controlcenter.services.ControlCenterService
import com.example.controlcenter.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val intent: Intent = Intent(this, ControlCenterService::class.java)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        onCheckSwitch()
        PermissionAPI()
        checkPermissionNotification(intent)
        changeSize(intent)
        changePositon(intent)
        checkSystemWritePermission()

    }

    // kiểm tra trước khi tắt switch có được tắt hay mở không, có thì checked
    private fun onCheckSwitch() {
        if (Utils.getCheckControl(this) == 0) {
            sw_control_center.isChecked = true
            startService(intent)

        } else {
            sw_control_center.isChecked = false
        }
    }

    // khi destroy thì lưu trạng thái của switch
    override fun onDestroy() {
        super.onDestroy()
        if (sw_control_center.isChecked == true) {
            Utils.setCheckControl(this, 0)
        } else {
            Utils.setCheckControl(this, 1)
        }
    }

    // kiểm tra quyền hiển thị thông báo - quyền play nhạc
    private fun checkPermissionNotification(intent: Intent) {

        // Sử lý sự kiên khi nhấn vào switch bật tắt
        sw_control_center.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // kiểm tra đã bật quyền cho phép hiện thông báo chưa
                if (Settings.Secure.getString(
                        this.getContentResolver(),
                        "enabled_notification_listeners"
                    ).contains(getApplicationContext().getPackageName())
                ) {
                    startService(intent)
                    Utils.setCheckControl(this, 0)
                } else {
                    Toast.makeText(
                        this,
                        "Bạn cần cấp một số quyền cho ứng dụng",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    // nếu chưa bật quyền đó thì sẽ vào màn hình cấp quyền
                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    getApplicationContext().startActivity(intent)
                }


            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    stopService(intent)
                    Utils.setCheckControl(this, 1)

                } else {
                    stopService(intent)
                    Utils.setCheckControl(this, 1)
                }


            }
        }

    }

    // sự kiện thay đổi vị trí của icon
    private fun changePositon(intent: Intent) {
        var i = Utils.getPosition(this)
        if (i == 2) {
            rb_ben_phai.isChecked = true
        }
        if (i == 1) {
            rb_ben_trai.isChecked = true
        }
        if (i == 3) {
            rb_ben_duoi.isChecked = true
        }
        rb_ben_trai.setOnClickListener {
            Utils.setPosition(this, 1)
            rb_ben_phai.isChecked = false
            rb_ben_duoi.isChecked = false
            if (Utils.getCheckControl(this) == 0) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    stopService(intent)
                    startService(intent)
                } else {
                    stopService(intent)
                    startService(intent)
                }

            }

        }
        rb_ben_phai.setOnClickListener {
            rb_ben_duoi.isChecked = false
            rb_ben_trai.isChecked = false
            Utils.setPosition(this, 2)
            if (Utils.getCheckControl(this) == 0) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    stopService(intent)
                    startService(intent)
                } else {
                    stopService(intent)
                    startService(intent)
                }
            }
        }
        rb_ben_duoi.setOnClickListener {
            rb_ben_trai.isChecked = false
            rb_ben_phai.isChecked = false
            Utils.setPosition(this, 3)
            if (Utils.getCheckControl(this) == 0) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    stopService(intent)
                    startService(intent)
                } else {
                    stopService(intent)
                    startService(intent)
                }
            }
        }
    }

    // sự kiện thay đổi size của icon
    private fun changeSize(intent: Intent) {
        sb_size.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                Utils.SetSize(applicationContext, i)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

                if (Utils.getCheckControl(applicationContext) == 0) {
                    stopService(intent)
                    startService(intent)
                }

            }
        })
        sb_size.progress = Utils.getSize(this)

    }

    // xin quyền chỉnh sửa thông số hệ thống
    private fun checkSystemWritePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this@MainActivity))
                return true
            else
                openAndroidPermissionsMenu()
        }
        return false
    }

    // vào quyền chỉnh sửa thông số hệ thống
    private fun openAndroidPermissionsMenu() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:" + this.packageName)
            startActivity(intent)
            Toast.makeText(this, "Bạn cần cấp một số quyền cho ứng dụng", Toast.LENGTH_SHORT)
                .show()
        }
    }

    // xin quyền ghi đè giao diện màn hình
    private fun PermissionAPI() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this@MainActivity)) {
                // dialog ở đây
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, 1234)
                Toast.makeText(this, "Bạn cần cấp một số quyền cho ứng dụng", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}