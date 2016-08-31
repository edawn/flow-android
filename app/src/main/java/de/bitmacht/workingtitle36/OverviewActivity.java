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

package de.bitmacht.workingtitle36;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;

public class OverviewActivity extends AppCompatActivity implements View.OnClickListener {
    
    private static final Logger logger = LoggerFactory.getLogger(OverviewActivity.class);

    private static final String STATE_REGULARS = "regulars";
    private static final String STATE_TRANSACTIONS = "transactions";
    private static final String STATE_IS_VIEWING_TODAY = "isViewingToday";
    private static final String STATE_PERIODS = "periods";
    private static final String STATE_MONTH_RECYCLER_VISIBLE = "monthRecyclerVisible";
    private static final String STATE_DAY_RECYCLER_VISIBLE = "dayRecyclerVisible";
    private static final String STATE_PERIOD_HISTORY = "periodHistory";

    private static final int LOADER_ID_REGULARS = 0;
    private static final int LOADER_ID_TRANSACTIONS = 1;

    public static final int REQUEST_TRANSACTION_NEW = 0;
    public static final int REQUEST_REGULARS_OVERVIEW = 1;
    public static final int REQUEST_SETTINGS = 2;
    public static final int REQUEST_TRANSACTION_EDIT = 3;

    public static final int PERIOD_BEFORE = 1;
    public static final int PERIOD_NEXT = 2;

    private DBHelper dbHelper;

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ImageButton monthBeforeBtn;
    private ImageButton monthNextBtn;
    private TextView monthRemain;
    private TextView monthSpent;
    private TextView monthAvailable;
    private Button monthTransactionsButton;
    private RecyclerView monthRecycler;
    private TextView dayLabel;
    private ImageButton dayBeforeBtn;
    private ImageButton dayNextBtn;
    private TextView dayRemain;
    private TextView daySpent;
    private TextView dayAvailable;
    private Button dayTransactionsButton;
    private RecyclerView dayRecycler;
    private NavigationView navBar;

