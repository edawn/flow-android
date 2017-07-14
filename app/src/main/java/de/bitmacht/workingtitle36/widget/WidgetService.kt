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

package de.bitmacht.workingtitle36.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.Build
import android.os.IBinder
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.graphics.ColorUtils
import android.widget.RemoteViews
import de.bitmacht.workingtitle36.*
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Interval
import java.util.*

class WidgetService : Service() {
    private var regularsLoader: DBLoader<ArrayList<RegularModel>>? = null
    private var transactionsLoader: DBLoader<DBLoader.TransactionsResult>? = null

    private var regulars: ArrayList<RegularModel>? = null
    private var requestPeriods: Periods? = null
    private var transactions: ArrayList<TransactionsModel>? = null
    private var alpha = 0.5f

    private var alarmPendingIntent: PendingIntent? = null

    private val dataModifiedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            logd("received: $intent")
            start()
        }
    }

    private val regularsLoadCompleteListener = Loader.OnLoadCompleteListener<ArrayList<RegularModel>> { loader, data ->
        regulars = data
        updateWidget()
    }

    private val transactionsLoadCompleteListener = Loader.OnLoadCompleteListener<DBLoader.TransactionsResult> { loader, data ->
        transactions = data!!.transactions
        requestPeriods = data.periods
        updateWidget()
    }

    override fun onCreate() {
        super.onCreate()
        logd("-")
        LocalBroadcastManager.getInstance(this).registerReceiver(dataModifiedReceiver, IntentFilter(DBTask.ACTION_DB_MODIFIED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logd("-")
        start()
        return Service.START_STICKY
    }

    private fun start() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val startOfNextDay = DateTime.now().plusDays(1).withTimeAtStartOfDay().millis
        val intent = Intent(this, WidgetService::class.java)
        alarmPendingIntent = PendingIntent.getService(this, 0, intent, 0)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            alarmManager.set(AlarmManager.RTC, startOfNextDay, alarmPendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC, startOfNextDay, alarmPendingIntent)
        }

        alpha = Utils.getfPref(this, R.string.pref_widget_transparency_key, alpha)

        val widgetIds = AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, WidgetProvider::class.java))
        if (widgetIds.size > 0) {
            startLoaders()
        }
    }

    override fun onDestroy() {
        logd("-")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataModifiedReceiver)

        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(alarmPendingIntent)

        transactionsLoader?.stopLoader(transactionsLoadCompleteListener)
        regularsLoader?.stopLoader(regularsLoadCompleteListener)

        super.onDestroy()
    }

    private inline fun <reified T> DBLoader<T>.stopLoader(listener: Loader.OnLoadCompleteListener<T>) {
        unregisterListener(listener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) cancelLoad()
        stopLoading()
    }

    private fun startLoaders() {
        regulars = null
        requestPeriods = null
        transactions = null

        regularsLoader = startLoader(regularsLoader, { DBLoader.createRegularsLoader(this) },
                LOADER_ID_REGULARS, regularsLoadCompleteListener)
        transactionsLoader = startLoader(transactionsLoader, { DBLoader.Companion.createTransactionsLoader(this, Periods()) },
                LOADER_ID_TRANSACTIONS, transactionsLoadCompleteListener)
    }
    
    private inline fun <reified T> startLoader(oldLoader: DBLoader<T>?, creator: () -> DBLoader<T>, id: Int,
                                               listener: Loader.OnLoadCompleteListener<T>): DBLoader<T> {
        oldLoader?.reset()
        return creator().apply { registerListener(id, listener); startLoading() }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun updateWidget() {
        if (regulars == null || requestPeriods == null || transactions == null) {
            return
        }

        val now = DateTime.now()

        if (!Interval(requestPeriods!!.longStart, requestPeriods!!.longPeriod).contains(now)) {
            // between the start of the loaders and their completion, a new long period (i.e. month) has begun,
            // so (at least) the TransactionsLoader should be restarted for the new long period at this point
            startLoaders()
            return
        }

        val periods = requestPeriods!!

        val currencyCode = MyApplication.currency.currencyCode

        val (spentDay, spentBeforeDay) = ValueUtils.calculateSpent(transactions!!, currencyCode, periods)
        val regularsSum = ValueUtils.calculateIncome(regulars!!, currencyCode, periods)

        val daysTotal = Days.daysBetween(periods.longStart, periods.longEnd).days
        val daysBefore = Days.daysBetween(periods.longStart, periods.shortStart).days

        // The amount one can spend during every short period (day) for the rest of the long period (month)
        // so that one's income and spending would even out
        val remainingDay = Value(currencyCode,
                regularsSum.add(spentBeforeDay).amount / (daysTotal - daysBefore)).add(spentDay)
        setWidgetValue(remainingDay)
    }

    private fun setWidgetValue(remaining: Value) {
        val remainingText = remaining.string
        logd("setting value: $remainingText")

        val widgetManager = AppWidgetManager.getInstance(this)
        val widgetIds = widgetManager.getAppWidgetIds(ComponentName(this, WidgetProvider::class.java))
        logd("ids: $widgetIds")

        var bgColor = resources.getColor(R.color.widgetBackground)
        val alphaInt = (255 * alpha).toInt()
        bgColor = ColorUtils.setAlphaComponent(bgColor, alphaInt)

        val N = widgetIds.size
        for (i in 0..N - 1) {
            val widgetId = widgetIds[i]
            val views = RemoteViews(this.packageName, R.layout.widget)

            views.setInt(R.id.container, "setBackgroundColor", bgColor)

            views.setTextViewText(R.id.value_button, remainingText)
            var intent = Intent(this, OverviewActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            var pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            views.setOnClickPendingIntent(R.id.value_button, pendingIntent)

            intent = Intent(this, TransactionEditActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            val stackBuilder = TaskStackBuilder.create(this)
            stackBuilder.addParentStack(TransactionEditActivity::class.java).addNextIntent(intent)
            pendingIntent = stackBuilder.getPendingIntent(0, 0)
            views.setOnClickPendingIntent(R.id.new_transaction_button, pendingIntent)

            widgetManager.updateAppWidget(widgetId, views)
        }

    }

    companion object {
        val LOADER_ID_REGULARS = 0
        val LOADER_ID_TRANSACTIONS = 1
    }

}
