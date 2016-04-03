package de.bitmacht.workingtitle36;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionsOverviewActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final Logger logger = LoggerFactory.getLogger(TransactionsOverviewActivity.class);
    private TransactionsAdapter transactionsAdapter = null;
    private DBHelper dbHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            logger.trace(">");
        }
        setContentView(R.layout.activity_transactions_overview);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.transactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        float separatorHeight = getResources().getDimension(R.dimen.recycler_view_separator_height);
        recyclerView.addItemDecoration(new VerticalSpaceItemDecoration((int) separatorHeight));

        transactionsAdapter = new TransactionsAdapter();
        recyclerView.setAdapter(transactionsAdapter);

        dbHelper = new DBHelper(this);

        getLoaderManager().initLoader(0, null, this);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(v.getContext(), TransactionEditActivity.class), 0);
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (BuildConfig.DEBUG) {
            logger.trace("id: {}", id);
        }
        return new TransactionsCursorLoader(this, dbHelper);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (BuildConfig.DEBUG) {
            if (data == null) {
                logger.warn("cursor is null");
            } else {
                logger.trace("rows: {}", data.getCount());
            }
        }
        transactionsAdapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (BuildConfig.DEBUG) {
            logger.trace(">");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }
}
