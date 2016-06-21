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

package de.bitmacht.workingtitle36.view;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.bitmacht.workingtitle36.Value;

public interface ValueWidget {
    /**
     * Sets the amount that will be displayed
     * @param value The amount
     * @return The text that will be displayed
     */
    @NonNull
    String setValue(@NonNull Value value);

    /**
     * Return the Value represented by this widget
     */
    @Nullable
    Value getValue();
}
