rem ---------------------------------------------------------------------------
rem Append to CLASSPATH
rem
rem $Id: $
rem ---------------------------------------------------------------------------
@echo off 
SET _TEMP=
 
REM Process the first argument
IF ""%1"" == """" GOTO end
SET _TEMP=%1
SHIFT
 
REM Process the remaining arguments. Paths with spaces are handled here
:setArgs
IF ""%1"" == """" GOTO doneSetArgs
SET _TEMP=%_TEMP% %1
SHIFT
GOTO setArgs
 
REM Build the classpath 
:doneSetArgs
SET CLASSPATH=%CLASSPATH%;%_TEMP%
GOTO end
 
:end