@echo off
REM OneDriveBackup for Kardia data files and reports

REM data files
copy *.xml %userprofile%\OneDrive\Documents\Kardia

REM report files
copy out\*.txt %userprofile%\OneDrive\Documents\Kardia
