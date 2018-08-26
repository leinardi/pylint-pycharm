/*
 * Copyright 2018 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leinardi.pycharm.pylint.plapi;

import com.squareup.moshi.Json;

/**
 * Pylint violation severity levels supported by this plugin.
 */
public enum SeverityLevel {
    @Json(name = "refactor")
    REFACTOR,
    @Json(name = "convention")
    CONVENTION,
    @Json(name = "warning")
    WARNING,
    @Json(name = "error")
    ERROR,
    @Json(name = "fatal")
    FATAL
}
