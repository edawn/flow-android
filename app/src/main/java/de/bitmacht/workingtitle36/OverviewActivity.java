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
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.view.ViewGroup;
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

    public static final String STATE_PERIOD_START = "periodStart";
    private static final String STATE_MONTH_RECYCLER_VISIBLE = "monthRecyclerVisible";
    private static final String STATE_DAY_RECYCLER_VISIBLE = "dayRecyclerVisible";
    private static final String STATE_SELECTED_DAY_FOR_PERIOD = "selectedDayForPeriod";

    private static final int LOADER_ID_REGULARS = 0;
    private static final int LOADER_ID_TRANSACTIONS = 1;

    public static final int REQUEST_TRANSACTION_NEW = 0;
    public static final int REQUEST_REGULARS_OVERVIEW = 1;
    public static final int REQUEST_SETTINGS = 2;
    public static final int REQUEST_TRANSACTION_EDIT = 3;

    public static final int PERIOD_NOW = 0;
    public static final int PERIOD_BEFORE = 1;
    public static final int PERIOD_NEXT = 2;
    public static final int PERIOD_UNCHANGED = 3;

    private DBHelper dbHelper;

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ImageButton monthBeforeBtn;
    private ImageButton monthNextBtn;
    private TextView monthView;
    private RecyclerView monthRecycler;
    private TextView dayLabel;
    private TextView dayView;
    private ImageButton dayBeforeBtn;
    private ImageButton dayNextBtn;
    private RecyclerView dayRecycler;
    private NavigationView navBar;

    @IntDef({PERIOD_NOW, PERIOD_BEFORE, PERIOD_NEXT, PERIOD_UNCHANGED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PeriodModifier {}

    private ActionBarDrawerToggle drawerToggle;

    /** this defines the current balancing period */
    DateTime periodStart = null;
    DateTime periodEnd = null;

    /** this defines the start of the start of the day for which more/detailed information will be shown */
    private DateTime startOfDay = null;

    private ArrayList<RegularModel> regulars = null;
    private ArrayList<TransactionsModel> transactions = null;

    private TransactionsArrayAdapter adapter;

    /**
     * Value of the transactions between the start of the accounting period (including) and
     * the selected day (excluding; relates to {@link OverviewActivity#startOfDay})
     */
    private Value spentBeforeDay = null;
    private Value spentDay = null;

    private HashMap<Long, Long> selectedDayForPeriod = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DBHelper(this);

        setContentView(R.layout.activity_overview);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        monthBeforeBtn = (ImageButton) findViewById(R.id.before_button);
        monthNextBtn = (ImageButton) findViewById(R.id.next_button);
        monthView = (TextView) findViewById(R.id.month);
        monthRecycler = (RecyclerView) findViewById(R.id.transactions_month);
        dayLabel = (TextView) findViewById(R.id.dayLabel);
        dayView = (TextView) findViewById(R.id.day);
        dayBeforeBtn = (ImageButton) findViewById(R.id.day_before_button);
        dayNextBtn = (ImageButton) findViewById(R.id.day_next_button);
        dayRecycler = (RecyclerView) findViewById(R.id.transactions_day);
        navBar = (NavigationView) findViewById(R.id.navigation);

        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {};
        drawerLayout.addDrawerListener(drawerToggle);

        monthBeforeBtn.setOnClickListener(this);
        monthNextBtn.setOnClickListener(this);

        monthView.setOnClickListener(this);

        monthRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionsArrayAdapter();
        monthRecycler.setAdapter(adapter);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) monthRecycler.getLayoutParams();
        lp.leftMargin = lp.rightMargin = monthView.getCompoundPaddingLeft();
        monthRecycler.setLayoutParams(lp);

        dayView.setOnClickListener(this);
        dayBeforeBtn.setOnClickListener(this);
        dayNextBtn.setOnClickListener(this);

        dayRecycler.setLayoutManager(new LinearLayoutManager(this));
        dayRecycler.setAdapter(adapter.getSubAdapter());
        lp = (ViewGroup.MarginLayoutParams) dayRecycler.getLayoutParams();
        lp.leftMargin = lp.rightMargin = dayView.getCompoundPaddingLeft();
        dayRecycler.setLayoutParams(lp);

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

        int monthTwisty = R.drawable.ic_navigate_next_black_24dp;
        int dayTwisty = R.drawable.ic_navigate_next_black_24dp;
        if (savedInstanceState != null) {
            periodStart = new DateTime(savedInstanceState.getLong(STATE_PERIOD_START));
            if (savedInstanceState.getBoolean(STATE_MONTH_RECYCLER_VISIBLE)) {
                monthTwisty = R.drawable.ic_keyboard_arrow_down_black_24dp;
                monthRecycler.setVisibility(View.VISIBLE);
            }
            if (savedInstanceState.getBoolean(STATE_DAY_RECYCLER_VISIBLE)) {
                dayTwisty = R.drawable.ic_keyboard_arrow_down_black_24dp;
                dayRecycler.setVisibility(View.VISIBLE);
            }
            //noinspection unchecked
            selectedDayForPeriod = (HashMap<Long, Long>) savedInstanceState.getSerializable(STATE_SELECTED_DAY_FOR_PERIOD);
        }
        monthView.setCompoundDrawablesWithIntrinsicBounds(monthTwisty, 0, 0, 0);
        dayView.setCompoundDrawablesWithIntrinsicBounds(dayTwisty, 0, 0, 0);
        changePeriod(PERIOD_UNCHANGED);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //TODO if the timezone changes after this, a wrong period may be restored
        outState.putLong(STATE_PERIOD_START, periodStart.getMillis());
        outState.putBoolean(STATE_MONTH_RECYCLER_VISIBLE, monthRecycler.getVisibility() == View.VISIBLE);
        outState.putBoolean(STATE_DAY_RECYCLER_VISIBLE, dayRecycler.getVisibility() == View.VISIBLE);
        outState.putSerializable(STATE_SELECTED_DAY_FOR_PERIOD, selectedDayForPeriod);
    }

    /**
     * Update the action bar
     */
    private void upDate() {
        if (periodEnd.isAfterNow()) {
            monthNextBtn.setEnabled(false);
            monthNextBtn.setAlpha(0.26f);
        } else {
            monthNextBtn.setEnabled(true);
            monthNextBtn.setAlpha(1.0f);
        }
        getSupportActionBar().setTitle(getString(R.string.overview_title, periodStart.toGregorianCalendar()));
    }

    private void setStartOfDay(DateTime newStartOfDay) {
        if (newStartOfDay.equals(startOfDay)) {
            return;
        }
        startOfDay = newStartOfDay;
        selectedDayForPeriod.put(periodStart.getMillis(), startOfDay.getMillis());

        if (periodStart.isAfter(startOfDay.minusDays(1))) {
            dayBeforeBtn.setEnabled(false);
            dayBeforeBtn.setAlpha(0.26f);
        } else {
            dayBeforeBtn.setEnabled(true);
            dayBeforeBtn.setAlpha(1.0f);
        }
        boolean isToday = new Interval(DateTime.now().withTimeAtStartOfDay(), Days.ONE).contains(startOfDay);
        if (!periodEnd.isAfter(startOfDay.plusDays(1)) || isToday) {
            dayNextBtn.setEnabled(false);
            dayNextBtn.setAlpha(0.26f);
        } else {
            dayNextBtn.setEnabled(true);
            dayNextBtn.setAlpha(1.0f);
        }
        if (isToday) {
            dayLabel.setText(getString(R.string.overview_today));
        } else {
            dayLabel.setText(getString(R.string.overview_day, startOfDay.dayOfMonth().get()));
        }
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
                args.putLong(TransactionsLoader.ARG_START, periodStart.getMillis());
                args.putLong(TransactionsLoader.ARG_END, periodEnd.getMillis());
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
            args.putLong(TransactionsLoader.ARG_START, periodStart.getMillis());
            args.putLong(TransactionsLoader.ARG_END, periodEnd.getMillis());
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

    private void changePeriod(@PeriodModifier int periodModifier) {
        DateTime now;
        if (periodModifier == PERIOD_NOW || periodStart == null) {
            now = DateTime.now();
        } else if (periodModifier == PERIOD_BEFORE) {
            now = periodStart.minusMonths(1);
        } else if (periodModifier == PERIOD_NEXT) {
            now = periodStart.plusMonths(1);
        } else { // PERIOD_UNCHANGED
            now = periodStart;
        }
        periodStart = now.withDayOfMonth(1).withTimeAtStartOfDay();
        periodEnd = now.plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay();

        Long newStartOfDay = selectedDayForPeriod.get(periodStart.getMillis());
        setStartOfDay(newStartOfDay != null ? new DateTime(newStartOfDay) :
                periodEnd.isAfterNow() ? DateTime.now().withTimeAtStartOfDay() : now.withTimeAtStartOfDay());

        upDate();

        getLoaderManager().restartLoader(LOADER_ID_REGULARS, null, regularsLoaderListener);

        Bundle args = new Bundle();
        args.putLong(TransactionsLoader.ARG_START, periodStart.getMillis());
        args.putLong(TransactionsLoader.ARG_END, periodEnd.getMillis());
        getLoaderManager().restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);
    }

    private void changeDay(@PeriodModifier int periodModifier) {
        if (periodStart == null || periodEnd == null || startOfDay == null) {
            return;
        }
        DateTime newStartOfDay;
        if (periodModifier == PERIOD_NOW) {
            newStartOfDay = DateTime.now();
        } else if (periodModifier == PERIOD_BEFORE) {
            newStartOfDay = startOfDay.minusDays(1);
        } else if (periodModifier == PERIOD_NEXT) {
            newStartOfDay = startOfDay.plusDays(1);
        } else { // PERIOD_UNCHANGED
            newStartOfDay = startOfDay;
        }

        // stay inside the bounds of the currently selected month
        if (newStartOfDay.isBefore(periodStart)) {
            newStartOfDay = periodStart;
        } else if (!newStartOfDay.isBefore(periodEnd)) {
            newStartOfDay = periodEnd.minusDays(1);
        }
        // enforce the assumption that there are no transactions in the future
        if (newStartOfDay.isAfterNow()) {
            newStartOfDay = DateTime.now();
        }
        setStartOfDay(newStartOfDay.withTimeAtStartOfDay());
        updateTransactions();
        updateOverview();
    }

    private void updateTransactions() {
        // filter the transactions so we get only the transactions performed on the selected day
        //TODO respect timezone
        //TODO applying to a result that does not include the current day makes only little sense
        //TODO somehow merge this with TransactionsArrayAdapter#setSubRange()
        long startOfDayMillis = startOfDay.getMillis();
        long endOfDayMillis = startOfDay.plusDays(1).getMillis();
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
        adapter.setData(transactions, startOfDayMillis, endOfDayMillis);
        spentDay = valueDay;
        spentBeforeDay = valueBeforeDay;
    }

    private void updateOverview() {
        if (regulars == null || spentDay == null || spentBeforeDay == null) {
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
            regularsValues.add(regular.getCumulativeValue(periodStart, periodEnd));
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

        int templateRes = R.string.overview_budget_plus_template;
        if (transactionsSum.amount < 0) {
            templateRes = R.string.overview_budget_minus_template;
            transactionsSum = transactionsSum.withAmount(Math.abs(transactionsSum.amount));
        }

        monthView.setText(getString(templateRes, regularsSum.getString(), transactionsSum.getString(), remaining.getString()));

        int daysTotal = Days.daysBetween(periodStart, periodEnd).getDays();
        int daysBefore = Days.daysBetween(periodStart, startOfDay).getDays();

        try {
            Value remainingFromDay = regularsSum.add(spentBeforeDay);
            Value remFromDayPerDay = remainingFromDay.withAmount(remainingFromDay.amount / (daysTotal - daysBefore));
            Value remainingDay = remFromDayPerDay.add(spentDay);
            templateRes = spentDay.amount < 0 ? R.string.overview_budget_minus_template : R.string.overview_budget_plus_template;
            dayView.setText(getString(templateRes, remFromDayPerDay.getString(),
                    spentDay.withAmount(Math.abs(spentDay.amount)).getString(), remainingDay.getString()));
        } catch (Value.CurrencyMismatchException e) {
            if (BuildConfig.DEBUG) {
                logger.warn("unable to add", e);
            }
        }
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.month || id == R.id.day) {
            TextView balanceView = id == R.id.month ? monthView : dayView;
            RecyclerView recyclerView = id == R.id.month ? monthRecycler : dayRecycler;
            int newVisibility = recyclerView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            recyclerView.setVisibility(newVisibility);
            if (newVisibility == View.VISIBLE) {
                Bundle args = new Bundle();
                args.putLong(TransactionsLoader.ARG_START, periodStart.getMillis());
                args.putLong(TransactionsLoader.ARG_END, periodEnd.getMillis());
                getLoaderManager().restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);
                balanceView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_keyboard_arrow_down_black_24dp, 0, 0, 0);
            } else {
                balanceView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_navigate_next_black_24dp, 0, 0, 0);
            }
        } else if (id == R.id.before_button) {
            changePeriod(PERIOD_BEFORE);
        } else if (id == R.id.next_button) {
            changePeriod(PERIOD_NEXT);
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
                    if (BuildConfig.DEBUG) {
                        logger.trace("-");
                    }
                    return new RegularsLoader(OverviewActivity.this, dbHelper);
                }

                @Override
                public void onLoadFinished(Loader<ArrayList<RegularModel>> loader, ArrayList<RegularModel> data) {
                    if (BuildConfig.DEBUG) {
                        logger.trace("-");
                    }
                    regulars = data;
                    updateOverview();
                }

                @Override
                public void onLoaderReset(Loader<ArrayList<RegularModel>> loader) {
                    if (BuildConfig.DEBUG) {
                        logger.trace("-");
                    }
                }
            };

    private static class RegularsLoader extends AsyncTaskLoader<ArrayList<RegularModel>> {
        private final DBHelper dbHelper;
        private ArrayList<RegularModel> result;

        RegularsLoader(Context context, DBHelper dbHelper) {
            super(context);
            this.dbHelper = dbHelper;
        }

        @Override
        public ArrayList<RegularModel> loadInBackground() {
            return DBHelper.queryRegulars(dbHelper);
        }

        @Override
        public void deliverResult(ArrayList<RegularModel> result) {
            this.result = result;
            super.deliverResult(result);
        }

        @Override
        protected void onStartLoading() {
            if (result != null) {
                deliverResult(result);
            }
            if (takeContentChanged() || result == null) {
                forceLoad();
            }
        }

        @Override
        protected void onReset() {
            super.onReset();
            result = null;
        }
    }

    LoaderManager.LoaderCallbacks<ArrayList<TransactionsModel>> transactionsListener =
            new LoaderManager.LoaderCallbacks<ArrayList<TransactionsModel>>() {
                @Override
                public Loader<ArrayList<TransactionsModel>> onCreateLoader(int id, Bundle args) {
                    return new TransactionsLoader(OverviewActivity.this, dbHelper, args);
                }

                @Override
                public void onLoadFinished(Loader<ArrayList<TransactionsModel>> loader, ArrayList<TransactionsModel> data) {
                    transactions = data;
                    updateTransactions();
                    updateOverview();
                }

                @Override
                public void onLoaderReset(Loader<ArrayList<TransactionsModel>> loader) {}
            };

    private static class TransactionsLoader extends AsyncTaskLoader<ArrayList<TransactionsModel>> {
        /**
         * The start of the interval for which the transactions shall be queried (including; in ms since the epoch; default: java.lang.Long.MIN_VALUE)
         */
        public static final String ARG_START = "interval_start";
        /**
         * The end of the interval for which the transactions shall be queried (excluding; in ms since the epoch; default: java.lang.Long.MAX_VALUE)
         */
        public static final String ARG_END = "interval_end";

        private final DBHelper dbHelper;
        private final Bundle args;
        private ArrayList<TransactionsModel> result;

        public TransactionsLoader(Context context, DBHelper dbHelper, Bundle args) {
            super(context);
            this.dbHelper = dbHelper;
            this.args = args;
        }

        @Override
        public ArrayList<TransactionsModel> loadInBackground() {
            long start = args.getLong(ARG_START, Long.MIN_VALUE);
            long end = args.getLong(ARG_END, Long.MAX_VALUE);

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(DBHelper.TRANSACTIONS_EDITS_TIME_SPAN_QUERY, new String[]{String.valueOf(start), String.valueOf(end)});

            ArrayList<TransactionsModel> result = new ArrayList<>(cursor.getCount());

            while (cursor.moveToNext()) {
                result.add(TransactionsModel.getInstanceWithEdit(cursor));
            }

            return result;
        }

        @Override
        public void deliverResult(ArrayList<TransactionsModel> result) {
            this.result = result;
            super.deliverResult(result);
        }

        @Override
        protected void onStartLoading() {
            if (result != null) {
                deliverResult(result);
            }
            if (takeContentChanged() || result == null) {
                forceLoad();
            }
        }

        @Override
        protected void onReset() {
            super.onReset();
            result = null;
        }
    }

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
            args.putLong(TransactionsLoader.ARG_START, periodStart.getMillis());
            args.putLong(TransactionsLoader.ARG_END, periodEnd.getMillis());
            getLoaderManager().restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);
        }
    }

}
