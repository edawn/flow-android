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

import android.annotation.SuppressLint
import android.app.LoaderManager
import android.content.*
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.IntDef
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import de.bitmacht.workingtitle36.db.DBLoader
import de.bitmacht.workingtitle36.db.DBTask
import de.bitmacht.workingtitle36.view.HoleyLayout
import kotlinx.android.synthetic.main.activity_overview.*
import org.joda.time.DateTime
import org.joda.time.Interval
import java.util.*

class OverviewActivity : AppCompatActivity() {

    private var helpScreen: HoleyLayout? = null

    @IntDef(PERIOD_BEFORE.toLong(), PERIOD_NEXT.toLong())
    @Retention(AnnotationRetention.SOURCE)
    annotation class PeriodModifier

    private lateinit var drawerToggle: ActionBarDrawerToggle

    private var regulars: ArrayList<RegularModel>? = null
    /** The transactions for the currently selected month */
    private var transactions: ArrayList<TransactionsModel>? = null
    private var hasTransactionsMonth = false
    private var hasTransactionsDay = false

    private lateinit var adapter: TransactionsArrayAdapter

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

    @SuppressLint("UseSparseArrays")
    private var periodHistory = HashMap<Long, Periods>()

    private val dataModifiedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            logd("received: $intent")

            loaderManager.restartLoader(LOADER_ID_TRANSACTIONS,
                    Bundle().apply { putParcelable(DBLoader.ARG_PERIODS, periods) }, transactionsListener)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logd("savedInstanceState: $savedInstanceState")

        setContentView(R.layout.activity_overview)

