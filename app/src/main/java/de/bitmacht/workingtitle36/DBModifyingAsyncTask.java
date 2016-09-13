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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.CallSuper;
import android.support.v4.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Any AsyncTask that modifies the underlying data necessary to calculate the budget should be
 * derived from this class.
 * This task will send the DB_MODIFIED broadcast after it finishes
 */
public abstract class DBModifyingAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(DBModifyingAsyncTask.class);

    public static final String ACTION_DB_MODIFIED = "de.bitmacht.workingtitle36.action.DB_MODIFIED";

    protected final Context appContext;

    protected DBModifyingAsyncTask(Context context) {
        appContext = context.getApplicationContext();
    }

    @CallSuper
    @Override
    protected void onPostExecute(Boolean success) {
        if (BuildConfig.DEBUG) {
            logger.trace("-");
        }
        //TODO add success extra
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(appContext);
        broadcastManager.sendBroadcast(new Intent(ACTION_DB_MODIFIED));
    }
}
