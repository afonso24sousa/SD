@echo off
echo ===============================
echo Iniciando Crossing Cr5...
echo Porta: 6005
echo ===============================

mvn compile exec:java -Dexec.mainClass="sd.traffic.crossing.CrossingProcess" -Dexec.args="Cr5 6105"

pause