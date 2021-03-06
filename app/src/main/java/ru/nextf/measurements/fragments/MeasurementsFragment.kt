package ru.nextf.measurements.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Notification
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import com.google.gson.reflect.TypeToken
import ru.nextf.measurements.*
import ru.nextf.measurements.activity.OneMeasurementActivity
import ru.nextf.measurements.adapters.DataAdapter
import ru.nextf.measurements.network.NetworkController
import ru.nextf.measurements.network.NetworkControllerFree
import kotlinx.android.synthetic.main.measurements_fragment.*
import kotlinx.android.synthetic.main.measurements_fragment.view.*
import ru.nextf.measurements.modelAPI.*
import java.util.Calendar
import kotlin.collections.ArrayList
import ru.nextf.measurements.R.mipmap.ic_launcher
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationManagerCompat
import ru.nextf.measurements.activity.MainActivity


/**
 * Created by addd on 03.12.2017.
 */

class MeasurementsFragment : Fragment(), NetworkController.CallbackListMeasurements,
        DataAdapter.CustomAdapterCallback, NetworkController.ResponsibleCallback,
        NetworkController.PaginationCallback, NetworkControllerFree.CallbackListFree,
        NetworkControllerFree.CallbackPaginationFree, MyWebSocket.SocketCallback {
    private lateinit var date: String
    private var owner = ""
    var emptyList: ArrayList<Measurement> = ArrayList(emptyList())
    lateinit var fragmentListMeasurements: List<Measurement>
    private var isLoading = false
    private var isLastPage = false
    private var currentPage = 1
    private var TOTAL_PAGES = 4
    private lateinit var bundle: Bundle
    private lateinit var adapter: DataAdapter
    private var daySave = -1
    private var monthSave = -1
    private var yearSave = -1
    private var handler = Handler()

    private lateinit var fabOpen: Animation
    private lateinit var fabOpen08: Animation
    private lateinit var fabClose: Animation
    private lateinit var textOpen: Animation
    private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val REQUEST_EXTERNAL_STORAGE = 1
    private lateinit var textClose: Animation
    private var isFabOpen = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity as AppCompatActivity).supportActionBar?.show()
        NetworkController.registerCallBack(this)
        NetworkController.registerResponsibleCallback(this)
        NetworkController.registerPaginationCallback(this)
        NetworkControllerFree.registerCallbackFree(this)
        NetworkControllerFree.registerPaginationFree(this)
        getPermission()
        val view: View = inflater.inflate(ru.nextf.measurements.R.layout.measurements_fragment, container, false)
                ?: View(context)
        fabOpen = AnimationUtils.loadAnimation(context, ru.nextf.measurements.R.anim.fab_open)
        fabOpen08 = AnimationUtils.loadAnimation(context, ru.nextf.measurements.R.anim.fab_open_08)
        fabClose = AnimationUtils.loadAnimation(context, ru.nextf.measurements.R.anim.fab_close)
        textOpen = AnimationUtils.loadAnimation(context, ru.nextf.measurements.R.anim.text_open)
        textClose = AnimationUtils.loadAnimation(context, ru.nextf.measurements.R.anim.text_close)

        view.fabMain.setOnClickListener { showFubs() }
        view.fabMainClose.setOnClickListener { hideFub() }
        view.fabToday.setOnClickListener { todayFab() }
        view.fabTomorrow.setOnClickListener { tomorrowFab() }
        view.fabDate.setOnClickListener { dateFab() }
        view.recyclerList.setOnTouchListener { _, _ ->
            hideFub()
            false
        }

        selectColorVersion(view.buttonAll, ru.nextf.measurements.R.color.colorPrimaryDark)

        view.buttonAll.setOnClickListener {
            allMeasurements()
        }
        view.buttonFree.setOnClickListener {
            freeMeasurements()
        }
        view.buttonMy.setOnClickListener {
            myMeasurements()
        }

        bundle = this.arguments
        date = getTodayDate()
        adapter = DataAdapter(emptyList, this)
        view.recyclerList.adapter = adapter
        view.progressBarMain.visibility = View.VISIBLE
        bundle?.let {
            when (bundle.getInt(CHECK)) {
                STATUS_CURRENT -> NetworkController.getCurrentMeasurements(date, APP_LIST_TODAY_CURRENT)
                STATUS_REJECT -> {
                    view.linearLayoutBottom.visibility = View.GONE
                    NetworkController.getRejectMeasurements(date, APP_LIST_TODAY_REJECTED)
                }
                STATUS_CLOSE -> {
                    view.linearLayoutBottom.visibility = View.GONE
                    NetworkController.getCloseMeasurements(date, APP_LIST_TODAY_CLOSED)
                }
            }
        }
        view.refresh.setOnRefreshListener {
            view.refresh.isRefreshing = true
            currentPage = 1
            isLastPage = false
            adapter = DataAdapter(emptyList, this)
            recyclerList.adapter = adapter
            if (owner.isNullOrEmpty()) {
                NetworkController.updateListInFragment()
            } else {
                NetworkControllerFree.getCurrentMeasurements(date, 1, owner)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.refresh.setColorSchemeColors(resources.getColor(R.color.colorAccent, context.theme),
                    resources.getColor(R.color.colorPrimary, context.theme),
                    resources.getColor(R.color.colorPrimaryDark, context.theme))
        } else {
            view.refresh.setColorSchemeColors(resources.getColor(R.color.colorAccent),
                    resources.getColor(R.color.colorPrimary),
                    resources.getColor(R.color.colorPrimaryDark))
        }
        myWebSocket.registerSocketCallback(this)
        return view
    }

    private fun getPermission() {
        val permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    private fun hideFub() {
        if (isFabOpen) {
            fabToday.startAnimation(fabClose)
            fabMainClose.startAnimation(fabClose)
            fabTomorrow.startAnimation(fabClose)
            fabDate.startAnimation(fabClose)
            fabMain.startAnimation(fabOpen)
            fabToday.isClickable = false
            fabMainClose.isClickable = false
            fabTomorrow.isClickable = false
            fabDate.isClickable = false
            fabMain.isClickable = true
            isFabOpen = false
            textViewToday.startAnimation(textClose)
            textViewDate.startAnimation(textClose)
            textViewTomorrow.startAnimation(textClose)
        }
    }

    private fun showFubs() {
        fabMain.isClickable = false
        fabMain.startAnimation(fabClose)
        fabMainClose.startAnimation(fabOpen)
        fabToday.startAnimation(fabOpen08)
        fabTomorrow.startAnimation(fabOpen08)
        fabDate.startAnimation(fabOpen08)
        fabToday.isClickable = true
        fabMainClose.isClickable = true
        fabTomorrow.isClickable = true
        fabDate.isClickable = true
        isFabOpen = true
        textViewToday.startAnimation(textOpen)
        textViewDate.startAnimation(textOpen)
        textViewTomorrow.startAnimation(textOpen)
    }

    private fun todayFab() {
        owner = ""
        buttonAll.textSize = 16.0F
        buttonFree.textSize = 14.0F
        buttonMy.textSize = 14.0F
        selectColorVersion(buttonAll, ru.nextf.measurements.R.color.colorPrimaryDark)
        selectColorVersion(buttonFree, ru.nextf.measurements.R.color.colorPrimary)
        selectColorVersion(buttonMy, ru.nextf.measurements.R.color.colorPrimary)
        hideFub()
        date = getTodayDate()
        adapter = DataAdapter(emptyList, this)
        recyclerList.adapter = adapter
        progressBarMain.visibility = View.VISIBLE
        currentPage = 1
        isLastPage = false
        when (bundle.get(CHECK)) {
            STATUS_CURRENT -> NetworkController.getCurrentMeasurements(date, APP_LIST_TODAY_CURRENT)
            STATUS_REJECT -> NetworkController.getRejectMeasurements(date, APP_LIST_TODAY_REJECTED)
            STATUS_CLOSE -> NetworkController.getCloseMeasurements(date, APP_LIST_TODAY_CLOSED)
        }
    }

    private fun tomorrowFab() {
        owner = ""
        buttonAll.textSize = 16.0F
        buttonFree.textSize = 14.0F
        buttonMy.textSize = 14.0F
        selectColorVersion(buttonAll, ru.nextf.measurements.R.color.colorPrimaryDark)
        selectColorVersion(buttonFree, ru.nextf.measurements.R.color.colorPrimary)
        selectColorVersion(buttonMy, ru.nextf.measurements.R.color.colorPrimary)
        hideFub()
        date = getTomorrowDate()
        adapter = DataAdapter(emptyList, this)
        recyclerList.adapter = adapter
        progressBarMain.visibility = View.VISIBLE
        currentPage = 1
        isLastPage = false
        when (bundle.get(CHECK)) {
            STATUS_CURRENT -> NetworkController.getCurrentMeasurements(date, APP_LIST_TOMORROW_CURRENT)
            STATUS_REJECT -> NetworkController.getRejectMeasurements(date, APP_LIST_TOMORROW_REJECTED)
            STATUS_CLOSE -> NetworkController.getCloseMeasurements(date, APP_LIST_TOMORROW_CLOSED)
        }
    }

    private fun dateFab() {
        hideFub()
        datePick()
    }

    private fun selectColorVersion(item: Button, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            item.setBackgroundColor(context.resources.getColor(color, context.theme))
        } else {
            item.setBackgroundColor(context.resources.getColor(color))
        }
    }

    private fun allMeasurements() {
        isLoading = false
        isLastPage = false
        currentPage = 1
        owner = ""
        adapter = DataAdapter(emptyList, this)
        recyclerList.adapter = adapter
        progressBarMain.visibility = View.VISIBLE
        when (bundle.get(CHECK)) {
            STATUS_CURRENT -> NetworkController.getCurrentMeasurements(date, null)
            STATUS_REJECT -> NetworkController.getRejectMeasurements(date, null)
            STATUS_CLOSE -> NetworkController.getCloseMeasurements(date, null)
        }
        hideFub()
        buttonAll.textSize = 16.0F
        buttonFree.textSize = 14.0F
        buttonMy.textSize = 14.0F
        selectColorVersion(buttonAll, ru.nextf.measurements.R.color.colorPrimaryDark)
        selectColorVersion(buttonFree, ru.nextf.measurements.R.color.colorPrimary)
        selectColorVersion(buttonMy, ru.nextf.measurements.R.color.colorPrimary)
    }

    private fun freeMeasurements() {
        isLoading = false
        isLastPage = false
        currentPage = 1
        adapter = DataAdapter(emptyList, this)
        recyclerList.adapter = adapter
        progressBarMain.visibility = View.VISIBLE
        hideFub()
        buttonFree.textSize = 16.0F
        buttonAll.textSize = 14.0F
        buttonMy.textSize = 14.0F
        selectColorVersion(buttonFree, ru.nextf.measurements.R.color.colorPrimaryDark)
        selectColorVersion(buttonAll, ru.nextf.measurements.R.color.colorPrimary)
        selectColorVersion(buttonMy, ru.nextf.measurements.R.color.colorPrimary)
        owner = "free"
        NetworkControllerFree.getCurrentMeasurements(date, 1, owner)
    }

    private fun myMeasurements() {
        isLoading = false
        isLastPage = false
        currentPage = 1
        adapter = DataAdapter(emptyList, this)
        recyclerList.adapter = adapter
        progressBarMain.visibility = View.VISIBLE
        hideFub()
        buttonMy.textSize = 16.0F
        buttonFree.textSize = 14.0F
        buttonAll.textSize = 14.0F
        selectColorVersion(buttonMy, ru.nextf.measurements.R.color.colorPrimaryDark)
        selectColorVersion(buttonAll, ru.nextf.measurements.R.color.colorPrimary)
        selectColorVersion(buttonFree, ru.nextf.measurements.R.color.colorPrimary)
        owner = "my"
        NetworkControllerFree.getCurrentMeasurements(date, 1, owner)
    }

    override fun onItemClick(pos: Int) {
        hideFub()
        val intent = Intent(context, OneMeasurementActivity::class.java)
        var deal = fragmentListMeasurements[pos].deal
        val json = gson.toJson(fragmentListMeasurements[pos])
        intent.putExtra(MEASUREMENT_KEY, json)
        intent.putExtra(ID_KEY, deal)
        intent.putExtra(SYMBOL_KEY, fragmentListMeasurements[pos].company?.symbol?.length.toString())
        startActivityForResult(intent, 0)
    }

    override fun onItemLongClick(pos: Int) {
        hideFub()
        val ad = android.app.AlertDialog.Builder(context)
        ad.setTitle(ru.nextf.measurements.R.string.become_response)  // заголовок
        var id = fragmentListMeasurements[pos].id
        ad.setPositiveButton(ru.nextf.measurements.R.string.yes) { _, _ ->
            if (id != null) {
                NetworkController.becomeResponsible(id)
            }
        }
        ad.setNegativeButton(ru.nextf.measurements.R.string.cancel) { _, _ -> }

        ad.setCancelable(true)
        ad.show()
    }


    private fun datePick() {
        val bundle = this.arguments
        val myCallBack = OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            owner = ""
            buttonAll.textSize = 16.0F
            buttonFree.textSize = 14.0F
            buttonMy.textSize = 14.0F
            selectColorVersion(buttonAll, ru.nextf.measurements.R.color.colorPrimaryDark)
            selectColorVersion(buttonFree, ru.nextf.measurements.R.color.colorPrimary)
            selectColorVersion(buttonMy, ru.nextf.measurements.R.color.colorPrimary)
            daySave = dayOfMonth
            monthSave = monthOfYear
            yearSave = year
            currentPage = 1
            isLastPage = false
            date = String.format("$year-%02d-%02d", monthOfYear + 1, dayOfMonth)
            adapter = DataAdapter(emptyList, this)
            recyclerList.adapter = adapter
            progressBarMain.visibility = View.VISIBLE
            when (bundle.get(CHECK)) {
                STATUS_CURRENT -> NetworkController.getCurrentMeasurements(date, null)
                STATUS_REJECT -> NetworkController.getRejectMeasurements(date, null)
                STATUS_CLOSE -> NetworkController.getCloseMeasurements(date, null)
            }
        }
        val calendar = Calendar.getInstance()
        val datePickerDialog = if (yearSave == -1) {
            DatePickerDialog(activity, myCallBack, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        } else {
            DatePickerDialog(activity, myCallBack, yearSave, monthSave, daySave)
        }
        datePickerDialog.show()
    }

    private fun updateList() {
        currentPage = 1
        isLastPage = false
        adapter = DataAdapter(emptyList, this)
        recyclerList.adapter = adapter
        progressBarMain.visibility = View.VISIBLE
        if (owner.isNullOrEmpty()) {
            NetworkController.updateListInFragment()
        } else {
            NetworkControllerFree.getCurrentMeasurements(date, 1, owner)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == 200) {
            updateList()
        }
    }


    @SuppressLint("SetTextI18n")
    override fun resultList(listMeasurements: List<Measurement>, result: Int, date: String, allMeasurements: Int?, myMeasurements: Int?, notDistributed: Int?, count: Int) {
        refresh.isRefreshing = false
        TOTAL_PAGES = if (count % 20 == 0) {
            count / 20
        } else {
            (count / 20) + 1
        }
        fragmentListMeasurements = listMeasurements

        val toolbar = (activity as AppCompatActivity).supportActionBar
        if (this.arguments.getInt(CHECK) == STATUS_CURRENT) {
            buttonAll.text = "$allMeasurements\nВсе"
            buttonFree.text = "$notDistributed\nСвободные"
            buttonMy.text = "$myMeasurements\nМои"
            toolbar?.title = "${formatDate(date)}"
        }
        if (this.arguments.getInt(CHECK) == STATUS_REJECT) {
            toolbar?.title = getString(ru.nextf.measurements.R.string.rejected)
        }
        if (this.arguments.getInt(CHECK) == STATUS_CLOSE) {
            toolbar?.title = getString(ru.nextf.measurements.R.string.closed)
        }

        if (listMeasurements.isEmpty()) {
            if (result == 1) {
                toast(ru.nextf.measurements.R.string.no_save_data)
            } else {
                toast(ru.nextf.measurements.R.string.nothing_show)
            }
        } else {
            if (result == 0) {
//                Toast.makeText(context, "Данные загружены из сети", Toast.LENGTH_SHORT).show()
            } else {
                if (this.arguments.getInt(CHECK) == STATUS_CURRENT) toolbar?.title = getString(ru.nextf.measurements.R.string.without_internet)
                toast(ru.nextf.measurements.R.string.no_internet)
            }
        }
        adapter = if (listMeasurements.isEmpty()) {
            DataAdapter(this.emptyList, this)
        } else {
            DataAdapter(listMeasurements as ArrayList, this)
        }
        recyclerList.adapter = adapter
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerList.layoutManager = layoutManager
        recyclerList.addOnScrollListener(object : ru.nextf.measurements.PaginationScrollListener(recyclerList.layoutManager as LinearLayoutManager) {
            override fun isLastPage(): Boolean {
                return isLastPage
            }

            override fun isLoading(): Boolean {
                return isLoading
            }

            override fun loadMoreItems() {
                isLoading = true
                currentPage += 1

                loadNextPage()
            }

            override fun getTotalPageCount(): Int {
                return TOTAL_PAGES
            }

        })
        addFooter()

        progressBarMain.visibility = View.GONE
    }

    private fun addFooter() {
        if (currentPage < TOTAL_PAGES) {
            adapter.addLoadingFooter()
        } else {
            isLastPage = true
        }
    }

    override fun resultResponsible(result: Boolean) {
        if (result) {
            updateList()
        }
    }

    override fun onStart() {
        super.onStart()
        myWebSocket.registerSocketCallback(this)
    }

    override fun onResume() {
        myWebSocket.registerSocketCallback(this)
        NetworkController.registerCallBack(this)
        NetworkController.registerResponsibleCallback(this)
        NetworkController.registerPaginationCallback(this)
        super.onResume()
    }

    override fun onDestroy() {
        NetworkController.registerCallBack(null)
        NetworkController.registerPaginationCallback(null)
        NetworkController.registerResponsibleCallback(null)
        super.onDestroy()
    }

    private fun loadNextPage() {
        NetworkController.pagination(currentPage)
    }

    override fun resultPaginationClose(listMeasurements: List<Measurement>, result: Int) {
        if (!adapter.isEmpty()) {
            adapter.removeLoadingFooter()
            isLoading = false

            if (!listMeasurements.isEmpty()) {
                adapter.addAll(listMeasurements)
            } else {
                currentPage -= 1
            }

            if (currentPage != TOTAL_PAGES) {
                adapter.addLoadingFooter()
            } else {
                isLastPage = true
            }
        }
        isLoading = false
    }

    override fun resultListFree(listMeasurements: List<Measurement>, result: Int, date: String, allMeasurements: Int?, myMeasurements: Int?, notDistributed: Int?, count: Int) {
        refresh.isRefreshing = false
        TOTAL_PAGES = if (count % 20 == 0) {
            count / 20
        } else {
            (count / 20) + 1
        }
        fragmentListMeasurements = listMeasurements

        val toolbar = (activity as AppCompatActivity).supportActionBar
        if (this.arguments.getInt(CHECK) == STATUS_CURRENT) {
            buttonAll.text = "$allMeasurements\nВсе"
            buttonFree.text = "$notDistributed\nСвободные"
            buttonMy.text = "$myMeasurements\nМои"
            toolbar?.title = "${formatDate(date)}"
        }
        if (this.arguments.getInt(CHECK) == STATUS_REJECT) {
            toolbar?.title = getString(ru.nextf.measurements.R.string.rejected)
        }
        if (this.arguments.getInt(CHECK) == STATUS_CLOSE) {
            toolbar?.title = getString(ru.nextf.measurements.R.string.closed)
        }

        if (listMeasurements.isEmpty()) {
            if (result == 1) {
                toast(ru.nextf.measurements.R.string.no_save_data)
            } else {
                toast(ru.nextf.measurements.R.string.nothing_show)
            }
        } else {
            if (result == 0) {
//                Toast.makeText(context, "Данные загружены из сети", Toast.LENGTH_SHORT).show()
            } else {
                if (this.arguments.getInt(CHECK) == STATUS_CURRENT) toolbar?.title = getString(ru.nextf.measurements.R.string.without_internet)
                toast(ru.nextf.measurements.R.string.no_internet)
            }
        }
        adapter = if (listMeasurements.isEmpty()) {
            DataAdapter(this.emptyList, this)
        } else {
            DataAdapter(listMeasurements as ArrayList, this)
        }
        recyclerList.adapter = adapter
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerList.layoutManager = layoutManager
        recyclerList.addOnScrollListener(object : ru.nextf.measurements.PaginationScrollListener(recyclerList.layoutManager as LinearLayoutManager) {
            override fun isLastPage(): Boolean {
                return isLastPage
            }

            override fun isLoading(): Boolean {
                return isLoading
            }

            override fun loadMoreItems() {
                isLoading = true
                currentPage += 1

                loadNextPageFree()
            }

            override fun getTotalPageCount(): Int {
                return TOTAL_PAGES
            }

        })
        addFooter()

        progressBarMain.visibility = View.GONE
    }

    private fun loadNextPageFree() {
        NetworkControllerFree.getCurrentMeasurements(date, currentPage, owner)
    }

    override fun paginationFree(listMeasurements: List<Measurement>, result: Int) {
        if (!adapter.isEmpty()) {
            adapter.removeLoadingFooter()
            isLoading = false

            adapter.addAll(listMeasurements)

            if (currentPage != TOTAL_PAGES) {
                adapter.addLoadingFooter()
            } else {
                isLastPage = true
            }
        }
        isLoading = false
    }

    override fun message(text: String) {
        val type = object : TypeToken<Event>() {}.type
        val event = gson.fromJson<Event>(text, type)
        when (event.event) {
            "on_complete_measurement", "on_reject_measurement", "on_take" -> {
                val type = object : TypeToken<EventUpdateList>() {}.type
                val transfer = gson.fromJson<EventUpdateList>(gson.toJson(event.data), type)
                handler.post {
                    if (transfer.date == date) {
                        if (bundle.getInt(CHECK) == STATUS_CURRENT) {
                            updateList()
                        }
                    }
                }
            }
            "on_create_measurement" -> {
                val type = object : TypeToken<EventCreate>() {}.type
                val create = gson.fromJson<EventCreate>(gson.toJson(event.data), type)
                if (create.date == date) {
                    handler.post {
                        updateList()
                    }
                }
                if (create.date == getTodayDate()) {
                    val notificationIntent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                    val builder = NotificationCompat.Builder(context, "wtf")
                            .setContentTitle("Новый замер")
                            .setContentText("На сегодня новый замер")
                            .setWhen(System.currentTimeMillis())
                            .setContentIntent(pendingIntent)
                            .setDefaults(Notification.DEFAULT_SOUND)
                            .setAutoCancel(true)
                            .setSmallIcon(R.mipmap.icon)
                    val notificationManager = NotificationManagerCompat.from(context)
                    notificationManager.notify(1001, builder.build())
                }
            }
            "on_transfer_measurement" -> {
                val type = object : TypeToken<EventTransfer>() {}.type
                val transfer = gson.fromJson<EventTransfer>(gson.toJson(event.data), type)
                handler.post {
                    if (transfer.oldDate == date || transfer.newDate == date) {
                        updateList()
                    }
                    if (transfer.newDate == getTodayDate()) {
                        val notificationIntent = Intent(context, MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                        val builder = NotificationCompat.Builder(context, "wtf")
                                .setContentTitle("Новый замер")
                                .setContentText("На сегодня новый замер")
                                .setWhen(System.currentTimeMillis())
                                .setContentIntent(pendingIntent)
                                .setDefaults(Notification.DEFAULT_SOUND)
                                .setAutoCancel(true)
                                .setSmallIcon(R.mipmap.icon)
                        val notificationManager = NotificationManagerCompat.from(context)
                        notificationManager.notify(1001, builder.build())
                    }
                }
            }
            "on_comment_measurement" -> {
                val type = object : TypeToken<NewCommentMeasurement>() {}.type
                val newComment = gson.fromJson<NewCommentMeasurement>(gson.toJson(event.data), type)
                for (meas in fragmentListMeasurements) {
                    if (meas.id == newComment.id) {
                        handler.post {
                            (meas.comments as ArrayList).add(newComment.comment)
                            val mPrefs = PreferenceManager.getDefaultSharedPreferences(ru.nextf.measurements.MyApp.instance)
                            val prefsEditor = mPrefs.edit()
                            when (bundle.getInt(CHECK)) {
                                STATUS_CURRENT -> {
                                    when (date) {
                                        getTodayDate() -> {
                                            val json = gson.toJson(fragmentListMeasurements)
                                            prefsEditor.putString(APP_LIST_TODAY_CURRENT, json)
                                            prefsEditor.apply()
                                        }
                                        getTomorrowDate() -> {
                                            val json = gson.toJson(fragmentListMeasurements)
                                            prefsEditor.putString(APP_LIST_TOMORROW_CURRENT, json)
                                            prefsEditor.apply()
                                        }
                                    }
                                }
                                STATUS_REJECT -> {
                                    when (date) {
                                        getTodayDate() -> {
                                            val json = gson.toJson(fragmentListMeasurements)
                                            prefsEditor.putString(APP_LIST_TODAY_REJECTED, json)
                                            prefsEditor.apply()
                                        }
                                        getTomorrowDate() -> {
                                            val json = gson.toJson(fragmentListMeasurements)
                                            prefsEditor.putString(APP_LIST_TOMORROW_REJECTED, json)
                                            prefsEditor.apply()
                                        }
                                    }
                                }
                                STATUS_CLOSE -> {
                                    when (date) {
                                        getTodayDate() -> {
                                            val json = gson.toJson(fragmentListMeasurements)
                                            prefsEditor.putString(APP_LIST_TODAY_CLOSED, json)
                                            prefsEditor.apply()
                                        }
                                        getTomorrowDate() -> {
                                            val json = gson.toJson(fragmentListMeasurements)
                                            prefsEditor.putString(APP_LIST_TOMORROW_CLOSED, json)
                                            prefsEditor.apply()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
