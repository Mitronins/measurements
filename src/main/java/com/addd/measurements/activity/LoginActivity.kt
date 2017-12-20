package com.addd.measurements.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.addd.measurements.APP_TOKEN
import com.addd.measurements.R
import com.addd.measurements.network.NetworkAuthorization
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(), NetworkAuthorization.MyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val mSettings: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (mSettings.contains(APP_TOKEN)) {
            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }

        btnLogin.setOnClickListener { goLogin() }
    }

    override fun onResume() {
        NetworkAuthorization.registerCallback(this)
        super.onResume()
    }

    private fun goLogin() {
        if (editLogin.length() == 0 || editPassword.length() == 0) {
            Toast.makeText(applicationContext, getString(R.string.emty_login_or_password), Toast.LENGTH_SHORT).show()
        } else {
            NetworkAuthorization.authorization(editLogin.text.toString(), editPassword.text.toString(), this)
        }
    }

    override fun resultAuth(result: Int) {
        when (result) {
            200 -> {
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finish()
            }
            400 -> Toast.makeText(applicationContext, getString(R.string.wrong_login_password), Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(applicationContext, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        NetworkAuthorization.registerCallback(null)
        super.onStop()
    }
}