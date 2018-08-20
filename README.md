![pylint logo](https://github.com/leinardi/pylint-PyCharm-plugin/blob/master/pylint-logo.png)

# pylint-PyCharm-plugin

The plugin provides a simple terminal to run Pylint
from PyCharm with a single click or hotkey and easily navigate
through type checking results. The idea of the Pylint terminal is
different from the normal PyCharm type checking that highlights
the errors in a current file. The Pylint terminal shows errors in
all files in your project (even in those not currently open).
Pylint is a Python source code analyzer which looks for programming errors,
helps enforcing a coding standard and sniffs for some code smells 
(as defined in Martin Fowler's Refactoring book).

![pylint plugin screenshot](https://github.com/leinardi/pylint-PyCharm-plugin/blob/master/pylint-pylint.png)

## Installation steps

The plugin requires [pylint](https://github.com/PyCQA/pylint) to be installed.

1. Download [pylint-plugin.jar](https://github.com/leinardi/pylint-PyCharm-plugin/releases)
2. In PyCharm go to Preferences -> Plugins -> Install plugins from disc
   -> Select downloaded file -> Restart PyCharm when prompted.
3. After restart you should find the plugin in View -> Tool windows
   -> Pylint terminal

## Configuration

Normally, plugin should not require any configuration steps. However,
sometimes plugin cannot find `pylint` command because it doesn't have
the full environment. If the plugin says something like
`/bin/bash: pylint command not found` when you try to run Pylint,
then this is likely the cause. In this case right click in Pylint
terminal in PyCharm -> Configure plugin. Then enter the path where
Pylint is installed as PATH suffix. If you are using a virtual environment,
thih would look like `/my/project/bin`. If necessary, you can also
configure Pylint command to use your custom flags.

## Usage

You can pin the terminal to either side of PyCharm window: click
on window toolbar → Move. The current default is bottom, which
works best if you typically have only a few errors. If you are
working on legacy code with many Pylint errors, you may want to use
the ‘left’ or ‘right’ setting. Finally, if you have multiple
monitors you might find the floating mode convenient.

## Credits

This plugin is an adaptation for Pylint of the [Dropbox mypy-PyCharm-plugin](https://github.com/dropbox/mypy-PyCharm-plugin).

## License

```
Copyright 2018 Roberto Leinardi.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
```
