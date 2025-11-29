@echo off
echo ===============================
echo Iniciando Crossing Cr4...
echo Porta: 6004
echo ===============================

mvn compile exec:java -Dexec.mainClass="sd.traffic.crossing.CrossingProcess" -Dexec.args="Cr4 6104"

pause