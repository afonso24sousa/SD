@echo off
title START ALL - Traffic Simulation
echo ============================================
echo   INICIAR TODO O SISTEMA DISTRIBUÍDO
echo ============================================

REM --------------------------------------------------
REM 1. Coordinator
REM --------------------------------------------------
start "Coordinator" cmd /k mvn exec:java -Dexec.mainClass="sd.traffic.coordinator.CoordinatorServer"

REM --------------------------------------------------
REM 2. Dashboard
REM --------------------------------------------------
start "DashboardHub" cmd /k mvn exec:java -Dexec.mainClass="sd.traffic.dashboard.DashboardHub"

REM --------------------------------------------------
REM 3. Crossings (Cr1..Cr5)
REM --------------------------------------------------
start "Crossing Cr1" cmd /k mvn exec:java -Dexec.mainClass="sd.traffic.crossing.CrossingProcess" -Dexec.args="Cr1 6001"
start "Crossing Cr2" cmd /k mvn exec:java -Dexec.mainClass="sd.traffic.crossing.CrossingProcess" -Dexec.args="Cr2 6002"
start "Crossing Cr3" cmd /k mvn exec:java -Dexec.mainClass="sd.traffic.crossing.CrossingProcess" -Dexec.args="Cr3 6003"
start "Crossing Cr4" cmd /k mvn exec:java -Dexec.mainClass="sd.traffic.crossing.CrossingProcess" -Dexec.args="Cr4 6004"
start "Crossing Cr5" cmd /k mvn exec:java -Dexec.mainClass="sd.traffic.crossing.CrossingProcess" -Dexec.args="Cr5 6005"

REM --------------------------------------------------
REM 4. EntryGenerators (E1, E2, E3)
REM --------------------------------------------------
start "EntryGenerator E1" cmd /k mvn exec:java -Dexec.mainClass="sd.traffic.entry.EntryGeneratorProcess" -Dexec.args="E1 0.8"
start "EntryGenerator E2" cmd /k mvn exec:java -Dexec.mainClass="sd.traffic.entry.EntryGeneratorProcess" -Dexec.args="E2 0.8"
start "EntryGenerator E3" cmd /k mvn exec:java -Dexec.mainClass="sd.traffic.entry.EntryGeneratorProcess" -Dexec.args="E3 0.8"

echo ============================================
echo   TODO O SISTEMA FOI INICIADO!
echo   Cada processo abriu na sua própria janela.
echo ============================================

pause
