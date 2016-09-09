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

import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import de.bitmacht.workingtitle36.R;
import de.bitmacht.workingtitle36.Utils;

public class WidgetConfigureActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        WidgetConfigureDialogFragment fragment = new WidgetConfigureDialogFragment();
        fragment.show(getSupportFragmentManager(), "dialog");
    }

    public static class WidgetConfigureDialogFragment extends AppCompatDialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LinearLayout contentView =
                    (LinearLayout) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_widget_configure, null);

            String appName = getString(R.string.app_name);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.widget_config_title, appName)).
                    setView(contentView);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Context context = getContext();
                    SeekBar seekbar = (SeekBar) getDialog().findViewById(R.id.transparency_seekbar);
                    float alpha = ((float) seekbar.getProgress())/seekbar.getMax();
                    Utils.setfPref(context, R.string.pref_widget_transparency_key, alpha);

                    context.startService(new Intent(context, WidgetService.class));

                    FragmentActivity activity = getActivity();
                    Intent intent = activity.getIntent();
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        int appWidgetId =
                                extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                        Intent resultValue = new Intent();
                        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        activity.setResult(RESULT_OK, resultValue);
                    }

                    dismiss();
                }
            });

            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });

            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }
}
