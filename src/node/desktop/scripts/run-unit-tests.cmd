::
:: run-unit-tests.cmd
::
:: Copyright (C) 2022 by RStudio, PBC
::
:: Unless you have received this program directly from RStudio pursuant
:: to the terms of a commercial license agreement with RStudio, then
:: this program is licensed to you under the terms of version 3 of the
:: GNU Affero General Public License. This program is distributed WITHOUT
:: ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
:: MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
:: AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
::

@echo off
setlocal

call envvars.bat
echo Executing Electron unit tests
set RSTUDIO_DOCKER_WINDOWS=1
npm test
endlocal
