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

package de.bitmacht.workingtitle36

fun Any.logd(msg: String): Unit { android.util.Log.d(Thread.currentThread().stackTrace[3].let {"${it.className}: [${it.methodName}]"}, msg) }
fun Any.logd(msg: String, throwable: Throwable?): Unit { android.util.Log.d(Thread.currentThread().stackTrace[3].let {"${it.className}: [${it.methodName}]"}, msg, throwable) }

fun Any.logw(msg: String): Unit { android.util.Log.w(Thread.currentThread().stackTrace[3].let {"${it.className}: [${it.methodName}]"}, msg) }
fun Any.logw(msg: String, throwable: Throwable?): Unit { android.util.Log.w(Thread.currentThread().stackTrace[3].let {"${it.className}: [${it.methodName}]"}, msg, throwable) }

fun Any.loge(msg: String): Unit { android.util.Log.e(Thread.currentThread().stackTrace[3].let {"${it.className}: [${it.methodName}]"}, msg) }
fun Any.loge(msg: String, throwable: Throwable?): Unit { android.util.Log.e(Thread.currentThread().stackTrace[3].let {"${it.className}: [${it.methodName}]"}, msg, throwable) }
