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
import java.util.Locale;
import java.util.TreeMap;

public class OverviewActivity extends AppCompatActivity {
    
    private static final Logger logger = LoggerFactory.getLogger(OverviewActivity.class);

    private static final int LOADER_ID_REGULARS = 0;

    private DBHelper dbHelper;
    private TextView monthView;
    private TextView dayView;

    // this defines the current balancing period
    DateTime periodStart = null;
    DateTime periodEnd = null;

    private ArrayList<RegularModel> regulars = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        monthView = (TextView) findViewById(R.id.month);
        dayView = (TextView) findViewById(R.id.day);

        // TODO this should be user-settable
        DateTime now = DateTime.now();
        periodStart = now.withDayOfMonth(1).withTimeAtStartOfDay();
        periodEnd = now.plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay();

        dbHelper = new DBHelper(this);

        Bundle args = new Bundle();
        args.putLong(RegularsCursorLoader.ARG_START, periodStart.getMillis());
        args.putLong(RegularsCursorLoader.ARG_END, periodEnd.getMillis());
        getLoaderManager().initLoader(LOADER_ID_REGULARS, args, regularsLoaderListener);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(v.getContext(), TransactionEditActivity.class), 0);
            }
        });
    }

    private void populate(ArrayList<RegularModel> regulars) {
        this.regulars = regulars;
        ArrayList<Value> values = new ArrayList<>(regulars.size());
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

    private LoaderManager.LoaderCallbacks<ArrayList<RegularModel>> regularsLoaderListener =
            new LoaderManager.LoaderCallbacks<ArrayList<RegularModel>>() {
                @Override
                public Loader<ArrayList<RegularModel>> onCreateLoader(int id, Bundle args) {
                    if (BuildConfig.DEBUG) {
                        logger.trace("-");
                    }
                    return new RegularsCursorLoader(OverviewActivity.this, dbHelper, args);
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
                    populate(data);
                }

                @Override
                public void onLoaderReset(Loader<ArrayList<RegularModel>> loader) {
                    if (BuildConfig.DEBUG) {
                        logger.trace("-");
                    }
                }
            };

    private static class RegularsCursorLoader extends AsyncTaskLoader<ArrayList<RegularModel>> {
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
        private ArrayList<RegularModel> result;

        RegularsCursorLoader(Context context, DBHelper dbHelper, Bundle args) {
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



}
