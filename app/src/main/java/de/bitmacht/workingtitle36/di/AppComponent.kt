/*
 * Copyright 2017 Kamil Sartys
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

package de.bitmacht.workingtitle36.di

import dagger.Component
import de.bitmacht.workingtitle36.OverviewActivity
import de.bitmacht.workingtitle36.OverviewRegularsActivity
import de.bitmacht.workingtitle36.RegularEditActivity
import de.bitmacht.workingtitle36.TransactionEditActivity
import de.bitmacht.workingtitle36.widget.WidgetService
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AppModule::class, DBModule::class))
interface AppComponent {
    fun inject(overviewActivity: OverviewActivity)
    fun inject(widgetService: WidgetService)
    fun inject(overviewRegularsActivity: OverviewRegularsActivity)
    fun inject(regularEditActivity: RegularEditActivity)
    fun inject(transactionEditActivity: TransactionEditActivity)
}