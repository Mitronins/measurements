package com.addd.measurements.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.addd.measurements.*
import com.addd.measurements.fragments.MeasurementsFragment
import com.addd.measurements.fragments.DealsFragment
import com.addd.measurements.modelAPI.User
import com.addd.measurements.network.NetworkController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, NetworkController.UserInfoCallback {
    private val bundle = Bundle()
    private lateinit var item: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        NetworkController.registerUserInfoCallBack(this)
        title = getString(R.string.measurements)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbarAst)
        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbarAst, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        val menuItem = nav_view.menu.findItem(R.id.nav_current)
        item = menuItem
        menuItem.isChecked = true

        var fragment = MeasurementsFragment()
        bundle.putInt(CHECK, 0)
        startFragment(fragment, bundle)
        informationUser()
    }


    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            val menuItem = nav_view.menu.findItem(R.id.nav_current)
            if (menuItem.isChecked) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.exit_app)
                        .setMessage(R.string.realy_exit_app)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes) { _, _ -> finish() }
                        .setNegativeButton(R.string.no) { dialog, _ -> dialog.cancel() }
                val alert = builder.create()
                alert.show()
            } else {
                this.item.isChecked = false
                menuItem.isChecked = true
                var fragment = MeasurementsFragment()
                bundle.putInt(CHECK, 0)
                startFragment(fragment, bundle)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val searchMenuItem = menu.findItem(R.id.action_search)
        val searchView = searchMenuItem?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                val intentMeasurement = Intent(applicationContext, SearchMeasurementsActivity::class.java)
                intentMeasurement.putExtra(IS_IT_SEARCH, query)
                if (item.itemId == R.id.nav_current || item.itemId == R.id.nav_rejected || item.itemId == R.id.nav_closed) {
                    startActivityForResult(intentMeasurement, 1)
                }
                val intentDeal = Intent(applicationContext, SearchDealsActivity::class.java)
                intentDeal.putExtra(IS_IT_SEARCH, query)
                if (item.itemId == R.id.nav_currentDeal || item.itemId == R.id.nav_rejectDeal || item.itemId == R.id.nav_closeDeal) {
                    startActivityForResult(intentDeal, 1)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?) = true
        })
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?) = true
        })
        return true
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val menuItem = nav_view.menu.findItem(R.id.nav_current)
        menuItem.isChecked = false
        when (item.itemId) {
            R.id.nav_current -> {
                val fragment = MeasurementsFragment()
                bundle.putInt(CHECK, STATUS_CURRENT)
                changeFragment(fragment, item, bundle)
            }
            R.id.nav_rejected -> {
                val fragment = MeasurementsFragment()
                bundle.putInt(CHECK, STATUS_REJECT)
                changeFragment(fragment, item, bundle)
            }
            R.id.nav_closed -> {
                val fragment = MeasurementsFragment()
                bundle.putInt(CHECK, STATUS_CLOSE)
                changeFragment(fragment, item, bundle)
            }
            R.id.nav_currentDeal -> {
                val fragment = DealsFragment()
                bundle.putInt(CHECK, STATUS_CURRENT)
                changeFragment(fragment, item, bundle)
            }
            R.id.nav_rejectDeal -> {
                val fragment = DealsFragment()
                bundle.putInt(CHECK, STATUS_REJECT)
                changeFragment(fragment, item, bundle)
            }
            R.id.nav_closeDeal -> {
                val fragment = DealsFragment()
                bundle.putInt(CHECK, STATUS_CLOSE)
                changeFragment(fragment, item, bundle)
            }
            R.id.nav_exit -> {
                exitFromAccount()
            }
        }



        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun exitFromAccount() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle(R.string.exit_account)
                .setMessage(R.string.realy_exit_account)
                .setCancelable(false)
                .setPositiveButton(R.string.yes)
                { _, _ ->
                    val mSettings: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    val editor = mSettings.edit()
                    editor.clear()
                    editor.apply()
                    startActivity(Intent(applicationContext, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton(R.string.no) { dialog, _ -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

    private fun changeFragment(fragment: Fragment, item: MenuItem, bundle: Bundle?) {
        fragment.arguments = bundle
        this.item = item

        // Вставляем фрагмент, заменяя текущий фрагмент
        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
        // Выделяем выбранный пункт меню в шторке
        item.isChecked = true
        // Выводим выбранный пункт в заголовке
        title = item.title
    }

    private fun startFragment(fragment: Fragment, bundle: Bundle?) {
        fragment.arguments = bundle
        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
    }

    private fun informationUser() {
        NetworkController.getInfoUser()
    }

    override fun result(user: User) {
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val navHeader = navigationView.getHeaderView(0)
        val textName = navHeader.findViewById<TextView>(R.id.textUserNameDrawer)
        if (user.firstName.isNullOrEmpty() || user.lastName.isNullOrEmpty()) {
            textName.text = ""
        } else {
            textName.text = "${user.firstName} ${user.lastName}"
        }
    }

    override fun onDestroy() {
        NetworkController.registerUserInfoCallBack(null)
        super.onDestroy()
    }
}
