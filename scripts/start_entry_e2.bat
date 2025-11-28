@echo off
title Entry Generator E2
echo ===============================
echo Iniciando EntryGenerator E2...
echo ID: E2
echo Lambda: 0.8
echo ===============================

mvn exec:java ^
  -Dexec.mainClass="sd.traffic.entry.EntryGeneratorProcess" ^
  -Dexec.args="E2 0.8"

pause
