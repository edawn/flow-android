package de.bitmacht.workingtitle36;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;
import java.util.TreeMap;

public class OverviewActivity extends AppCompatActivity implements View.OnClickListener {
    
    private static final Logger logger = LoggerFactory.getLogger(OverviewActivity.class);

    private static final int LOADER_ID_REGULARS = 0;
    private static final int LOADER_ID_TRANSACTIONS = 1;

    public static final int REQUEST_TRANSACTION_EDIT = 0;

    private DBHelper dbHelper;
    private TextView monthView;
    private TextView dayView;

    // this defines the current balancing period
    DateTime periodStart = null;
    DateTime periodEnd = null;

    private ArrayList<RegularModel> regulars = null;
    private ArrayList<TransactionsModel> transactions = null;
    private RecyclerView monthTransactions;
    private TransactionsArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        monthView = (TextView) findViewById(R.id.month);
        monthView.setOnClickListener(this);
        dayView = (TextView) findViewById(R.id.day);
        dayView.setOnClickListener(this);

        monthTransactions = (RecyclerView) findViewById(R.id.transactions_month);
        monthTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionsArrayAdapter();
        monthTransactions.setAdapter(adapter);

        // TODO this should be user-settable
        DateTime now = DateTime.now();
        periodStart = now.withDayOfMonth(1).withTimeAtStartOfDay();
        periodEnd = now.plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay();

        dbHelper = new DBHelper(this);

        Bundle args = new Bundle();
        args.putLong(RegularsLoader.ARG_START, periodStart.getMillis());
        args.putLong(RegularsLoader.ARG_END, periodEnd.getMillis());
        getLoaderManager().initLoader(LOADER_ID_REGULARS, args, regularsLoaderListener);

        args = new Bundle();
        args.putLong(TransactionsLoader.ARG_START, periodStart.getMillis());
        args.putLong(TransactionsLoader.ARG_END, periodEnd.getMillis());
        getLoaderManager().initLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(v.getContext(), TransactionEditActivity.class), REQUEST_TRANSACTION_EDIT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TRANSACTION_EDIT && monthTransactions.getVisibility() == View.VISIBLE) {
            Bundle args = new Bundle();
            args.putLong(TransactionsLoader.ARG_START, periodStart.getMillis());
            args.putLong(TransactionsLoader.ARG_END, periodEnd.getMillis());
            getLoaderManager().restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);
        }
    }

    private void updateOverview() {
        if (regulars == null) {
            if (BuildConfig.DEBUG) {
                logger.warn("regulars is null");
            }
            return;
        }

        Value transactionsSum = new Value(Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0);
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

        Value regularsSum = new Value(Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0);
        try {
            regularsSum = regularsSum.addAll(regularsValues);
        } catch (Value.CurrencyMismatchException e) {
            if (BuildConfig.DEBUG) {
                logger.warn("adding values failed", e);
            }
        }

        Value remaining = new Value(Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0);
        try {
            remaining = regularsSum.add(transactionsSum.withAmount(-transactionsSum.amount));
        } catch (Value.CurrencyMismatchException e) {
            if (BuildConfig.DEBUG) {
                logger.warn("subtraction failed", e);
            }
        }

        if (BuildConfig.DEBUG) {
            logger.trace("regsum: {} trsum: {} rem: {}", regularsSum, transactionsSum, regularsSum);
        }
        monthView.setText(getString(R.string.overview_budget_template, regularsSum.getString(), transactionsSum.getString(), remaining.getString()));
    }

    private void updateTransactions() {
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.month) {
            int newVisibility = monthTransactions.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            monthTransactions.setVisibility(newVisibility);

            if (newVisibility == View.VISIBLE) {
                Bundle args = new Bundle();
                args.putLong(TransactionsLoader.ARG_START, periodStart.getMillis());
                args.putLong(TransactionsLoader.ARG_END, periodEnd.getMillis());
                getLoaderManager().restartLoader(LOADER_ID_TRANSACTIONS, args, transactionsListener);
            }
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
                            new String[]{String.valueOf(regular.creationTime), String.valueOf(periodRange.first),
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
                        TransactionsRegularModel executed = new TransactionsRegularModel(regular.creationTime, now.getMillis(), periodNumber);
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
                    adapter.setData(transactions);
                    updateOverview();
                    updateTransactions();
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
}
