/*
 * Copyright 2016 Kamil Sartys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bitmacht.workingtitle36

import android.app.LoaderManager
import android.content.*
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.IntDef
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import de.bitmacht.workingtitle36.view.HoleyLayout
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Interval
import org.slf4j.LoggerFactory
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.*

class OverviewActivity : AppCompatActivity(), View.OnClickListener {

    private var dbHelper: DBHelper? = null

    private var toolbar: Toolbar? = null
    private var drawerLayout: DrawerLayout? = null
    private lateinit var monthBeforeBtn: ImageButton
    private lateinit var monthNextBtn: ImageButton
    private var monthRemain: TextView? = null
    private var monthSpent: TextView? = null
    private var monthAvailable: TextView? = null
    private lateinit var monthTransactionsButton: Button
    private lateinit var monthRecycler: RecyclerView
    private var dayLabel: TextView? = null
    private lateinit var dayBeforeBtn: ImageButton
    private lateinit var dayNextBtn: ImageButton
    private var dayRemain: TextView? = null
    private var daySpent: TextView? = null
    private var dayAvailable: TextView? = null
    private lateinit var dayTransactionsButton: Button
    private lateinit var dayRecycler: RecyclerView
    private var navBar: NavigationView? = null
    private var helpScreen: HoleyLayout? = null

    @IntDef(PERIOD_BEFORE.toLong(), PERIOD_NEXT.toLong())
    @Retention(RetentionPolicy.SOURCE)
    annotation class PeriodModifier

    private var drawerToggle: ActionBarDrawerToggle? = null

    private var regulars: ArrayList<RegularModel>? = null
    /** The transactions for the currently selected month */
    private var transactions: ArrayList<TransactionsModel>? = null
    private var hasTransactionsMonth = false
    private var hasTransactionsDay = false

    private var adapter: TransactionsArrayAdapter? = null

    /**
     * This indicates that the user has chosen to view the current day; it takes precedence over
     * the short period in [.periods]
     */
    private var isViewingToday = true
    private var periods = Periods()
    /**
     * Value of the transactions between the start of the accounting period (including) and
     * the selected day
     */
    private var spentBeforeDay: Value? = null
    private var spentDay: Value? = null

    private var periodHistory = HashMap<Long, Periods>()

    private val dataModifiedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BuildConfig.DEBUG) {
                logger.trace("received: {}", intent)
            }

            val args = Bundle()
            args.putParcelable(TransactionsLoader.ARG_PERIODS, periods)
            loaderManager.restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            logger.trace("savedInstanceState: {}", savedInstanceState)
        }

        dbHelper = DBHelper(this)

        setContentView(R.layout.activity_overview)

        toolbar = findViewById(R.id.toolbar) as Toolbar
        drawerLayout = findViewById(R.id.drawer_layout) as DrawerLayout
        monthBeforeBtn = findViewById(R.id.before_button) as ImageButton
        monthNextBtn = findViewById(R.id.next_button) as ImageButton
        monthRemain = findViewById(R.id.month_balance_remain_value) as TextView
        monthSpent = findViewById(R.id.month_balance_spent_value) as TextView
        monthAvailable = findViewById(R.id.month_balance_available_value) as TextView
        monthTransactionsButton = findViewById(R.id.month_transactions_button) as Button
        monthRecycler = findViewById(R.id.transactions_month) as RecyclerView
        dayLabel = findViewById(R.id.dayLabel) as TextView
        dayBeforeBtn = findViewById(R.id.day_before_button) as ImageButton
        dayNextBtn = findViewById(R.id.day_next_button) as ImageButton
        dayRemain = findViewById(R.id.day_balance_remain_value) as TextView
        daySpent = findViewById(R.id.day_balance_spent_value) as TextView
        dayAvailable = findViewById(R.id.day_balance_available_value) as TextView
        dayTransactionsButton = findViewById(R.id.day_transactions_button) as Button
        dayRecycler = findViewById(R.id.transactions_day) as RecyclerView
        navBar = findViewById(R.id.navigation) as NavigationView
        if (Utils.getbPref(this, R.string.pref_first_time_key, true)) {
            helpScreen = findViewById(R.id.help_screen) as HoleyLayout
            helpScreen!!.visibility = View.VISIBLE
            val hole = findViewById(R.id.hole_fab)
            hole.scaleX = 10f
            hole.scaleY = 10f
            hole.animate().scaleX(1f).scaleY(1f).interpolator = DecelerateInterpolator()
        }

        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        drawerToggle = object : ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {

        }
        drawerLayout!!.addDrawerListener(drawerToggle!!)

        monthBeforeBtn!!.setOnClickListener(this)
        monthNextBtn!!.setOnClickListener(this)

        monthTransactionsButton!!.setOnClickListener(this)
        dayTransactionsButton!!.setOnClickListener(this)

        monthRecycler!!.layoutManager = LinearLayoutManager(this)
        adapter = TransactionsArrayAdapter()
        monthRecycler!!.adapter = adapter

        dayBeforeBtn!!.setOnClickListener(this)
        dayNextBtn!!.setOnClickListener(this)

        dayRecycler!!.layoutManager = LinearLayoutManager(this)
        dayRecycler!!.adapter = adapter!!.subAdapter

        val itemClickListener: ClickListener = { adapter, adapterPosition ->
            val transaction = (adapter as TransactionsArrayAdapter).getModel(adapterPosition)
            if (transaction != null) {
                val intent = Intent(this@OverviewActivity, TransactionEditActivity::class.java)
                intent.putExtra(TransactionEditActivity.EXTRA_TRANSACTION, transaction)
                startActivityForResult(intent, REQUEST_TRANSACTION_EDIT)
            }
        }

        adapter!!.setOnItemClickListener(itemClickListener)
        adapter!!.subAdapter.setOnItemClickListener(itemClickListener)

        ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

                    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        val transaction = adapter!!.removeItem(viewHolder as BaseTransactionsAdapter<BaseTransactionsAdapter<RecyclerView.ViewHolder>.BaseTransactionVH>.BaseTransactionVH)
                        transaction.isRemoved = true
                        val tdt = TransactionsDeleteTask(this@OverviewActivity, transaction)
                        tdt.execute()
                        //TODO this should be shown only after a successful removal
                        Snackbar.make(findViewById(android.R.id.content), R.string.snackbar_transaction_removed, Snackbar.LENGTH_LONG).setAction(R.string.snackbar_undo, UndoClickListener(transaction)).show()
                        transactions!!.remove(transaction)
                        updateOverview()
                    }
                }).attachToRecyclerView(monthRecycler)

        ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

                    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        val transaction = adapter!!.subAdapter.removeItem(viewHolder as BaseTransactionsAdapter<BaseTransactionsAdapter<RecyclerView.ViewHolder>.BaseTransactionVH>.BaseTransactionVH)
                        transaction.isRemoved = true
                        val tdt = TransactionsDeleteTask(this@OverviewActivity, transaction)
                        tdt.execute()
                        //TODO this should be shown only after a successful removal
                        Snackbar.make(findViewById(android.R.id.content), R.string.snackbar_transaction_removed, Snackbar.LENGTH_LONG).setAction(R.string.snackbar_undo, UndoClickListener(transaction)).show()
                        transactions!!.remove(transaction)
                        updateOverview()
                    }
                }).attachToRecyclerView(dayRecycler)


        findViewById(R.id.fab).setOnClickListener { v -> startActivityForResult(Intent(v.context, TransactionEditActivity::class.java), REQUEST_TRANSACTION_NEW) }

        navBar!!.setNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener { item ->
            val id = item.itemId
            if (id == R.id.menu_regular_transactions) {
                drawerLayout!!.closeDrawer(navBar)
                startActivityForResult(Intent(this@OverviewActivity, OverviewRegularsActivity::class.java), REQUEST_REGULARS_OVERVIEW)
                return@OnNavigationItemSelectedListener true
            } else if (id == R.id.menu_settings) {
                drawerLayout!!.closeDrawer(navBar)
                startActivityForResult(Intent(this@OverviewActivity, SettingsActivity::class.java), REQUEST_SETTINGS)
                return@OnNavigationItemSelectedListener true
            } else if (id == R.id.menu_about) {
                AboutDialogFragment().show(supportFragmentManager, null)
                return@OnNavigationItemSelectedListener true
            }
            false
        })

        if (savedInstanceState != null) {
            regulars = savedInstanceState.getParcelableArrayList<RegularModel>(STATE_REGULARS)
            transactions = savedInstanceState.getParcelableArrayList<TransactionsModel>(STATE_TRANSACTIONS)
            isViewingToday = savedInstanceState.getBoolean(STATE_IS_VIEWING_TODAY)
            periods = if (isViewingToday) Periods() else savedInstanceState.getParcelable<Parcelable>(STATE_PERIODS) as Periods
            if (savedInstanceState.getBoolean(STATE_MONTH_RECYCLER_VISIBLE)) {
                monthRecycler!!.visibility = View.VISIBLE
            }
            if (savedInstanceState.getBoolean(STATE_DAY_RECYCLER_VISIBLE)) {
                dayRecycler!!.visibility = View.VISIBLE
            }

            periodHistory = savedInstanceState.getSerializable(STATE_PERIOD_HISTORY) as HashMap<Long, Periods>
        }

        loaderManager.initLoader(LOADER_ID_REGULARS, null, regularsLoaderListener)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (helpScreen != null) {
            Utils.setbPref(this, R.string.pref_first_time_key, false)
            helpScreen!!.visibility = View.GONE
            helpScreen = null
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onStart() {
        super.onStart()
        if (BuildConfig.DEBUG) {
            logger.trace("-")
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(dataModifiedReceiver, IntentFilter(DBModifyingAsyncTask.ACTION_DB_MODIFIED))

        val args = Bundle()
        args.putParcelable(TransactionsLoader.ARG_PERIODS, periods)
        loaderManager.initLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener)
    }

    override fun onStop() {
        if (BuildConfig.DEBUG) {
            logger.trace("-")
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataModifiedReceiver)
        super.onStop()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(STATE_REGULARS, regulars)
        outState.putParcelableArrayList(STATE_TRANSACTIONS, transactions)
        outState.putBoolean(STATE_IS_VIEWING_TODAY, isViewingToday)
        //TODO if the timezone changes after this, a wrong period may be restored
        outState.putParcelable(STATE_PERIODS, periods)
        outState.putBoolean(STATE_MONTH_RECYCLER_VISIBLE, monthRecycler!!.visibility == View.VISIBLE)
        outState.putBoolean(STATE_DAY_RECYCLER_VISIBLE, dayRecycler!!.visibility == View.VISIBLE)
        outState.putSerializable(STATE_PERIOD_HISTORY, periodHistory)
    }

    private fun setButtonEnabled(button: View, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1.0f else 0.26f
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle!!.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle!!.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle!!.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TRANSACTION_NEW || requestCode == REQUEST_TRANSACTION_EDIT) {
            if (resultCode == RESULT_OK) {
                val args = Bundle()
                args.putParcelable(TransactionsLoader.ARG_PERIODS, periods)
                loaderManager.restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener)
            }
        } else if (requestCode == REQUEST_REGULARS_OVERVIEW) {
            if (resultCode == RESULT_OK) {
                loaderManager.restartLoader(LOADER_ID_REGULARS, null, regularsLoaderListener)
            }
        } else if (requestCode == REQUEST_SETTINGS) {
            //TODO check if settings actually changed before updating
            loaderManager.restartLoader(LOADER_ID_REGULARS, null, regularsLoaderListener)
            val args = Bundle()
            args.putParcelable(TransactionsLoader.ARG_PERIODS, periods)
            loaderManager.restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener)
        }
    }

    override fun onBackPressed() {
        if (drawerLayout!!.isDrawerOpen(navBar!!)) {
            drawerLayout!!.closeDrawer(navBar)
        } else {
            super.onBackPressed()
        }
    }

    private fun changeMonth(@PeriodModifier periodModifier: Int) {
        var newPeriods = if (periodModifier == PERIOD_BEFORE) periods.previousLong() else periods.nextLong()
        val historicPeriods = periodHistory[newPeriods.longStart?.millis]
        if (historicPeriods != null) {
            newPeriods = historicPeriods
        }

        val args = Bundle()
        args.putParcelable(TransactionsLoader.ARG_PERIODS, newPeriods)
        loaderManager.restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener)
    }

    private fun changeDay(@PeriodModifier periodModifier: Int) {
        val newPeriods = (if (periodModifier == PERIOD_BEFORE) periods.previousShort() else periods.nextShort()) ?: return

        periods = newPeriods

        onPeriodChanged()

        updateTransactions()
    }

    private fun onPeriodChanged() {
        periodHistory.put(periods.longStart!!.millis, periods)

        val now = DateTime.now()
        // update the action bar
        // the next month would be in the future
        setButtonEnabled(monthNextBtn, !periods.longEnd.isAfter(now))
        supportActionBar!!.setTitle(getString(R.string.overview_title, periods!!.longStart!!.toGregorianCalendar()))

        // the first day of the month
        setButtonEnabled(dayBeforeBtn, periods.shortStart!!.isAfter(periods.longStart))
        // the last day of the month
        setButtonEnabled(dayNextBtn, periods.longEnd.isAfter(periods.shortEnd))

        isViewingToday = Interval(periods.shortStart, periods.shortPeriod).contains(now)
        dayLabel!!.text = if (isViewingToday)
            getString(R.string.overview_today)
        else
            getString(R.string.overview_day, periods!!.shortStart!!.dayOfMonth().get())
    }

    /**
     * Call this when the transactions have changed
     */
    private fun updateTransactions() {
        // filter the transactions so we get only the transactions performed on the selected day
        //TODO respect timezone
        //TODO applying to a result that does not include the current day makes only little sense
        //TODO somehow merge this with TransactionsArrayAdapter#setSubRange()
        if (transactions == null) {
            return
        }
        val startOfDayMillis = periods.shortStart!!.millis
        val endOfDayMillis = periods.shortEnd.millis
        val currencyCode = MyApplication.currency.currencyCode
        var valueBeforeDay = Value(currencyCode, 0)
        var valueDay = Value(currencyCode, 0)
        hasTransactionsMonth = !transactions!!.isEmpty()
        hasTransactionsDay = false
        for (transact in transactions!!) {
            val transactionTime = transact.mostRecentEdit!!.transactionTime
            if (transactionTime < startOfDayMillis) {
                try {
                    valueBeforeDay = valueBeforeDay.add(transact.mostRecentEdit!!.value)
                } catch (e: Value.CurrencyMismatchException) {
                    if (BuildConfig.DEBUG) {
                        logger.warn("unable to add: {}", transact.mostRecentEdit)
                    }
                }

            } else if (transactionTime < endOfDayMillis) {
                try {
                    valueDay = valueDay.add(transact.mostRecentEdit!!.value)
                    hasTransactionsDay = true
                } catch (e: Value.CurrencyMismatchException) {
                    if (BuildConfig.DEBUG) {
                        logger.warn("unable to add: {}", transact.mostRecentEdit)
                    }
                }

            }
        }
        adapter!!.setData(transactions!!, startOfDayMillis, endOfDayMillis)
        spentDay = valueDay
        spentBeforeDay = valueBeforeDay
        updateOverview()
    }

    private fun updateOverview() {
        if (regulars == null || transactions == null || spentDay == null || spentBeforeDay == null) {
            if (BuildConfig.DEBUG) {
                logger.warn("not initialized yet")
            }
            return
        }

        val currencyCode = MyApplication.currency.currencyCode

        var transactionsSum = Value(currencyCode, 0)
        if (transactions != null) {
            for (transaction in transactions!!) {
                if (transaction.mostRecentEdit == null) {
                    if (BuildConfig.DEBUG) {
                        logger.warn("mostRecentEdit is null: transaction: {}", transaction)
                    }
                    continue
                }
                try {
                    transactionsSum = transactionsSum.add(transaction.mostRecentEdit!!.value)
                } catch (e: Value.CurrencyMismatchException) {
                    if (BuildConfig.DEBUG) {
                        logger.warn("adding value failed")
                    }
                }

            }
        }

        val regularsValues = ArrayList<Value>(regulars!!.size)
        for (regular in regulars!!) {
            regularsValues.add(regular.getCumulativeValue(periods.longStart!!, periods.longEnd))
        }

        var regularsSum = Value(currencyCode, 0)
        try {
            regularsSum = regularsSum.addAll(regularsValues)
        } catch (e: Value.CurrencyMismatchException) {
            if (BuildConfig.DEBUG) {
                logger.warn("adding values failed", e)
            }
        }

        var remaining = Value(currencyCode, 0)
        try {
            remaining = regularsSum.add(transactionsSum)
        } catch (e: Value.CurrencyMismatchException) {
            if (BuildConfig.DEBUG) {
                logger.warn("subtraction failed", e)
            }
        }

        if (BuildConfig.DEBUG) {
            logger.trace("regsum: {} trsum: {} rem: {}", regularsSum, transactionsSum, regularsSum)
        }

        monthRemain!!.text = remaining.string
        monthSpent!!.text = transactionsSum.withAmount(-transactionsSum.amount).string
        monthAvailable!!.text = regularsSum.string
        adjustExpandButton(monthTransactionsButton, hasTransactionsMonth, monthRecycler)

        val daysTotal = Days.daysBetween(periods.longStart, periods.longEnd).days
        val daysBefore = Days.daysBetween(periods.longStart, periods.shortStart).days

        try {
            val remainingFromDay = regularsSum.add(spentBeforeDay!!)
            val remFromDayPerDay = remainingFromDay.withAmount(remainingFromDay.amount / (daysTotal - daysBefore))
            val remainingDay = remFromDayPerDay.add(spentDay!!)

            dayRemain!!.text = remainingDay.string
            daySpent!!.text = spentDay!!.withAmount(-spentDay!!.amount).string
            dayAvailable!!.text = remFromDayPerDay.string
            adjustExpandButton(dayTransactionsButton, hasTransactionsDay, dayRecycler)
        } catch (e: Value.CurrencyMismatchException) {
            if (BuildConfig.DEBUG) {
                logger.warn("unable to add", e)
            }
        }

    }

    private fun adjustExpandButton(expandButton: Button, isEnabled: Boolean, expandedView: View) {
        if (isEnabled) {
            expandButton.isEnabled = true
            expandButton.setText(if (expandedView.visibility == View.VISIBLE)
                R.string.overview_transactions_hide
            else
                R.string.overview_transactions_show)
        } else {
            expandButton.isEnabled = false
            expandButton.setText(R.string.overview_transactions_no)
        }
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.month_transactions_button || id == R.id.day_transactions_button) {
            val recyclerView = if (id == R.id.month_transactions_button) monthRecycler else dayRecycler
            val newVisibility = if (recyclerView.getVisibility() == View.VISIBLE) View.GONE else View.VISIBLE
            recyclerView.setVisibility(newVisibility)
            if (newVisibility == View.VISIBLE) {
                val args = Bundle()
                args.putParcelable(TransactionsLoader.ARG_PERIODS, periods)
                loaderManager.restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener)
            } else {
                (v as Button).setText(R.string.overview_transactions_show)
            }
        } else if (id == R.id.before_button) {
            changeMonth(PERIOD_BEFORE)
        } else if (id == R.id.next_button) {
            changeMonth(PERIOD_NEXT)
        } else if (id == R.id.day_before_button) {
            changeDay(PERIOD_BEFORE)
        } else if (id == R.id.day_next_button) {
            changeDay(PERIOD_NEXT)
        }
    }

    private val regularsLoaderListener = object : LoaderManager.LoaderCallbacks<ArrayList<RegularModel>> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<ArrayList<RegularModel>> {
            return RegularsLoader(this@OverviewActivity, dbHelper!!)
        }

        override fun onLoadFinished(loader: Loader<ArrayList<RegularModel>>, data: ArrayList<RegularModel>?) {
            regulars = data
            updateOverview()
        }

        override fun onLoaderReset(loader: Loader<ArrayList<RegularModel>>) {}
    }

    private val transactionsListener = object : LoaderManager.LoaderCallbacks<ArrayList<TransactionsModel>> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<ArrayList<TransactionsModel>> {
            return TransactionsLoader(this@OverviewActivity, dbHelper!!, args!!)
        }

        override fun onLoadFinished(loader: Loader<ArrayList<TransactionsModel>>, data: ArrayList<TransactionsModel>?) {
            transactions = data
            periods = (loader as TransactionsLoader).periods!!
            onPeriodChanged()
            updateTransactions()
        }

        override fun onLoaderReset(loader: Loader<ArrayList<TransactionsModel>>) {}
    }

    private inner class UndoClickListener internal constructor(private val transaction: TransactionsModel) : View.OnClickListener {

        override fun onClick(v: View) {
            transaction.isRemoved = false
            val tdt = TransactionsDeleteTask(this@OverviewActivity, transaction)
            tdt.execute()
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(OverviewActivity::class.java)

        private val STATE_REGULARS = "regulars"
        private val STATE_TRANSACTIONS = "transactions"
        private val STATE_IS_VIEWING_TODAY = "isViewingToday"
        private val STATE_PERIODS = "periods"
        private val STATE_MONTH_RECYCLER_VISIBLE = "monthRecyclerVisible"
        private val STATE_DAY_RECYCLER_VISIBLE = "dayRecyclerVisible"
        private val STATE_PERIOD_HISTORY = "periodHistory"

        private val LOADER_ID_REGULARS = 0
        private val LOADER_ID_TRANSACTIONS = 1

        val REQUEST_TRANSACTION_NEW = 0
        val REQUEST_REGULARS_OVERVIEW = 1
        val REQUEST_SETTINGS = 2
        val REQUEST_TRANSACTION_EDIT = 3

        const val PERIOD_BEFORE = 1
        const val PERIOD_NEXT = 2
    }

}
