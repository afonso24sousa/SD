@echo off
title Entry Generator E1
echo ===============================
echo Iniciando EntryGenerator E1...
echo ID: E1
echo Lambda: 0.8
echo ===============================

mvn exec:java ^
  -Dexec.mainClass="sd.traffic.entry.EntryGeneratorProcess" ^
  -Dexec.args="E1 0.8"

pause
