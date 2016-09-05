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

package de.bitmacht.workingtitle36.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Loader;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import de.bitmacht.workingtitle36.BuildConfig;
import de.bitmacht.workingtitle36.DBHelper;
import de.bitmacht.workingtitle36.MyApplication;
import de.bitmacht.workingtitle36.OverviewActivity;
import de.bitmacht.workingtitle36.Periods;
import de.bitmacht.workingtitle36.R;
import de.bitmacht.workingtitle36.RegularModel;
import de.bitmacht.workingtitle36.RegularsLoader;
import de.bitmacht.workingtitle36.TransactionEditActivity;
import de.bitmacht.workingtitle36.TransactionsLoader;
import de.bitmacht.workingtitle36.TransactionsModel;
import de.bitmacht.workingtitle36.Value;

public class WidgetService extends Service implements Loader.OnLoadCompleteListener {
    public static final int LOADER_ID_REGULARS = 0;
    public static final int LOADER_ID_TRANSACTIONS = 1;
    private static final Logger logger = LoggerFactory.getLogger(WidgetService.class);
    private DBHelper dbHelper;
    private RegularsLoader regularsLoader;
    private TransactionsLoader transactionsLoader;

    private ArrayList<RegularModel> regulars = null;
    private Periods requestPeriods = null;
    private ArrayList<TransactionsModel> transactions = null;


    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DBHelper(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (BuildConfig.DEBUG) {
            logger.trace("-");
        }
        startLoaders();
        return START_STICKY;
    }

    private void startLoaders() {
        regulars = null;
        requestPeriods = null;
        transactions = null;

        if (regularsLoader != null) {
            regularsLoader.reset();
        }
        regularsLoader = new RegularsLoader(this, dbHelper);
        //noinspection unchecked
        regularsLoader.registerListener(LOADER_ID_REGULARS, this);
        regularsLoader.startLoading();

        if (transactionsLoader != null) {
            transactionsLoader.reset();
        }

        transactionsLoader = new TransactionsLoader(this, dbHelper, new Periods());
        //noinspection unchecked
        transactionsLoader.registerListener(LOADER_ID_TRANSACTIONS, this);
        transactionsLoader.startLoading();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLoadComplete(Loader loader, Object data) {
        int id = loader.getId();
        if (BuildConfig.DEBUG) {
            logger.trace("id: {}", id);
        }
        if (id == LOADER_ID_REGULARS) {
            //noinspection unchecked
            regulars = (ArrayList<RegularModel>) data;
        } else if (id == LOADER_ID_TRANSACTIONS) {
            //noinspection unchecked
            transactions = (ArrayList<TransactionsModel>) data;
            requestPeriods = ((TransactionsLoader) loader).getPeriods();
        }

        updateWidget();
    }

    /**
     * hallo
     * {@link OverviewActivity#updateOverview()}
     */
    private void updateWidget() {
        if (regulars == null || requestPeriods == null || transactions == null) {
            return;
        }

        DateTime now = DateTime.now();

        if (!new Interval(requestPeriods.getLongStart(), requestPeriods.getLongPeriod()).contains(now)) {
            // between the start of the loaders and their completion, a new long period (i.e. month) has begun,
            // so (at least) the TransactionsLoader should be restarted for the new long period at this point
            startLoaders();
            return;
        }

        Periods periods = requestPeriods;

        //TODO deduplicate from OverviewActivity#updateTransactions() and OverviewActivity#updateOverview()
        //though there might be no non-ugly solution ...

        long startOfDayMillis = periods.getShortStart().getMillis();
        long endOfDayMillis = periods.getShortEnd().getMillis();
        String currencyCode = MyApplication.getCurrency().getCurrencyCode();
        Value valueBeforeDay = new Value(currencyCode, 0);
        Value valueDay = new Value(currencyCode, 0);
        for (TransactionsModel transact : transactions) {
            long transactionTime = transact.mostRecentEdit.transactionTime;
            if (transactionTime < startOfDayMillis) {
                try {
                    valueBeforeDay = valueBeforeDay.add(transact.mostRecentEdit.getValue());
                } catch (Value.CurrencyMismatchException e) {
                    if (BuildConfig.DEBUG) {
                        logger.warn("unable to add: {}", transact.mostRecentEdit);
                    }
                }
            } else if (transactionTime < endOfDayMillis) {
                try {
                    valueDay = valueDay.add(transact.mostRecentEdit.getValue());
                } catch (Value.CurrencyMismatchException e) {
                    if (BuildConfig.DEBUG) {
                        logger.warn("unable to add: {}", transact.mostRecentEdit);
                    }
                }
            }
        }
        Value spentDay = valueDay;
        Value spentBeforeDay = valueBeforeDay;

        ArrayList<Value> regularsValues = new ArrayList<>(regulars.size());
        for (RegularModel regular : regulars) {
            regularsValues.add(regular.getCumulativeValue(periods.getLongStart(), periods.getLongEnd()));
        }

        Value regularsSum = new Value(currencyCode, 0);
        try {
            regularsSum = regularsSum.addAll(regularsValues);
        } catch (Value.CurrencyMismatchException e) {
            if (BuildConfig.DEBUG) {
                logger.warn("adding values failed", e);
            }
        }

        int daysTotal = Days.daysBetween(periods.getLongStart(), periods.getLongEnd()).getDays();
        int daysBefore = Days.daysBetween(periods.getLongStart(), periods.getShortStart()).getDays();

        try {
            Value remainingFromDay = regularsSum.add(spentBeforeDay);
            Value remFromDayPerDay = remainingFromDay.withAmount(remainingFromDay.amount / (daysTotal - daysBefore));
            Value remainingDay = remFromDayPerDay.add(spentDay);
            setWidgetValue(remainingDay);
        } catch (Value.CurrencyMismatchException e) {
            if (BuildConfig.DEBUG) {
                logger.warn("unable to add", e);
            }
        }
    }

    private void setWidgetValue(Value remaining) {
        String remainingText = remaining.getString();
        if (BuildConfig.DEBUG) {
            logger.trace("setting value: {}", remainingText);
        }

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        int[] widgetIds = widgetManager.getAppWidgetIds(new ComponentName(this, WidgetProvider.class));
        if (BuildConfig.DEBUG) {
            logger.trace("ids: {}", widgetIds);
        }

        final int N = widgetIds.length;
        for (int i = 0; i < N; i++) {
            int widgetId = widgetIds[i];
            RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.widget);

            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, new Intent(this, TransactionEditActivity.class), 0);
            views.setOnClickPendingIntent(R.id.new_transaction_button, pendingIntent);

            views.setTextViewText(R.id.value_button, remainingText);
            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, OverviewActivity.class), 0);
            views.setOnClickPendingIntent(R.id.value_button, pendingIntent);


            widgetManager.updateAppWidget(widgetId, views);
        }

    }

}
