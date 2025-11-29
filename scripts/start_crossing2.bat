@echo off
echo ===============================
echo Iniciando Crossing Cr2...
echo Porta: 6002
echo ===============================

mvn compile exec:java -Dexec.mainClass="sd.traffic.crossing.CrossingProcess" -Dexec.args="Cr2 6102"

pause