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

package de.bitmacht.workingtitle36.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.bitmacht.workingtitle36.BuildConfig

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (BuildConfig.DEBUG) {
            logger.trace("-")
        }
        context.startService(Intent(context, WidgetService::class.java))
    }

    override fun onDisabled(context: Context) {
        if (BuildConfig.DEBUG) {
            logger.trace("-")
        }
        context.stopService(Intent(context, WidgetService::class.java))
    }

    companion object {

        private val logger = LoggerFactory.getLogger(WidgetProvider::class.java)
    }
}
