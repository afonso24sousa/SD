@echo off
echo ===============================
echo Iniciando Crossing Cr1...
echo Porta: 6001
echo ===============================

mvn compile exec:java -Dexec.mainClass="sd.traffic.crossing.CrossingProcess" -Dexec.args="Cr1 6101"

pause
