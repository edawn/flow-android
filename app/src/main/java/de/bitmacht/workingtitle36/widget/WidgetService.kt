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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.app.TaskStackBuilder
import android.support.v4.graphics.ColorUtils
import android.widget.RemoteViews
import de.bitmacht.workingtitle36.*
import de.bitmacht.workingtitle36.db.DBManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import org.joda.time.DateTime
import org.joda.time.Interval
import java.util.*

class WidgetService : Service() {
    private var regularsDisposable = Disposables.disposed()
    private var transactionsDisposable = Disposables.disposed()

    private var regulars: ArrayList<RegularModel>? = null
    private var requestPeriods: Periods? = null
    private var transactions: ArrayList<TransactionsModel>? = null
    private var alpha = 0.5f

    private var alarmPendingIntent: PendingIntent? = null

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener({ _, key ->
        if (key == getString(R.string.pref_currency_key)) updateWidget()
    })

    override fun onCreate() {
        super.onCreate()
        logd("-")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logd("-")
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val startOfNextDay = DateTime.now().plusDays(1).withTimeAtStartOfDay().millis
        alarmPendingIntent = PendingIntent.getService(this, 0, Intent(this, WidgetService::class.java), 0)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            alarmManager.set(AlarmManager.RTC, startOfNextDay, alarmPendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC, startOfNextDay, alarmPendingIntent)
        }

        alpha = Utils.getfPref(this, R.string.pref_widget_transparency_key, alpha)

        val widgetIds = AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, WidgetProvider::class.java))
        if (widgetIds.isNotEmpty()) startLoaders()

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(prefListener)

        return Service.START_STICKY
    }

    override fun onDestroy() {
        logd("-")

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(prefListener)

        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(alarmPendingIntent)

        transactionsDisposable.dispose()
        regularsDisposable.dispose()

        super.onDestroy()
    }

    private fun startLoaders() {
        transactionsDisposable.dispose()
        regularsDisposable.dispose()

        regulars = null
        requestPeriods = null
        transactions = null

        regularsDisposable = DBManager.instance.getRegularsObservable().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe { result ->
                    regulars = result.regulars
                    updateWidget()
                }

        transactionsDisposable = DBManager.instance.getTransactionsObservable(Periods())
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnNext(createTransactionsProcessor(Periods()))
                .subscribe()
    }

    private fun createTransactionsProcessor(periods: Periods): (DBManager.TransactionsResult) -> Unit = {
        logd("received transactions result: $it")
        requestPeriods = periods
        transactions = it.transactions
        updateWidget()
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun updateWidget() {
        if (regulars == null || requestPeriods == null || transactions == null) return

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

        val (_, remainingDay) = ValueUtils.calculateRemaining(regularsSum, spentDay, spentBeforeDay, currencyCode, periods)

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
}
