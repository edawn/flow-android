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

import android.content.Context
import dagger.Module
import dagger.Provides
import de.bitmacht.workingtitle36.db.DBHelper
import de.bitmacht.workingtitle36.db.DBManager
import javax.inject.Singleton

@Module
class DBModule {

    @Singleton
    @Provides fun provideDBManager(dbHelper: DBHelper) = DBManager(dbHelper)

    @Singleton
    @Provides fun provideDBHelper(appContext: Context) = DBHelper(appContext)
}