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

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView

class AboutDialogFragment : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val contentView = LayoutInflater.from(activity).inflate(R.layout.dialog_about, null) as LinearLayout
        val messageView = contentView.findViewById(R.id.message) as TextView
        val licensesView = contentView.findViewById(R.id.licenses) as WebView

        val version = BuildConfig.VERSION_CODE.toString() + "/" + BuildConfig.VERSION_NAME
        val appName = getString(R.string.app_name)

        val message = Html.fromHtml(getString(R.string.about_message, version, appName))

        messageView.text = message
        messageView.linksClickable = true
        messageView.movementMethod = LinkMovementMethod.getInstance()

        licensesView.loadUrl("file:///android_asset/licenses.html")

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(getString(R.string.about_title, appName)).setView(contentView).setPositiveButton(android.R.string.ok, null)
        return builder.create()
    }
}
