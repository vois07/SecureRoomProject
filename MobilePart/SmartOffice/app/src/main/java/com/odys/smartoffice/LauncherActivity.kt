package com.odys.smartoffice

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        AppConstants.login = PreferencesManager.getUsername(this)

        val thread = object : Thread() {
            override fun run() {
                try {
                    Thread.sleep(AppConstants.splashTime)
                    startActivity(Intent(applicationContext, RegisterActivity::class.java))
                    finish()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }

        thread.start()
    }
}
