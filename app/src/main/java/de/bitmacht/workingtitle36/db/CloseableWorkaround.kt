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

package de.bitmacht.workingtitle36.db

import android.database.Cursor

/**
 * Created by kamil on 01.12.17.
 *
 * Workaround for Cursor not implementing Closeable in API level < 16
 * For more information see:
 * https://github.com/j256/ormlite-android/issues/20
 * https://stackoverflow.com/questions/39430179/kotlin-closable-and-sqlitedatabase-on-android
 * https://stackoverflow.com/questions/13878908/sqlitedatabase-does-not-implement-interface
 * Based on:
 * https://github.com/JetBrains/kotlin/blob/1.2.0/libraries/stdlib/src/kotlin/io/Closeable.kt
 */

/**
 * Executes the given [block] function on this resource and then closes it down correctly whether an exception
 * is thrown or not.
 *
 * @param block a function to process this [Closeable] resource.
 * @return the result of [block] function invoked on this resource.
 */
inline fun <T : Cursor?, R> T.use(block: (T) -> R): R {
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        when {
            this == null -> {}
            exception == null -> close()
            else ->
                try {
                    close()
                } catch (closeException: Throwable) {
                    // cause.addSuppressed(closeException) // ignored here
                }
        }
    }
}
