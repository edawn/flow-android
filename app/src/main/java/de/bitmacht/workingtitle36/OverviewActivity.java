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
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

public class OverviewActivity extends AppCompatActivity {
    
    private static final Logger logger = LoggerFactory.getLogger(OverviewActivity.class);

    private static final int LOADER_ID_REGULARS = 0;
    private static final int LOADER_ID_REGULAR_TRANSACTIONS = 1;

    private DBHelper dbHelper;
    private TextView monthView;
    private TextView dayView;

    private List<RegularModel> regulars = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        monthView = (TextView) findViewById(R.id.month);
        dayView = (TextView) findViewById(R.id.day);

        dbHelper = new DBHelper(this);

        getLoaderManager().initLoader(LOADER_ID_REGULARS, null, cursorLoaderListener);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(v.getContext(), TransactionEditActivity.class), 0);
            }
        });
    }

    private void populate(List<RegularModel> regulars) {
        this.regulars = regulars;
        ArrayList<Value> values = new ArrayList<Value>(regulars.size());
        for (RegularModel regular : regulars) {
            values.add(regular.getExecutedValue());
        }
        try {
            Value sum = new Value(Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0).addAll(values);
            monthView.setText(getString(R.string.overview_budget_template, "?", sum.getString()));
            dayView.setText(getString(R.string.overview_budget_template, "?", "?"));
            if (BuildConfig.DEBUG) {
                logger.trace("sum: {}", sum);
            }
        } catch (Value.CurrencyMismatchException e) {
            if (BuildConfig.DEBUG) {
                logger.warn("adding values failed", e);
            }
        }
    }

    private LoaderManager.LoaderCallbacks<Cursor> cursorLoaderListener =
            new LoaderManager.LoaderCallbacks<Cursor>() {
                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    if (BuildConfig.DEBUG) {
                        logger.trace("-");
                    }
                    if (id == LOADER_ID_REGULARS) {
                        return new RegularsCursorLoader(OverviewActivity.this, dbHelper, args);
                    }
                    throw new IllegalArgumentException("Unknown id");
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                    if (BuildConfig.DEBUG) {
                        logger.trace("-");
                    }
                    if (loader instanceof RegularsCursorLoader) {
                        DateTime now = DateTime.now();
                        // this defines the current balancing period
                        // TODO this should be user-settable
                        DateTime periodStart = now.withDayOfMonth(1).withTimeAtStartOfDay();
                        DateTime periodEnd = now.plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay();

                        Bundle args = new Bundle();
                        args.putLong(ExecutedRegularsCursorLoader.ARG_START, periodStart.getMillis());
                        args.putLong(ExecutedRegularsCursorLoader.ARG_END, periodEnd.getMillis());
                        ArrayList<RegularModel> regulars = new ArrayList<>(data.getCount());
                        while (data.moveToNext()) {
                            regulars.add(new RegularModel(data));
                        }
                        args.putParcelableArrayList(ExecutedRegularsCursorLoader.ARG_REGULARS, regulars);
                        getLoaderManager().initLoader(LOADER_ID_REGULAR_TRANSACTIONS, args, executedRegularsLoaderListener);
                    }
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                    if (BuildConfig.DEBUG) {
                        logger.trace("");
                    }
                }
            };

    private LoaderManager.LoaderCallbacks<List<RegularModel>> executedRegularsLoaderListener =
            new LoaderManager.LoaderCallbacks<List<RegularModel>>() {
                @Override
                public Loader<List<RegularModel>> onCreateLoader(int id, Bundle args) {
                    if (BuildConfig.DEBUG) {
                        logger.trace("-");
                    }
                    if (id == LOADER_ID_REGULAR_TRANSACTIONS) {
                        return new ExecutedRegularsCursorLoader(OverviewActivity.this, dbHelper, args);
                    }
                    throw new IllegalArgumentException("Unknown id");
                }

                @Override
                public void onLoadFinished(Loader<List<RegularModel>> loader, List<RegularModel> data) {
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
                    populate(data);
                }

                @Override
                public void onLoaderReset(Loader<List<RegularModel>> loader) {
                    if (BuildConfig.DEBUG) {
                        logger.trace("-");
                    }
                }
            };

    private static class RegularsCursorLoader extends AsyncTaskLoader<Cursor> {
        private final DBHelper dbHelper;
        private Cursor cursor;

        RegularsCursorLoader(Context context, DBHelper dbHelper, Bundle args) {
            super(context);
            this.dbHelper = dbHelper;
        }

        @Override
        public Cursor loadInBackground() {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            return db.rawQuery(DBHelper.ACTIVE_REGULARS_QUERY, null);
        }

        @Override
        public void deliverResult(Cursor data) {
            cursor = data;
            super.deliverResult(data);
        }

        @Override
        protected void onStartLoading() {
            if (cursor != null) {
                deliverResult(cursor);
            }
            if (takeContentChanged() || cursor == null) {
                forceLoad();
            }
        }

        @Override
        protected void onReset() {
            super.onReset();
            onReleaseResources();
        }

        protected void onReleaseResources() {
            if (cursor == null) {
                return;
            }
            cursor.close();
            cursor = null;
        }
    }

    private static class ExecutedRegularsCursorLoader extends AsyncTaskLoader<List<RegularModel>> {
        /**
         * The start of the balancing period (including; in ms since the epoch; default: java.lang.Long.MIN_VALUE)
         */
        public static final String ARG_START = "period_start";
        /**
         * The start of the balancing period (including; in ms since the epoch; default: java.lang.Long.MAX_VALUE)
         */
        public static final String ARG_END = "period_end";
        /**
         * An ArrayList of {@link RegularModel}s for which the transactions shall be queried.
         */
        public static final String ARG_REGULARS = DBHelper.REGULARS_TABLE_NAME;

        private final DBHelper dbHelper;
        private final Bundle args;
        private List<RegularModel> result;

        ExecutedRegularsCursorLoader(Context context, DBHelper dbHelper, Bundle args) {
            super(context);
            this.dbHelper = dbHelper;
            this.args = args;
        }

        @Override
        public List<RegularModel> loadInBackground() {
            long start = args.getLong(ARG_START, Long.MIN_VALUE);
            long end = args.getLong(ARG_END, Long.MAX_VALUE);
            ArrayList<RegularModel> regulars = args.getParcelableArrayList(ARG_REGULARS);

            DateTime now = null;

            if (regulars != null) {
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
            }
            return regulars;
        }

        @Override
        public void deliverResult(List<RegularModel> result) {
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
            onReleaseResources();
        }

        protected void onReleaseResources() {
            result = null;
        }
    }



}
