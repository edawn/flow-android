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

import android.app.Dialog
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.SeekBar

import de.bitmacht.workingtitle36.R
import de.bitmacht.workingtitle36.Utils

class WidgetConfigureActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        val fragment = WidgetConfigureDialogFragment()
        fragment.show(supportFragmentManager, "dialog")
    }

    class WidgetConfigureDialogFragment : AppCompatDialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val contentView = LayoutInflater.from(activity).inflate(R.layout.dialog_widget_configure, null) as LinearLayout

            val appName = getString(R.string.app_name)

            val builder = AlertDialog.Builder(activity)
            builder.setTitle(getString(R.string.widget_config_title, appName)).setView(contentView)

            builder.setPositiveButton(android.R.string.ok) { dialog, which ->
                val context = context
                val seekbar = getDialog().findViewById(R.id.transparency_seekbar) as SeekBar
                val alpha = seekbar.progress.toFloat() / seekbar.max
                Utils.setfPref(context, R.string.pref_widget_transparency_key, alpha)

                context.startService(Intent(context, WidgetService::class.java))

                val activity = activity
                val intent = activity.intent
                val extras = intent.extras
                if (extras != null) {
                    val appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                    val resultValue = Intent()
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    activity.setResult(RESULT_OK, resultValue)
                }

                dismiss()
            }

            builder.setNegativeButton(android.R.string.cancel) { dialog, which -> dismiss() }

            return builder.create()
        }

        override fun onDismiss(dialog: DialogInterface?) {
            super.onDismiss(dialog)
            val activity = activity
            activity?.finish()
        }
    }
}
