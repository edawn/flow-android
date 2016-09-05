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

package de.bitmacht.workingtitle36.widget;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.CallSuper;

/**
 * Any AsyncTask that modifies the underlying data necessary to calculate the budget should be
 * derived from this class.
 * This will initiate an update of the widget(s) when the task finishes
 */
public abstract class WidgetAwareAsyncTask extends AsyncTask<Void, Void, Boolean> {

    protected final Context appContext;

    protected WidgetAwareAsyncTask(Context context) {
        appContext = context.getApplicationContext();
    }

    @CallSuper
    @Override
    protected void onPostExecute(Boolean success) {
        appContext.startService(new Intent(appContext, WidgetService.class));
    }
}
