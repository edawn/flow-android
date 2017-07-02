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

package de.bitmacht.workingtitle36

import android.app.Activity
import android.app.LoaderManager
import android.content.Intent
import android.content.Loader
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import kotlinx.android.synthetic.main.activity_regulars_overview.*
import java.util.*

class OverviewRegularsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    private lateinit var regularsAdapter: RegularsAdapter

    private var regularsModified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = DBHelper(this)

        setContentView(R.layout.activity_regulars_overview)

        regulars.layoutManager = LinearLayoutManager(this)
        regularsAdapter = RegularsAdapter()
        regulars.adapter = regularsAdapter

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val regular = regularsAdapter.removeItem(viewHolder as BaseTransactionsAdapter<*>.BaseTransactionVH)
                RegularsRemoveTask(this@OverviewRegularsActivity, regular.id!!).execute()
                regularsModified = true
                setResult(Activity.RESULT_OK)
                Snackbar.make(findViewById(android.R.id.content), R.string.snackbar_transaction_removed, Snackbar.LENGTH_LONG)
                        .setAction(R.string.snackbar_undo, {
                            RegularsUpdateTask(this@OverviewRegularsActivity, regular).execute()
                        }).show()
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(regulars)

        regularsAdapter.setOnItemClickListener { adapter, position ->
            val regular = (adapter as RegularsAdapter).getModel(position)
            if (regular != null) {
                startActivityForResult(Intent(this@OverviewRegularsActivity, RegularEditActivity::class.java)
                        .apply { putExtra(RegularEditActivity.EXTRA_REGULAR, regular) }, REQUEST_REGULAR_EDIT)
            } else {
                logw("no such item")
            }
        }

        findViewById(R.id.fab).setOnClickListener { v -> startActivityForResult(Intent(v.context, RegularEditActivity::class.java), REQUEST_REGULAR_EDIT) }

        loaderManager.initLoader(LOADER_ID_REGULARS, null, regularsLoaderListener)

        savedInstanceState?.apply {
            if (getBoolean(REGULARS_MODIFIED_KEY)) {
                regularsModified = true
                setResult(Activity.RESULT_OK)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_REGULAR_EDIT && resultCode == Activity.RESULT_OK) {
            loaderManager.restartLoader(LOADER_ID_REGULARS, null, regularsLoaderListener)
            setResult(Activity.RESULT_OK)
            regularsModified = true
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (regularsModified) {
            outState.putBoolean(REGULARS_MODIFIED_KEY, true)
        }
    }

    private val regularsLoaderListener = object : LoaderManager.LoaderCallbacks<ArrayList<RegularModel>> {

        override fun onCreateLoader(id: Int, args: Bundle?): Loader<ArrayList<RegularModel>> =
                RegularsLoader(this@OverviewRegularsActivity, dbHelper)

        override fun onLoadFinished(loader: Loader<ArrayList<RegularModel>>, data: ArrayList<RegularModel>) =
                regularsAdapter.setData(data)

        override fun onLoaderReset(loader: Loader<ArrayList<RegularModel>>) {}
    }

    companion object {

        private val LOADER_ID_REGULARS = 0

        val REQUEST_REGULAR_EDIT = 0

        private val REGULARS_MODIFIED_KEY = "regulars_modified"
    }
}
