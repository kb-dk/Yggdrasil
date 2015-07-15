@echo off
setlocal enableextensions

call %~dp0\env.cmd

if "%JAVA_DEBUG_OPTS%" == "" (
   set JAVA_DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044
)

if "%JAVA_OPTS%" == "" (
   set JAVA_OPTS=-Xms256m -Xmx1024m
)

%JAVA% %JAVA_DEBUG_OPTS% %JAVA_OPTS% -cp "%CP%" ${assembly.main.class.name} %*
