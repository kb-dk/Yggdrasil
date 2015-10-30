@ECHO OFF
REM If JAVA_HOME is not set, use java.exe in execution path
if "%JAVA_HOME%" == "" (
   set JAVA=java
) else (
   set JAVA="%JAVA_HOME%\bin\java"
)

REM ${assembly.home.env.name} must point to home directory.
SET ${assembly.home.env.name}=%~dp0\..

if "%${assembly.config.env.name}%" == "" (
   set ${assembly.config.env.name}=%${assembly.home.env.name}%config
)

REM CP must contain a semicolon-separated list of JARs used.
SET CP=%${assembly.home.env.name}%;%${assembly.home.env.name}%config
FOR /R %${assembly.home.env.name}%/lib %%a in (*.jar) DO CALL :AddToPath %%a
REM ECHO %CP%
GOTO :EOF

:AddToPath
SET CP=%CP%;%1
GOTO :EOF