    @IntDef({PERIOD_BEFORE, PERIOD_NEXT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PeriodModifier {}

    private ActionBarDrawerToggle drawerToggle;

    private ArrayList<RegularModel> regulars = null;
    /** The transactions for the currently selected month*/
    private ArrayList<TransactionsModel> transactions = null;
    private boolean hasTransactionsMonth = false;
    private boolean hasTransactionsDay = false;

    private TransactionsArrayAdapter adapter;

    /**
     * This indicates that the user has chosen to view the current day; it takes precedence over
     * the short period in {@link #periods}
     */
    private boolean isViewingToday = true;
    private Periods periods = new Periods();
    /**
     * Value of the transactions between the start of the accounting period (including) and
     * the selected day
     */
    private Value spentBeforeDay = null;
    private Value spentDay = null;

    private HashMap<Long, Periods> periodHistory = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            logger.trace("savedInstanceState: {}", savedInstanceState);
        }

        dbHelper = new DBHelper(this);

        setContentView(R.layout.activity_overview);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        monthBeforeBtn = (ImageButton) findViewById(R.id.before_button);
        monthNextBtn = (ImageButton) findViewById(R.id.next_button);
        monthRemain = (TextView) findViewById(R.id.month_balance_remain_value);
        monthSpent = (TextView) findViewById(R.id.month_balance_spent_value);
        monthAvailable = (TextView) findViewById(R.id.month_balance_available_value);
        monthTransactionsButton = (Button) findViewById(R.id.month_transactions_button);
        monthRecycler = (RecyclerView) findViewById(R.id.transactions_month);
        dayLabel = (TextView) findViewById(R.id.dayLabel);
        dayBeforeBtn = (ImageButton) findViewById(R.id.day_before_button);
        dayNextBtn = (ImageButton) findViewById(R.id.day_next_button);
        dayRemain = (TextView) findViewById(R.id.day_balance_remain_value);
        daySpent = (TextView) findViewById(R.id.day_balance_spent_value);
        dayAvailable = (TextView) findViewById(R.id.day_balance_available_value);
        dayTransactionsButton = (Button) findViewById(R.id.day_transactions_button);
        dayRecycler = (RecyclerView) findViewById(R.id.transactions_day);
        navBar = (NavigationView) findViewById(R.id.navigation);

        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {};
        drawerLayout.addDrawerListener(drawerToggle);

        monthBeforeBtn.setOnClickListener(this);
        monthNextBtn.setOnClickListener(this);

        monthTransactionsButton.setOnClickListener(this);
        dayTransactionsButton.setOnClickListener(this);

        monthRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionsArrayAdapter();
        monthRecycler.setAdapter(adapter);

        dayBeforeBtn.setOnClickListener(this);
        dayNextBtn.setOnClickListener(this);

        dayRecycler.setLayoutManager(new LinearLayoutManager(this));
        dayRecycler.setAdapter(adapter.getSubAdapter());

        BaseTransactionsAdapter.OnItemClickListener itemClickListener = new BaseTransactionsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseTransactionsAdapter adapter, int adapterPosition) {
                TransactionsModel transaction = ((TransactionsArrayAdapter) adapter).getModel(adapterPosition);
                if (transaction != null) {
                    Intent intent = new Intent(OverviewActivity.this, TransactionEditActivity.class);
                    intent.putExtra(TransactionEditActivity.EXTRA_TRANSACTION, transaction);
                    startActivityForResult(intent, REQUEST_TRANSACTION_EDIT);
                }
            }
        };
        adapter.setOnItemClickListener(itemClickListener);
        adapter.getSubAdapter().setOnItemClickListener(itemClickListener);

        new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        TransactionsModel transaction = adapter.removeItem((BaseTransactionsAdapter.BaseTransactionVH) viewHolder);
                        transaction.isRemoved = true;
                        TransactionsDeleteTask tdt = new TransactionsDeleteTask(OverviewActivity.this, null, transaction);
                        tdt.execute();
                        //TODO this should be shown only after a successful removal
                        Snackbar.make(findViewById(android.R.id.content), R.string.snackbar_transaction_removed, Snackbar.LENGTH_LONG).
                                setAction(R.string.snackbar_undo, new UndoClickListener(transaction)).show();
                        transactions.remove(transaction);
                        updateOverview();
                    }
                }).attachToRecyclerView(monthRecycler);

        new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        TransactionsModel transaction = adapter.getSubAdapter().removeItem((BaseTransactionsAdapter.BaseTransactionVH) viewHolder);
                        transaction.isRemoved = true;
                        TransactionsDeleteTask tdt = new TransactionsDeleteTask(OverviewActivity.this, null, transaction);
                        tdt.execute();
                        //TODO this should be shown only after a successful removal
                        Snackbar.make(findViewById(android.R.id.content), R.string.snackbar_transaction_removed, Snackbar.LENGTH_LONG).
                                setAction(R.string.snackbar_undo, new UndoClickListener(transaction)).show();
                        transactions.remove(transaction);
                        updateOverview();
                    }
                }).attachToRecyclerView(dayRecycler);

        //noinspection ConstantConditions
        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(v.getContext(), TransactionEditActivity.class), REQUEST_TRANSACTION_NEW);
            }
        });

        navBar.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_regular_transactions) {
                    drawerLayout.closeDrawer(navBar);
                    startActivityForResult(new Intent(OverviewActivity.this, OverviewRegularsActivity.class), REQUEST_REGULARS_OVERVIEW);
                    return true;
                } else if (id == R.id.menu_settings) {
                    drawerLayout.closeDrawer(navBar);
                    startActivityForResult(new Intent(OverviewActivity.this, SettingsActivity.class), REQUEST_SETTINGS);
                    return true;
                } else if (id == R.id.menu_about) {
                    new AboutDialogFragment().show(getSupportFragmentManager(), null);
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState != null) {
            regulars = savedInstanceState.getParcelableArrayList(STATE_REGULARS);
            transactions = savedInstanceState.getParcelableArrayList(STATE_TRANSACTIONS);
            isViewingToday = savedInstanceState.getBoolean(STATE_IS_VIEWING_TODAY);
            periods = isViewingToday ? new Periods() : (Periods) savedInstanceState.getParcelable(STATE_PERIODS);
            if (savedInstanceState.getBoolean(STATE_MONTH_RECYCLER_VISIBLE)) {
                monthRecycler.setVisibility(View.VISIBLE);
            }
            if (savedInstanceState.getBoolean(STATE_DAY_RECYCLER_VISIBLE)) {
                dayRecycler.setVisibility(View.VISIBLE);
            }
            //noinspection unchecked
            periodHistory = (HashMap<Long, Periods>) savedInstanceState.getSerializable(STATE_PERIOD_HISTORY);
        }

        getLoaderManager().initLoader(LOADER_ID_REGULARS, null, regularsLoaderListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (BuildConfig.DEBUG) {
            logger.trace("-");
        }

        Bundle args = new Bundle();
        args.putParcelable(TransactionsLoader.ARG_PERIODS, periods);
        getLoaderManager().initLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_REGULARS, regulars);
        outState.putParcelableArrayList(STATE_TRANSACTIONS, transactions);
        outState.putBoolean(STATE_IS_VIEWING_TODAY, isViewingToday);
        //TODO if the timezone changes after this, a wrong period may be restored
        outState.putParcelable(STATE_PERIODS, periods);
        outState.putBoolean(STATE_MONTH_RECYCLER_VISIBLE, monthRecycler.getVisibility() == View.VISIBLE);
        outState.putBoolean(STATE_DAY_RECYCLER_VISIBLE, dayRecycler.getVisibility() == View.VISIBLE);
        outState.putSerializable(STATE_PERIOD_HISTORY, periodHistory);
    }

    private void setButtonEnabled(View button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.26f);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TRANSACTION_NEW || requestCode == REQUEST_TRANSACTION_EDIT) {
            if (resultCode == RESULT_OK) {
                Bundle args = new Bundle();
                args.putParcelable(TransactionsLoader.ARG_PERIODS, periods);
                getLoaderManager().restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);
            }
        } else if (requestCode == REQUEST_REGULARS_OVERVIEW) {
            if (resultCode == RESULT_OK) {
                getLoaderManager().restartLoader(LOADER_ID_REGULARS, null, regularsLoaderListener);
            }
        } else if (requestCode == REQUEST_SETTINGS) {
            //TODO check if settings actually changed before updating
            getLoaderManager().restartLoader(LOADER_ID_REGULARS, null, regularsLoaderListener);
            Bundle args = new Bundle();
            args.putParcelable(TransactionsLoader.ARG_PERIODS, periods);
            getLoaderManager().restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navBar)) {
            drawerLayout.closeDrawer(navBar);
        } else {
            super.onBackPressed();
        }
    }

    private void changeMonth(@PeriodModifier int periodModifier) {
        Periods newPeriods = periodModifier == PERIOD_BEFORE ? periods.previousLong() : periods.nextLong();
        Periods historicPeriods = periodHistory.get(newPeriods.getLongStart().getMillis());
        if (historicPeriods != null) {
            newPeriods = historicPeriods;
        }

        Bundle args = new Bundle();
        args.putParcelable(TransactionsLoader.ARG_PERIODS, newPeriods);
        getLoaderManager().restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);
    }

    private void changeDay(@PeriodModifier int periodModifier) {
        Periods newPeriods =
                periodModifier == PERIOD_BEFORE ? periods.previousShort() : periods.nextShort();

        if (newPeriods == null) {
            return;
        }

        periods = newPeriods;

        onPeriodChanged();

        updateTransactions();
    }

    private void onPeriodChanged() {
        periodHistory.put(periods.getLongStart().getMillis(), periods);

        DateTime now = DateTime.now();
        // update the action bar
        // the next month would be in the future
        setButtonEnabled(monthNextBtn, !periods.getLongEnd().isAfter(now));
        getSupportActionBar().setTitle(getString(R.string.overview_title, periods.getLongStart().toGregorianCalendar()));

        // the first day of the month
        setButtonEnabled(dayBeforeBtn, periods.getShortStart().isAfter(periods.getLongStart()));
        // the last day of the month
        setButtonEnabled(dayNextBtn, periods.getLongEnd().isAfter(periods.getShortEnd()));

        isViewingToday = new Interval(periods.getShortStart(), periods.getShortPeriod()).contains(now);
        dayLabel.setText(isViewingToday ? getString(R.string.overview_today) :
                getString(R.string.overview_day, periods.getShortStart().dayOfMonth().get()));
    }

    /**
     * Call this when the transactions have changed
     */
    private void updateTransactions() {
        // filter the transactions so we get only the transactions performed on the selected day
        //TODO respect timezone
        //TODO applying to a result that does not include the current day makes only little sense
        //TODO somehow merge this with TransactionsArrayAdapter#setSubRange()
        if (transactions == null) {
            return;
        }
        long startOfDayMillis = periods.getShortStart().getMillis();
        long endOfDayMillis = periods.getShortEnd().getMillis();
        String currencyCode = MyApplication.getCurrency().getCurrencyCode();
        Value valueBeforeDay = new Value(currencyCode, 0);
        Value valueDay = new Value(currencyCode, 0);
        hasTransactionsMonth = !transactions.isEmpty();
        hasTransactionsDay = false;
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
                    hasTransactionsDay = true;
                } catch (Value.CurrencyMismatchException e) {
                    if (BuildConfig.DEBUG) {
                        logger.warn("unable to add: {}", transact.mostRecentEdit);
                    }
                }
            }
        }
        adapter.setData(transactions, startOfDayMillis, endOfDayMillis);
        spentDay = valueDay;
        spentBeforeDay = valueBeforeDay;
        updateOverview();
    }

    private void updateOverview() {
        if (regulars == null || transactions == null || spentDay == null || spentBeforeDay == null) {
            if (BuildConfig.DEBUG) {
                logger.warn("not initialized yet");
            }
            return;
        }

        final String currencyCode = MyApplication.getCurrency().getCurrencyCode();

        Value transactionsSum = new Value(currencyCode, 0);
        if (transactions != null) {
            for (TransactionsModel transaction : transactions) {
                if (transaction.mostRecentEdit == null) {
                    if (BuildConfig.DEBUG) {
                        logger.warn("mostRecentEdit is null: transaction: {}", transaction);
                    }
                    continue;
                }
                try {
                    transactionsSum = transactionsSum.add(transaction.mostRecentEdit.getValue());
                } catch (Value.CurrencyMismatchException e) {
                    if (BuildConfig.DEBUG) {
                        logger.warn("adding value failed");
                    }
                }
            }
        }

        ArrayList<Value> regularsValues = new ArrayList<>(regulars.size());
        for (RegularModel regular : regulars) {
            if (regular.isDisabled) {
                continue;
            }
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

        Value remaining = new Value(currencyCode, 0);
        try {
            remaining = regularsSum.add(transactionsSum);
        } catch (Value.CurrencyMismatchException e) {
            if (BuildConfig.DEBUG) {
                logger.warn("subtraction failed", e);
            }
        }

        if (BuildConfig.DEBUG) {
            logger.trace("regsum: {} trsum: {} rem: {}", regularsSum, transactionsSum, regularsSum);
        }

        monthRemain.setText(remaining.getString());
        monthSpent.setText(transactionsSum.withAmount(-transactionsSum.amount).getString());
        monthAvailable.setText(regularsSum.getString());
        adjustExpandButton(monthTransactionsButton, hasTransactionsMonth, monthRecycler);

        int daysTotal = Days.daysBetween(periods.getLongStart(), periods.getLongEnd()).getDays();
        int daysBefore = Days.daysBetween(periods.getLongStart(), periods.getShortStart()).getDays();

        try {
            Value remainingFromDay = regularsSum.add(spentBeforeDay);
            Value remFromDayPerDay = remainingFromDay.withAmount(remainingFromDay.amount / (daysTotal - daysBefore));
            Value remainingDay = remFromDayPerDay.add(spentDay);

            dayRemain.setText(remainingDay.getString());
            daySpent.setText(spentDay.withAmount(-spentDay.amount).getString());
            dayAvailable.setText(remFromDayPerDay.getString());
            adjustExpandButton(dayTransactionsButton, hasTransactionsDay, dayRecycler);
        } catch (Value.CurrencyMismatchException e) {
            if (BuildConfig.DEBUG) {
                logger.warn("unable to add", e);
            }
        }
    }

    private void adjustExpandButton(Button expandButton, boolean isEnabled, View expandedView) {
        if (isEnabled) {
            expandButton.setEnabled(true);
            expandButton.setText(expandedView.getVisibility() == View.VISIBLE ?
                    R.string.overview_transactions_hide : R.string.overview_transactions_show);
        } else {
            expandButton.setEnabled(false);
            expandButton.setText(R.string.overview_transactions_no);
        }
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.month_transactions_button || id == R.id.day_transactions_button) {
            RecyclerView recyclerView = id == R.id.month_transactions_button ? monthRecycler : dayRecycler;
            int newVisibility = recyclerView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            recyclerView.setVisibility(newVisibility);
            if (newVisibility == View.VISIBLE) {
                Bundle args = new Bundle();
                args.putParcelable(TransactionsLoader.ARG_PERIODS, periods);
                getLoaderManager().restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);
            } else {
                ((Button) v).setText(R.string.overview_transactions_show);
            }
        } else if (id == R.id.before_button) {
            changeMonth(PERIOD_BEFORE);
        } else if (id == R.id.next_button) {
            changeMonth(PERIOD_NEXT);
        } else if (id == R.id.day_before_button) {
            changeDay(PERIOD_BEFORE);
        } else if (id == R.id.day_next_button) {
            changeDay(PERIOD_NEXT);
        }
    }

    private LoaderManager.LoaderCallbacks<ArrayList<RegularModel>> regularsLoaderListener =
            new LoaderManager.LoaderCallbacks<ArrayList<RegularModel>>() {
                @Override
                public Loader<ArrayList<RegularModel>> onCreateLoader(int id, Bundle args) {
                    return new RegularsLoader(OverviewActivity.this, dbHelper);
                }

                @Override
                public void onLoadFinished(Loader<ArrayList<RegularModel>> loader, ArrayList<RegularModel> data) {
                    regulars = data;
                    updateOverview();
                }

                @Override
                public void onLoaderReset(Loader<ArrayList<RegularModel>> loader) {
                }
            };

    LoaderManager.LoaderCallbacks<ArrayList<TransactionsModel>> transactionsListener =
            new LoaderManager.LoaderCallbacks<ArrayList<TransactionsModel>>() {
                @Override
                public Loader<ArrayList<TransactionsModel>> onCreateLoader(int id, Bundle args) {
                    return new TransactionsLoader(OverviewActivity.this, dbHelper, args);
                }

                @Override
                public void onLoadFinished(Loader<ArrayList<TransactionsModel>> loader, ArrayList<TransactionsModel> data) {
                    transactions = data;
                    periods = ((TransactionsLoader) loader).getPeriods();
                    onPeriodChanged();
                    updateTransactions();
                }

                @Override
                public void onLoaderReset(Loader<ArrayList<TransactionsModel>> loader) {}
            };

    private class UndoClickListener implements View.OnClickListener, TransactionsDeleteTask.UpdateFinishedCallback {

        private final TransactionsModel transaction;

        UndoClickListener(TransactionsModel transaction) {
            this.transaction = transaction;
        }

        @Override
        public void onClick(View v) {
            transaction.isRemoved = false;
            TransactionsDeleteTask tdt = new TransactionsDeleteTask(OverviewActivity.this, this, transaction);
            tdt.execute();
        }

        //TODO instead of this callback-in-a-listener approach, one might consider (simply) adding the transaction back to the adapter
        @Override
        public void onUpdateFinished(boolean success) {
            Bundle args = new Bundle();
            args.putParcelable(TransactionsLoader.ARG_PERIODS, periods);
            getLoaderManager().restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);
        }
    }

}
