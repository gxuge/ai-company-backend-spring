@echo off
setlocal enabledelayedexpansion

cd /d %~dp0\..

set "TARGET_DIR=jeecg-module-system\jeecg-system-start\target"
set "JAR_NAME="

for /f "delims=" %%I in ('dir /b /a:-d /o-d "%TARGET_DIR%\jeecg-system-start-*.jar" 2^>nul') do (
  set "JAR_NAME=%%I"
  goto :foundJar
)

echo [ERROR] No JAR found: %TARGET_DIR%\jeecg-system-start-*.jar
echo Run: mvn -U -Pprod -DskipTests clean package -pl jeecg-module-system/jeecg-system-start -am
exit /b 1

:foundJar
echo Found JAR: %JAR_NAME%
copy /Y "%TARGET_DIR%\%JAR_NAME%" "docker-deploy\monolith\system\jeecg-system-start.jar" >nul
if errorlevel 1 (
  echo [ERROR] Copy failed.
  exit /b 1
)

echo Done. Output: docker-deploy\monolith\system\jeecg-system-start.jar
endlocal
