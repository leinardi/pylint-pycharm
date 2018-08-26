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

package com.leinardi.pycharm.pylint.util;

import javax.swing.ImageIcon;
import java.net.URL;

public final class Icons {

    private Icons() {
    }

    public static ImageIcon icon(final String iconPath) {
        final URL url = Icons.class.getResource(iconPath);
        if (url != null) {
            return new ImageIcon(url);
        }

        return null;
    }

}
