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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.bitmacht.workingtitle36.BuildConfig;

public class WidgetProvider extends AppWidgetProvider {

    private static final Logger logger = LoggerFactory.getLogger(WidgetProvider.class);


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (BuildConfig.DEBUG) {
            logger.trace("-");
        }
        context.startService(new Intent(context, WidgetService.class));
    }

    @Override
    public void onDisabled(Context context) {
        if (BuildConfig.DEBUG) {
            logger.trace("-");
        }
        context.stopService(new Intent(context, WidgetService.class));
    }
}
