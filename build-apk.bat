@echo off
powershell -NoProfile -ExecutionPolicy Bypass -Command "& '%~dp0\build-apk.ps1'"
pause
