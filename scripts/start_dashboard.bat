@echo off
echo ===============================
echo Iniciando DashboardHub...
echo ===============================

mvn clean compile exec:java -Dexec.mainClass=sd.traffic.dashboard.DashboardHub

pause