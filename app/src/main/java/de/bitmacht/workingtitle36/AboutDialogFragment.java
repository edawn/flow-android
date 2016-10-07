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

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutDialogFragment extends AppCompatDialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LinearLayout contentView = (LinearLayout) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_about, null);
        TextView messageView = (TextView) contentView.findViewById(R.id.message);
        WebView licensesView = (WebView) contentView.findViewById(R.id.licenses);

        String appName = getString(R.string.app_name);

        Spanned message = Html.fromHtml(getString(R.string.about_message, appName));

        messageView.setText(message);
        messageView.setLinksClickable(true);
        messageView.setMovementMethod(LinkMovementMethod.getInstance());

        licensesView.loadUrl("file:///android_asset/licenses.html");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.about_title, appName)).
                setView(contentView).
                setPositiveButton(android.R.string.ok, null);
        return builder.create();
    }
}
