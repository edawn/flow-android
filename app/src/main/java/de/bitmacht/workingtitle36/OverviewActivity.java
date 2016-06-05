package de.bitmacht.workingtitle36;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Pair;
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
import java.util.TreeMap;

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

    private TextView dayLabel;
    private ImageButton dayBeforeBtn;
    private ImageButton dayNextBtn;

    @IntDef({PERIOD_NOW, PERIOD_BEFORE, PERIOD_NEXT, PERIOD_UNCHANGED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PeriodModifier {}

    private ImageButton monthBeforeBtn;
    private ImageButton monthNextBtn;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navBar;
    private DBHelper dbHelper;
    private TextView monthView;
    private TextView dayView;

    /** this defines the current balancing period */
    DateTime periodStart = null;
    DateTime periodEnd = null;

    /** this defines the start of the start of the day for which more/detailed information will be shown */
    private DateTime startOfDay = null;

    private ArrayList<RegularModel> regulars = null;
    private ArrayList<TransactionsModel> transactions = null;

    private RecyclerView monthRecycler;
    private TransactionsArrayAdapter adapter;

    private RecyclerView dayRecycler;

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
        setContentView(R.layout.activity_overview);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        monthBeforeBtn = (ImageButton) findViewById(R.id.before_button);
        monthBeforeBtn.setOnClickListener(this);
        monthNextBtn = (ImageButton) findViewById(R.id.next_button);
        monthNextBtn.setOnClickListener(this);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {};

        drawerLayout.addDrawerListener(drawerToggle);

        navBar = (NavigationView) findViewById(R.id.navigation);
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
                    return true;
                }
                return false;
            }
        });

        monthView = (TextView) findViewById(R.id.month);
        monthView.setOnClickListener(this);
        dayView = (TextView) findViewById(R.id.day);
        dayView.setOnClickListener(this);

        dayLabel = (TextView) findViewById(R.id.dayLabel);
        dayBeforeBtn = (ImageButton) findViewById(R.id.day_before_button);
        dayBeforeBtn.setOnClickListener(this);
        dayNextBtn = (ImageButton) findViewById(R.id.day_next_button);
        dayNextBtn.setOnClickListener(this);

        monthRecycler = (RecyclerView) findViewById(R.id.transactions_month);
        monthRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionsArrayAdapter();
        monthRecycler.setAdapter(adapter);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) monthRecycler.getLayoutParams();
        lp.leftMargin = lp.rightMargin = monthView.getCompoundPaddingLeft();
        monthRecycler.setLayoutParams(lp);

        dayRecycler = (RecyclerView) findViewById(R.id.transactions_day);
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

        dbHelper = new DBHelper(this);

        if (savedInstanceState != null) {
            periodStart = new DateTime(savedInstanceState.getLong(STATE_PERIOD_START));
            if (savedInstanceState.getBoolean(STATE_MONTH_RECYCLER_VISIBLE)) {
                monthRecycler.setVisibility(View.VISIBLE);
            }
            if (savedInstanceState.getBoolean(STATE_DAY_RECYCLER_VISIBLE)) {
                dayRecycler.setVisibility(View.VISIBLE);
            }
            selectedDayForPeriod = (HashMap<Long, Long>) savedInstanceState.getSerializable(STATE_SELECTED_DAY_FOR_PERIOD);
        }
        changePeriod(PERIOD_UNCHANGED);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(v.getContext(), TransactionEditActivity.class), REQUEST_TRANSACTION_NEW);
            }
        });
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
                Bundle args = new Bundle();
                args.putLong(RegularsLoader.ARG_START, periodStart.getMillis());
                args.putLong(RegularsLoader.ARG_END, periodEnd.getMillis());
                getLoaderManager().restartLoader(LOADER_ID_REGULARS, args, regularsLoaderListener);
            }
        } else if (requestCode == REQUEST_SETTINGS) {
            //TODO check if settings actually changed before updating
            Bundle args = new Bundle();
            args.putLong(RegularsLoader.ARG_START, periodStart.getMillis());
            args.putLong(RegularsLoader.ARG_END, periodEnd.getMillis());
            getLoaderManager().restartLoader(LOADER_ID_REGULARS, args, regularsLoaderListener);
            args = new Bundle();
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

        Bundle args = new Bundle();
        args.putLong(RegularsLoader.ARG_START, periodStart.getMillis());
        args.putLong(RegularsLoader.ARG_END, periodEnd.getMillis());
        getLoaderManager().restartLoader(LOADER_ID_REGULARS, args, regularsLoaderListener);

        args = new Bundle();
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
            regularsValues.add(regular.getExecutedValue());
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
            RecyclerView recyclerView = id == R.id.month ? monthRecycler : dayRecycler;
            int newVisibility = recyclerView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            recyclerView.setVisibility(newVisibility);
            if (newVisibility == View.VISIBLE) {
                Bundle args = new Bundle();
                args.putLong(TransactionsLoader.ARG_START, periodStart.getMillis());
                args.putLong(TransactionsLoader.ARG_END, periodEnd.getMillis());
                getLoaderManager().restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);
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
                    return new RegularsLoader(OverviewActivity.this, dbHelper, args);
                }

                @Override
                public void onLoadFinished(Loader<ArrayList<RegularModel>> loader, ArrayList<RegularModel> data) {
                    if (BuildConfig.DEBUG) {
                        logger.trace("-");
                    }
                    for (RegularModel regular : data) {
                        logger.debug("regular: {}", regular.description);
                        if (regular.executed == null) {
                            continue;
                        }
                        for (TransactionsRegularModel transact : regular.executed) {
                            logger.debug("transact: {}/{}", transact.periodNumber, new DateTime(regular.getTimeForPeriodNumber(transact.periodNumber)));
                        }
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
        /**
         * The start of the balancing period (including; in ms since the epoch; default: java.lang.Long.MIN_VALUE)
         */
        public static final String ARG_START = "period_start";
        /**
         * The end of the balancing period (excluding; in ms since the epoch; default: java.lang.Long.MAX_VALUE)
         */
        public static final String ARG_END = "period_end";
        /**
         * An ArrayList of {@link RegularModel}s for which the transactions shall be queried.
         */
        public static final String ARG_REGULARS = DBHelper.REGULARS_TABLE_NAME;

        private final DBHelper dbHelper;
        private final Bundle args;
        private ArrayList<RegularModel> result;

        RegularsLoader(Context context, DBHelper dbHelper, Bundle args) {
            super(context);
            this.dbHelper = dbHelper;
            this.args = args;
        }

        @Override
        public ArrayList<RegularModel> loadInBackground() {
            long start = args.getLong(ARG_START, Long.MIN_VALUE);
            long end = args.getLong(ARG_END, Long.MAX_VALUE);
            ArrayList<RegularModel> regulars = args.getParcelableArrayList(ARG_REGULARS);

            DateTime now = null;

            if (regulars == null) {
                regulars = DBHelper.queryRegulars(dbHelper);
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                for (RegularModel regular : regulars) {
                    TreeMap<Integer, TransactionsRegularModel> executedMap = new TreeMap<>();

                    Pair<Integer, Integer> periodRange = regular.getPeriodNumberRange(start, end);

                    Cursor executedCursor = db.rawQuery(DBHelper.EXECUTED_REGULARS_BY_ID_PERIOD_RANGE_QUERY,
                            new String[]{String.valueOf(regular.id), String.valueOf(periodRange.first),
                                    String.valueOf(periodRange.second)});

                    while (executedCursor.moveToNext()) {
                        TransactionsRegularModel executed = new TransactionsRegularModel(executedCursor);
                        executedMap.put(executed.periodNumber, executed);
                    }

                    executedCursor.close();

                    for (int periodNumber = periodRange.first; periodNumber < periodRange.second; periodNumber++) {
                        if (executedMap.containsKey(periodNumber)) {
                            continue;
                        }
                        if (now == null) {
                            //TODO check time zone
                            now = new DateTime();
                        }
                        TransactionsRegularModel executed = new TransactionsRegularModel(regular.id, now.getMillis(), periodNumber);
                        try {
                            executed.insert(db);
                        } catch (SQLException e) {
                            logger.warn("insert failed", e);
                        }
                        executedMap.put(periodNumber, executed);
                    }
                    regular.executed = new ArrayList<>(executedMap.values());
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return regulars;
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
