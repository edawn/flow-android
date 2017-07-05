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

import android.content.AsyncTaskLoader
import android.content.Context

import java.util.ArrayList

//TODO possibly implement onStopLoading and onCanceled
class RegularsLoader(context: Context, private val dbHelper: DBHelper) : AsyncTaskLoader<ArrayList<RegularModel>>(context) {
    private var result: ArrayList<RegularModel>? = null

    override fun loadInBackground(): ArrayList<RegularModel> = DBHelper.queryRegulars(dbHelper)

    override fun deliverResult(result: ArrayList<RegularModel>) {
        this.result = result
        super.deliverResult(result)
    }

    override fun onStartLoading() {
        if (result != null) {
            deliverResult(result!!)
        }
        if (takeContentChanged() || result == null) {
            forceLoad()
        }
    }

    override fun onReset() {
        super.onReset()
        result = null
    }
}

