**[0.12.1] 2020-02-04**
 - Fixed regression generating several errors in Event Log during inspection
 
**[0.12.0] 2020-02-01**
 - New: Min IDEA version raised from 2016 to 2018
 - New: Tidied up deprecations in the 2018 SDK
 - New: Fixed possible deadlock during inspection

**[0.11.2] 2020-02-01**
 - Fix #42: no linting when using `--init-hook` in the parameter field

**[0.11.1] 2019-09-15**
 - New: Improved error handling

**[0.11.0] 2019-01-02**
 - PyLint real-time inspection disabled by default as numerous users find running it in the background has a negative
   impact on their system performance
 - Fix #29: Implementing a better virtualenv detection

**[0.10.2] 2018-09-25**
 - Fix #26: SyntaxError: Non-UTF-8 code starting with '\x90' when interpreter is set on Windows

**[0.10.1] 2018-09-21**
 - Fix #22: PyLint absolute path not working on Windows
 - Fix #24: PyLint auto-detection not working on Windows

**[0.10.0] 2018-09-12**
- Fix #7: Support linting inside current Virtualenv
- Fix #19: Don't show the 'syntax-error' message for real-time scan
- New: Improved Pylint auto-detection
- New: Option to install Pylint if missing
- New: Settings button now opens File | Settings | Pylint
- New: Minimum compatibility version raised to 163.15529

**[0.9.0] 2018-09-04**
 - Info: I am aware of the venv import error but for now I only have a partial solution. If you want to help or just get updates on the issue, click [here](https://github.com/leinardi/pylint-pycharm/issues/7).
 - New: Showing better info to the user if Pylint is missing
 - New: Added ability to optionally specify a pylintrc
 - New: Added ability to optionally specify Pylint arguments

**[0.8.0] 2018-09-01**
 - New: Added missing type `info`
 - New: Autoscroll to Source is disabled by default

**[0.7.1] 2018-09-01**
 - New: Project has moved to [https://github.com/leinardi/pylint-pycharm](https://github.com/leinardi/pylint-pycharm)

**[0.7.0] 2018-08-31**
 - New: Added Scan Files Before Checkin
 - New: Added real-time scanning!
 - New: UX based on Checkstyle-IDEA plugin.
 - New: Plugin id and name changed (please remove manually the old plugin)

**[0.1.0]**
 - Initial release.
