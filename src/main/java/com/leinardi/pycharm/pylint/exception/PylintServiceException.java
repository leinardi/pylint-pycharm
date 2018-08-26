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

package com.leinardi.pycharm.pylint.exception;

/**
 * An exception that originates with the Pylint access layer (aka Pylint plugin service), but is <em>not</em>
 * a native PylintException.
 * <p><b>Important:</b> Be sure to throw it <em>only</em> from the 'csaccess' sourceset!</p>
 */
public class PylintServiceException extends PylintPluginException {

    public PylintServiceException(final String message) {
        super(message);
    }

    public PylintServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
