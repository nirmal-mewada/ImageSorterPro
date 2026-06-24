@echo off
:: ============================================================================
:: ImageSorterPro — Add "Open with ImageSorterPro" to Windows Explorer
:: Right-click on any folder to open it directly in ImageSorterPro.
:: Requires no admin rights (installs to current user registry).
:: ============================================================================

setlocal enabledelayedexpansion

:: --- Detect the app executable location ---
:: Try standard jpackage install paths, then fall back to script's own directory

set "EXE_PATH="

:: 1. Check if exe is in the same directory as this script (portable/bundled)
set "SCRIPT_DIR=%~dp0"
if exist "%SCRIPT_DIR%ImageSorterPro.exe" (
    set "EXE_PATH=%SCRIPT_DIR%ImageSorterPro.exe"
    goto :found
)
:: Check parent directory (scripts are in app/ subdirectory)
if exist "%SCRIPT_DIR%..\ImageSorterPro.exe" (
    for %%I in ("%SCRIPT_DIR%..") do set "EXE_PATH=%%~fI\ImageSorterPro.exe"
    goto :found
)

:: 2. Check per-user jpackage install location
if exist "%LOCALAPPDATA%\ImageSorterPro\ImageSorterPro.exe" (
    set "EXE_PATH=%LOCALAPPDATA%\ImageSorterPro\ImageSorterPro.exe"
    goto :found
)

:: 3. Check per-machine jpackage install location
if exist "%ProgramFiles%\ImageSorterPro\ImageSorterPro.exe" (
    set "EXE_PATH=%ProgramFiles%\ImageSorterPro\ImageSorterPro.exe"
    goto :found
)

:: 4. Check PATH
where ImageSorterPro.exe >nul 2>&1
if %errorlevel% equ 0 (
    for /f "delims=" %%i in ('where ImageSorterPro.exe') do set "EXE_PATH=%%i"
    goto :found
)

echo.
echo ERROR: Could not find ImageSorterPro.exe
echo Please install ImageSorterPro first, or place this script in the app directory.
echo.
pause
exit /b 1

:found
echo.
echo ============================================================
echo   ImageSorterPro — Context Menu Installer
echo ============================================================
echo.
echo   Executable: %EXE_PATH%
echo.
echo   This will add "Open with ImageSorterPro" to the right-click
echo   menu when you right-click on any folder in Windows Explorer.
echo.

:: --- Add registry entries ---

:: Right-click ON a folder
reg add "HKCU\Software\Classes\Directory\shell\ImageSorterPro" /ve /d "Open with ImageSorterPro" /f >nul 2>&1
reg add "HKCU\Software\Classes\Directory\shell\ImageSorterPro" /v "Icon" /d "\"%EXE_PATH%\",0" /f >nul 2>&1
reg add "HKCU\Software\Classes\Directory\shell\ImageSorterPro\command" /ve /d "\"%EXE_PATH%\" \"%%V\"" /f >nul 2>&1

:: Right-click on folder BACKGROUND (inside a folder, on empty space)
reg add "HKCU\Software\Classes\Directory\Background\shell\ImageSorterPro" /ve /d "Open with ImageSorterPro" /f >nul 2>&1
reg add "HKCU\Software\Classes\Directory\Background\shell\ImageSorterPro" /v "Icon" /d "\"%EXE_PATH%\",0" /f >nul 2>&1
reg add "HKCU\Software\Classes\Directory\Background\shell\ImageSorterPro\command" /ve /d "\"%EXE_PATH%\" \"%%V\"" /f >nul 2>&1

if %errorlevel% equ 0 (
    echo   [OK] Context menu entries added successfully!
    echo.
    echo   You can now right-click any folder in Explorer and select
    echo   "Open with ImageSorterPro" to start sorting images.
    echo.
    echo   To remove, run: context-menu-uninstall.bat
) else (
    echo   [ERROR] Failed to add registry entries.
    echo   Try running this script as administrator.
)

echo.
pause
