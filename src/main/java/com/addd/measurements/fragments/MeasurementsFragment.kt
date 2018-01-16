package com.addd.measurements.fragments

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.addd.measurements.*
import com.addd.measurements.activity.OneMeasurementActivity
import com.addd.measurements.adapters.DataAdapter
import com.addd.measurements.modelAPI.Measurement
import com.addd.measurements.network.NetworkController
import kotlinx.android.synthetic.main.activity_search_measurements.*
import kotlinx.android.synthetic.main.activity_search_measurements.view.*
import java.util.Calendar
import kotlin.collections.ArrayList


/**
 * Created by addd on 03.12.2017.
 */

class MeasurementsFragment : Fragment(), NetworkController.CallbackListMeasurements,
        DataAdapter.CustomAdapterCallback, NetworkController.ResponsibleCallback,
        NetworkController.PaginationCallback {
    private lateinit var date: String
    var emptyList: ArrayList<Measurement> = ArrayList(emptyList())
    lateinit var fragmentListMeasurements: List<Measurement>
    private var isLoading = false
    private var isLastPage = false
    private var currentPage = 1
    private var TOTAL_PAGES = 4
    private lateinit var bundle: Bundle
    private lateinit var adapter: DataAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity as AppCompatActivity).supportActionBar?.show()
        NetworkController.registerCallBack(this)
        NetworkController.registerResponsibleCallback(this)
        NetworkController.registerPaginationCallback(this)
        val view: View = inflater.inflate(R.layout.measurements_fragment, container, false) ?: View(context)


        val bottomNavigationView: BottomNavigationView = view.findViewById(R.id.bottomNavigation)

        onClickMenu(bottomNavigationView)

        bundle = this.arguments
        date = getTodayDate()
        adapter = DataAdapter(emptyList, this)
        view.recyclerList.adapter = adapter
        view.progressBarMain.visibility = View.VISIBLE
        bundle?.let {
            when (bundle.getInt(CHECK)) {
                STATUS_CURRENT -> NetworkController.getCurrentMeasurements(date, APP_LIST_TODAY_CURRENT)
                STATUS_REJECT -> NetworkController.getRejectMeasurements(date, APP_LIST_TODAY_REJECTED)
                STATUS_CLOSE -> NetworkController.getCloseMeasurements(date, APP_LIST_TODAY_CLOSED)
            }
        }

        return view
    }

    private fun onClickMenu(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            val bundle = this.arguments
            when (item.itemId) {
                R.id.today -> {
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
                R.id.tomorrow -> {
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
                R.id.date -> {
                    datePick()

                }
            }
            true
        }
    }

    override fun onItemClick(pos: Int) {
        val intent = Intent(context, OneMeasurementActivity::class.java)
        var deal = fragmentListMeasurements[pos].deal
        val json = gson.toJson(fragmentListMeasurements[pos])
        intent.putExtra(MEASUREMENT_KEY, json)
        intent.putExtra(ID_KEY, deal)
        intent.putExtra(SYMBOL_KEY, fragmentListMeasurements[pos].company?.symbol?.length.toString())
        startActivityForResult(intent, 0)
    }

    override fun onItemLongClick(pos: Int) {
        val ad = android.app.AlertDialog.Builder(context)
        ad.setTitle(R.string.become_response)  // заголовок
        var id = fragmentListMeasurements[pos].id
        ad.setPositiveButton(R.string.yes) { _, _ ->
            if (id != null) {
                NetworkController.becomeResponsible(id)
            }
        }
        ad.setNegativeButton(R.string.cancel) { _, _ -> }

        ad.setCancelable(true)
        ad.show()
    }

    private fun datePick() {
        val bundle = this.arguments
        val myCallBack = OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
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
        val datePikerDialog = DatePickerDialog(context, myCallBack, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        datePikerDialog.show()
    }

    private fun updateList() {
        currentPage = 1
        isLastPage = false
        adapter = DataAdapter(emptyList, this)
        recyclerList.adapter = adapter
        progressBarMain.visibility = View.VISIBLE
        NetworkController.updateListInFragment()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == 200) {
            updateList()
        }
    }


    override fun resultList(listMeasurements: List<Measurement>, result: Int, date: String, allMeasurements: Int?, myMeasurements: Int?, notDistributed: Int?, count: Int) {
        TOTAL_PAGES = if (count % 20 == 0) {
            count / 20
        } else {
            (count / 20) + 1
        }
        fragmentListMeasurements = listMeasurements

        val toolbar = (activity as AppCompatActivity).supportActionBar
        if (this.arguments.getInt(CHECK) == STATUS_CURRENT) {
            toolbar?.title = "${formatDate(date)} В:$allMeasurements Н:$notDistributed M:$myMeasurements"
        }
        if (this.arguments.getInt(CHECK) == STATUS_REJECT) {
            toolbar?.title = getString(R.string.rejected)
        }
        if (this.arguments.getInt(CHECK) == STATUS_CLOSE) {
            toolbar?.title = getString(R.string.closed)
        }

        if (listMeasurements.isEmpty()) {
            if (result == 1) {
                toast(R.string.no_save_data)
            } else {
                toast(R.string.nothing_show)
            }
        } else {
            if (result == 0) {
//                Toast.makeText(context, "Данные загружены из сети", Toast.LENGTH_SHORT).show()
            } else {
                if (this.arguments.getInt(CHECK) == STATUS_CURRENT) toolbar?.title = getString(R.string.without_internet)
                toast(R.string.no_internet)
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
        val dividerItemDecoration = DividerItemDecoration(recyclerList.context, layoutManager.orientation)
        recyclerList.addItemDecoration(dividerItemDecoration)
        recyclerList.addOnScrollListener(object : PaginationScrollListener(recyclerList.layoutManager as LinearLayoutManager) {
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

    override fun onResume() {
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

            adapter.addAll(listMeasurements)

            if (currentPage != TOTAL_PAGES)
                adapter.addLoadingFooter()
            else
                isLastPage = true
        }
        isLoading = false
    }

}
