@echo off
:: ============================================================================
:: ImageSorterPro — Remove "Open with ImageSorterPro" from Windows Explorer
:: Removes the right-click context menu entries added by context-menu-install.bat
:: ============================================================================

echo.
echo ============================================================
echo   ImageSorterPro — Context Menu Uninstaller
echo ============================================================
echo.

:: Remove right-click on folder entries
reg delete "HKCU\Software\Classes\Directory\shell\ImageSorterPro" /f >nul 2>&1

:: Remove right-click on folder background entries
reg delete "HKCU\Software\Classes\Directory\Background\shell\ImageSorterPro" /f >nul 2>&1

echo   [OK] Context menu entries removed successfully.
echo.
pause
