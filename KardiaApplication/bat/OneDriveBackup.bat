@echo off
REM OneDriveBackup for Kardia data files and reports

if not exist "%userprofile%\OneDrive\Documents\App_Data_and_Reporting_Backups\Kardia\" mkdir %userprofile%\OneDrive\Documents\App_Data_and_Reporting_Backups\Kardia

xcopy *.xml %userprofile%\OneDrive\Documents\App_Data_and_Reporting_Backups\Kardia /Y

xcopy out\*.* %userprofile%\OneDrive\Documents\App_Data_and_Reporting_Backups\Kardia\out /I /Y
