title HomeDash
cd cfg

@ECHO OFF

SET BIN_CMD=java com.ftpix.homedash.app.App
SET CLASSPATH=../bin;.;
SET LIB_PATH=../lib
SET BIN_PATH=../bin
SET PLUGIN_PATH=../plugins
set PATH=%PATH%;%BIN_PATH%;.;

FOR %%i IN ("%LIB_PATH%/*.jar") DO CALL ../cpappend.bat %LIB_PATH%/%%i;
FOR %%i IN ("%BIN_PATH%/*.jar") DO CALL ../cpappend.bat %BIN_PATH%/%%i;
FOR %%i IN ("%PLUGIN_PATH%/*.jar") DO CALL ../cpappend.bat %PLUGIN_PATH%/%%i;
@echo CLASSPATH=%CLASSPATH%

@ECHO ON
%BIN_CMD%

cd ../