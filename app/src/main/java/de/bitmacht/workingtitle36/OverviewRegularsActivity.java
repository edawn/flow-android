package de.bitmacht.workingtitle36;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class OverviewRegularsActivity extends AppCompatActivity {
    
    private static final Logger logger = LoggerFactory.getLogger(OverviewRegularsActivity.class);

    private static final int LOADER_ID_REGULARS = 0;

    public static final int REQUEST_REGULAR_EDIT = 0;

    private static final String REGULARS_MODIFIED_KEY = "regulars_modified";

    private RecyclerView regularsRecycler;

    private DBHelper dbHelper;

    private boolean regularsModified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regulars_overview);

        regularsRecycler = (RecyclerView) findViewById(R.id.regulars);
        regularsRecycler.setLayoutManager(new LinearLayoutManager(this));
        regularsRecycler.setAdapter(new RegularsAdapter());
        ItemTouchHelper itemSwipeHelper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                    ((RegularsAdapter) regularsRecycler.getAdapter()).removeItem((BaseTransactionsAdapter.BaseTransactionVH) viewHolder);
                }
        });
        itemSwipeHelper.attachToRecyclerView(regularsRecycler);

        dbHelper = new DBHelper(this);

        getLoaderManager().initLoader(LOADER_ID_REGULARS, null, regularsLoaderListener);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(v.getContext(), RegularEditActivity.class), REQUEST_REGULAR_EDIT);
            }
        });

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(REGULARS_MODIFIED_KEY)) {
                regularsModified = true;
                setResult(RESULT_OK);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_REGULAR_EDIT && resultCode == RESULT_OK) {
            getLoaderManager().restartLoader(LOADER_ID_REGULARS, null, regularsLoaderListener);
            setResult(RESULT_OK);
            regularsModified = true;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (regularsModified) {
            outState.putBoolean(REGULARS_MODIFIED_KEY, true);
        }
    }

    private LoaderManager.LoaderCallbacks<ArrayList<RegularModel>> regularsLoaderListener =
            new LoaderManager.LoaderCallbacks<ArrayList<RegularModel>>() {

                @Override
                public Loader<ArrayList<RegularModel>> onCreateLoader(int id, Bundle args) {
                    return new RegularsLoader(OverviewRegularsActivity.this, dbHelper, args);
                }

                @Override
                public void onLoadFinished(Loader<ArrayList<RegularModel>> loader, ArrayList<RegularModel> data) {
                    ((RegularsAdapter) regularsRecycler.getAdapter()).setData(data);
                }

                @Override
                public void onLoaderReset(Loader<ArrayList<RegularModel>> loader) {
                }
            };

    private static class RegularsLoader extends AsyncTaskLoader<ArrayList<RegularModel>> {

        private final DBHelper dbHelper;
        private ArrayList<RegularModel> result;

        RegularsLoader(Context context, DBHelper dbHelper, Bundle args) {
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
}
