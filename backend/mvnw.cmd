@echo off
setlocal
set WRAPPER_DIR=%~dp0.mvn\wrapper
set MAVEN_HOME=%WRAPPER_DIR%\apache-maven-3.9.9
set MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd
if exist "%MAVEN_CMD%" goto run
if "%JAVA_HOME%"=="" (
  echo JAVA_HOME must point to a Java 21 JDK before running the Maven wrapper.
  exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $dir='%WRAPPER_DIR%'; New-Item -ItemType Directory -Force -Path $dir | Out-Null; $zip=Join-Path $dir 'apache-maven-3.9.9-bin.zip'; if (!(Test-Path $zip)) { Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip' -OutFile $zip }; Expand-Archive -Force $zip $dir"
:run
call "%MAVEN_CMD%" -f "%~dp0pom.xml" %*