        if (Utils.getbPref(this, R.string.pref_first_time_key, true)) {
            help_screen.apply { helpScreen = this; visibility = View.VISIBLE }
            findViewById(R.id.hole_fab).apply {
                scaleX = 10f
                scaleY = 10f
                animate().scaleX(1f).scaleY(1f).interpolator = DecelerateInterpolator()
            }
        }

        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        drawerToggle = object : ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open, R.string.drawer_close) {}
        drawer_layout.addDrawerListener(drawerToggle)

        before_button.setOnClickListener({ changeMonth(PERIOD_BEFORE) })
        next_button.setOnClickListener({ changeMonth(PERIOD_NEXT) })

        fun showHideListenerFactory(recyclerView: RecyclerView) =
                View.OnClickListener { button ->
                    val newVisibility = if (recyclerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    recyclerView.visibility = newVisibility
                    if (newVisibility == View.VISIBLE) {
                        loaderManager.restartLoader(LOADER_ID_TRANSACTIONS,
                                Bundle().apply { putParcelable(DBLoader.ARG_PERIODS, periods) },
                                transactionsListener)
                    } else {
                        (button as Button).setText(R.string.overview_transactions_show)
                    }
                }
        month_transactions_button.setOnClickListener(showHideListenerFactory(transactions_month))
        day_transactions_button.setOnClickListener(showHideListenerFactory(transactions_day))

        transactions_month.layoutManager = LinearLayoutManager(this)
        adapter = TransactionsArrayAdapter()
        transactions_month.adapter = adapter

        day_before_button.setOnClickListener({ changeDay(PERIOD_BEFORE) })
        day_next_button.setOnClickListener({ changeDay(PERIOD_NEXT) })

        transactions_day.layoutManager = LinearLayoutManager(this)
        transactions_day.adapter = adapter.subAdapter

        val itemClickListener: ClickListener = { adapter, adapterPosition ->
            val transaction = (adapter as TransactionsArrayAdapter).getModel(adapterPosition)
            if (transaction != null) {
                startActivityForResult(Intent(this@OverviewActivity, TransactionEditActivity::class.java)
                        .apply { putExtra(TransactionEditActivity.EXTRA_TRANSACTION, transaction) }, REQUEST_TRANSACTION_EDIT)
            }
        }

        adapter.setOnItemClickListener(itemClickListener)
        adapter.subAdapter.setOnItemClickListener(itemClickListener)

        class SwipeCallback(private val adapter: TransactionsArrayAdapter) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val transaction = adapter.removeItem(viewHolder as BaseTransactionsAdapter<*>.BaseTransactionVH)
                transaction.isRemoved = true
                DBTask.createTransactionUpdateTask(this@OverviewActivity, transaction).execute()
                //TODO this should be shown only after a successful removal

                Snackbar.make(findViewById(android.R.id.content), R.string.snackbar_transaction_removed, Snackbar.LENGTH_LONG)
                        .setAction(R.string.snackbar_undo, {
                            transaction.isRemoved = false
                            DBTask.createTransactionUpdateTask(this@OverviewActivity, transaction).execute()
                        }).show()
                transactions!!.remove(transaction)
                updateOverview()
            }
        }

        ItemTouchHelper(SwipeCallback(adapter)).attachToRecyclerView(transactions_month)
        ItemTouchHelper(SwipeCallback(adapter.subAdapter)).attachToRecyclerView(transactions_day)

        fab.setOnClickListener { v -> startActivityForResult(Intent(v.context, TransactionEditActivity::class.java), REQUEST_TRANSACTION_NEW) }

        navigation.setNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener { item ->
            return@OnNavigationItemSelectedListener when (item.itemId) {
                R.id.menu_regular_transactions -> {
                    drawer_layout.closeDrawer(navigation)
                    startActivityForResult(Intent(this@OverviewActivity, OverviewRegularsActivity::class.java), REQUEST_REGULARS_OVERVIEW)
                    true
                }
                R.id.menu_settings -> {
                    drawer_layout.closeDrawer(navigation)
                    startActivityForResult(Intent(this@OverviewActivity, SettingsActivity::class.java), REQUEST_SETTINGS)
                    true
                }
                R.id.menu_about -> {
                    AboutDialogFragment().show(supportFragmentManager, null)
                    true
                }
                else -> false
            }
        })

        savedInstanceState?.apply {
            regulars = getParcelableArrayList<RegularModel>(STATE_REGULARS)
            transactions = getParcelableArrayList<TransactionsModel>(STATE_TRANSACTIONS)
            isViewingToday = getBoolean(STATE_IS_VIEWING_TODAY)
            periods = if (isViewingToday) Periods() else getParcelable<Parcelable>(STATE_PERIODS) as Periods
            if (getBoolean(STATE_MONTH_RECYCLER_VISIBLE)) {
                transactions_month.visibility = View.VISIBLE
            }
            if (getBoolean(STATE_DAY_RECYCLER_VISIBLE)) {
                transactions_day.visibility = View.VISIBLE
            }
            periodHistory = getSerializable(STATE_PERIOD_HISTORY) as HashMap<Long, Periods>
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
        logd("-")

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(dataModifiedReceiver, IntentFilter(DBTask.ACTION_DB_MODIFIED))

        loaderManager.initLoader(LOADER_ID_TRANSACTIONS,
                Bundle().apply { putParcelable(DBLoader.ARG_PERIODS, periods) }, transactionsListener)
    }

    override fun onStop() {
        logd("-")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataModifiedReceiver)
        super.onStop()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putParcelableArrayList(STATE_REGULARS, regulars)
            putParcelableArrayList(STATE_TRANSACTIONS, transactions)
            putBoolean(STATE_IS_VIEWING_TODAY, isViewingToday)
            //TODO if the timezone changes after this, a wrong period may be restored
            putParcelable(STATE_PERIODS, periods)
            putBoolean(STATE_MONTH_RECYCLER_VISIBLE, transactions_month.visibility == View.VISIBLE)
            putBoolean(STATE_DAY_RECYCLER_VISIBLE, transactions_day.visibility == View.VISIBLE)
            putSerializable(STATE_PERIOD_HISTORY, periodHistory)
        }
    }

    private fun setButtonEnabled(button: View, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1.0f else 0.26f
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            if (drawerToggle.onOptionsItemSelected(item)) true else super.onOptionsItemSelected(item)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_TRANSACTION_NEW, REQUEST_TRANSACTION_EDIT -> if (resultCode == RESULT_OK) {
                loaderManager.restartLoader(LOADER_ID_TRANSACTIONS,
                        Bundle().apply { putParcelable(DBLoader.ARG_PERIODS, periods) }, transactionsListener)
            }
            REQUEST_REGULARS_OVERVIEW -> if (resultCode == RESULT_OK) {
                loaderManager.restartLoader(LOADER_ID_REGULARS, null, regularsLoaderListener)
            }
            REQUEST_SETTINGS -> {
                //TODO check if settings actually changed before updating
                loaderManager.restartLoader(LOADER_ID_REGULARS, null, regularsLoaderListener)
                loaderManager.restartLoader(LOADER_ID_TRANSACTIONS,
                        Bundle().apply { putParcelable(DBLoader.ARG_PERIODS, periods) }, transactionsListener)
            }
        }
    }

    override fun onBackPressed() {
        when (drawer_layout.isDrawerOpen(navigation)) {
            true -> drawer_layout.closeDrawer(navigation)
            else -> super.onBackPressed()
        }
    }

    private fun changeMonth(@PeriodModifier periodModifier: Int) {
        var newPeriods = if (periodModifier == PERIOD_BEFORE) periods.previousLong else periods.nextLong
        periodHistory[newPeriods.longStart.millis]?.let { newPeriods = it }
        loaderManager.restartLoader(LOADER_ID_TRANSACTIONS,
                Bundle().apply { putParcelable(DBLoader.ARG_PERIODS, newPeriods) }, transactionsListener)
    }

    private fun changeDay(@PeriodModifier periodModifier: Int) {
        periods = (if (periodModifier == PERIOD_BEFORE) periods.previousShort else periods.nextShort) ?: return
        onPeriodChanged()
        updateTransactions()
    }

    private fun onPeriodChanged() {
        periodHistory.put(periods.longStart.millis, periods)

        val now = DateTime.now()
        // update the action bar
        // the next month would be in the future
        setButtonEnabled(next_button, !periods.longEnd.isAfter(now))
        supportActionBar!!.title = getString(R.string.overview_title, periods.longStart.toGregorianCalendar())

        // the first day of the month
        setButtonEnabled(day_before_button, periods.shortStart.isAfter(periods.longStart))
        // the last day of the month
        setButtonEnabled(day_next_button, periods.longEnd.isAfter(periods.shortEnd))

        isViewingToday = Interval(periods.shortStart, periods.shortPeriod).contains(now)
        dayLabel.text = if (isViewingToday)
            getString(R.string.overview_today)
        else
            getString(R.string.overview_day, periods.shortStart.dayOfMonth().get())
    }

    /**
     * Call this when the transactions have changed
     */
    private fun updateTransactions() {
        // filter the transactions so we get only the transactions performed on the selected day
        //TODO respect timezone
        //TODO applying to a result that does not include the current day makes only little sense
        //TODO somehow merge this with TransactionsArrayAdapter#setSubRange()

        with(ValueUtils.calculateSpent(transactions!!, MyApplication.currency.currencyCode, periods)) {
            spentDay = currentDay
            spentBeforeDay = beforeCurrentDay
            hasTransactionsMonth = hasTransactions
            hasTransactionsDay = hasTransactionsCurrentDay
        }
        adapter.setData(transactions!!, periods.shortStart.millis, periods.shortEnd.millis)
        updateOverview()
    }

    private fun updateOverview() {
        if (regulars == null || transactions == null || spentDay == null || spentBeforeDay == null) {
            logw("not initialized yet")
            return
        }

        val currencyCode = MyApplication.currency.currencyCode

        val transactionsSum = ValueUtils.calculateSpent(transactions!!, currencyCode).total
        val regularsSum = ValueUtils.calculateIncome(regulars!!, currencyCode, periods)
        val remainingSum = regularsSum.add(transactionsSum)

        logd("regsum: $regularsSum trsum: $transactionsSum rem: $remainingSum")

        month_balance_remain_value.text = remainingSum.string
        month_balance_spent_value.text = transactionsSum.withAmount(-transactionsSum.amount).string
        month_balance_available_value.text = regularsSum.string
        adjustExpandButton(month_transactions_button, hasTransactionsMonth, transactions_month)

        val remainingDay = ValueUtils.calculateRemaining(regularsSum, spentDay!!, spentBeforeDay!!, currencyCode, periods)

        day_balance_remain_value.text = remainingDay.currentDay.string
        day_balance_spent_value.text = spentDay!!.withAmount(-spentDay!!.amount).string
        day_balance_available_value.text = remainingDay.perDay.string
        adjustExpandButton(day_transactions_button, hasTransactionsDay, transactions_day)
    }

    private fun adjustExpandButton(expandButton: Button, setEnabled: Boolean, expandedView: View) {
        expandButton.apply {
            if (setEnabled) {
                isEnabled = true
                setText(if (expandedView.visibility == View.VISIBLE)
                    R.string.overview_transactions_hide
                else
                    R.string.overview_transactions_show)
            } else {
                isEnabled = false
                setText(R.string.overview_transactions_no)
            }
        }
    }

    private val regularsLoaderListener = object<T : ArrayList<RegularModel>> : LoaderManager.LoaderCallbacks<T> {

        override fun onCreateLoader(id: Int, args: Bundle?) =
                DBLoader.createRegularsLoader(this@OverviewActivity) as Loader<T>

        override fun onLoadFinished(loader: Loader<T>, data: T?) {
            regulars = data
            updateOverview()
        }

        override fun onLoaderReset(loader: Loader<T>) {}
    }

    private val transactionsListener = object<T : DBLoader.TransactionsResult> : LoaderManager.LoaderCallbacks<T> {
        override fun onCreateLoader(id: Int, args: Bundle?) =
                DBLoader.createTransactionsLoader(this@OverviewActivity, args!!) as Loader<T>

        override fun onLoadFinished(loader: Loader<T>, data: T?) {
            transactions = data!!.transactions
            periods = data.periods
            onPeriodChanged()
            updateTransactions()
        }

        override fun onLoaderReset(loader: Loader<T>) {}
    }

    companion object {

        private val STATE_REGULARS = "regulars"
        private val STATE_TRANSACTIONS = "transactions"
        private val STATE_IS_VIEWING_TODAY = "isViewingToday"
        private val STATE_PERIODS = "periods"
        private val STATE_MONTH_RECYCLER_VISIBLE = "transactions_monthVisible"
        private val STATE_DAY_RECYCLER_VISIBLE = "transactions_dayVisible"
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
