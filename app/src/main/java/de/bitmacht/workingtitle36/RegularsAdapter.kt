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

import android.view.LayoutInflater
import android.view.ViewGroup


import de.bitmacht.workingtitle36.view.RegularView

class RegularsAdapter : BaseTransactionsAdapter<BaseTransactionsAdapter<*>.BaseTransactionVH>() {

    private var regulars: MutableList<RegularModel>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseTransactionsAdapter<*>.BaseTransactionVH {
        val inflater = LayoutInflater.from(parent.context)
        val regularView = inflater.inflate(R.layout.regular_view, parent, false) as RegularView
        return this.ClickableTransactionVH(regularView)
    }

    override fun onBindViewHolder(holder: BaseTransactionsAdapter<*>.BaseTransactionVH, position: Int) {
        val regularView = holder.itemView as RegularView
        regularView.setData(regulars!![position])
        onPostBind(regularView)
    }

    override fun getItemCount(): Int {
        return if (regulars == null) 0 else regulars!!.size
    }

    fun setData(regulars: MutableList<RegularModel>) {
        this.regulars = regulars
        notifyDataSetChanged()
    }

    /**
     * Remove the item
     * @return The model of the item removed
     */
    fun removeItem(holder: BaseTransactionsAdapter<*>.BaseTransactionVH): RegularModel {
        val position = holder.adapterPosition
        val regular = regulars!!.removeAt(position)
        notifyItemRemoved(position)
        return regular
    }

    /**
     * Return the model associated with a adapter position
     * @return The model or null if there is no item for the given position
     */
    fun getModel(position: Int): RegularModel? {
        try {
            return regulars!![position]
        } catch (e: IndexOutOfBoundsException) {
            logd("No item at requested position: $position", e)
        }

        return null
    }
}
