@echo off
echo ===============================
echo Iniciando Crossing Cr3...
echo Porta: 6003
echo ===============================

mvn compile exec:java -Dexec.mainClass="sd.traffic.crossing.CrossingProcess" -Dexec.args="Cr3 6103"

pause