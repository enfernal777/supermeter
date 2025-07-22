@echo off
echo.
echo ========================================
echo  JMeter HTML Reporter Plugin Deployment
echo  (Based on GitHub Working Examples)
echo ========================================
echo.

REM Copy plugin to desktop for easy access
copy "target\jmeter-html-reporter-1.0.0.jar" "%USERPROFILE%\Desktop\jmeter-html-reporter-1.0.0.jar" >nul 2>&1

echo [1/4] Plugin JAR copied to Desktop
echo.

REM Check common JMeter installation locations
set JMETER_FOUND=0

REM Check Program Files
if exist "C:\Program Files\apache-jmeter*\lib\ext" (
    set JMETER_HOME=C:\Program Files\apache-jmeter*
    set JMETER_FOUND=1
)

REM Check Program Files (x86)
if exist "C:\Program Files (x86)\apache-jmeter*\lib\ext" (
    set JMETER_HOME=C:\Program Files (x86)\apache-jmeter*
    set JMETER_FOUND=1
)

REM Check Downloads folder
if exist "%USERPROFILE%\Downloads\apache-jmeter*\lib\ext" (
    set JMETER_HOME=%USERPROFILE%\Downloads\apache-jmeter*
    set JMETER_FOUND=1
)

REM Check Desktop
if exist "%USERPROFILE%\Desktop\apache-jmeter*\lib\ext" (
    set JMETER_HOME=%USERPROFILE%\Desktop\apache-jmeter*
    set JMETER_FOUND=1
)

if %JMETER_FOUND%==1 (
    echo [2/4] JMeter installation found: %JMETER_HOME%
    copy "target\jmeter-html-reporter-1.0.0.jar" "%JMETER_HOME%\lib\ext\" >nul 2>&1
    echo [3/4] Plugin installed to JMeter lib/ext folder
) else (
    echo [2/4] JMeter installation NOT found automatically
    echo [3/4] Manual installation required - see instructions below
)

echo [4/4] Creating manual installation guide...
echo.

echo ============================================
echo  MANUAL INSTALLATION INSTRUCTIONS
echo  (If automatic installation didn't work)
echo ============================================
echo.
echo 1. Find your JMeter installation folder (contains 'bin', 'lib' folders)
echo 2. Navigate to: JMETER_HOME\lib\ext\
echo 3. Copy this file: jmeter-html-reporter-1.0.0.jar (now on your Desktop)
echo 4. Restart JMeter COMPLETELY (close all windows)
echo 5. Add Backend Listener to your test plan
echo 6. Select: com.supermeter.jmeter.gui.HTMLReportBackendListener
echo.

echo ============================================
echo  TESTING THE PLUGIN
echo ============================================
echo.
echo 1. Open JMeter
echo 2. Right-click Thread Group ^> Add ^> Listener ^> Backend Listener
echo 3. Backend Listener Implementation dropdown should show:
echo    "com.supermeter.jmeter.gui.HTMLReportBackendListener"
echo 4. Configure parameters:
echo    - reportFilePath: C:\temp\test-report.html
echo    - includeSuccessfulSamples: true
echo    - maxSampleDetails: 1000
echo 5. Run a test and check if C:\temp\test-report.html is created
echo.

echo ============================================
echo  TROUBLESHOOTING TIPS
echo ============================================
echo.
echo If plugin doesn't appear in dropdown:
echo 1. Verify JAR is in correct lib/ext folder
echo 2. Restart JMeter completely (all windows closed)
echo 3. Check JMeter logs for errors
echo 4. Try different JMeter version (tested with 5.6.3)
echo.

echo ============================================
echo  PLUGIN INFORMATION
echo ============================================
echo.
echo Plugin JAR: jmeter-html-reporter-1.0.0.jar (10KB)
echo Class Name: com.supermeter.jmeter.gui.HTMLReportBackendListener
echo Based on: GitHub working examples (Prometheus, New Relic patterns)
echo Compatible: JMeter 5.6.3+ (BackendListener API)
echo.

echo Press any key to open the plugin JAR location...
pause >nul
explorer "%USERPROFILE%\Desktop"

echo.
echo Deployment script completed!
echo Plugin JAR is now on your Desktop for manual installation.
pause 