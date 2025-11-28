@echo off
title Entry Generator E3
echo ===============================
echo Iniciando EntryGenerator E3...
echo ID: E3
echo Lambda: 0.8
echo ===============================

mvn exec:java ^
  -Dexec.mainClass="sd.traffic.entry.EntryGeneratorProcess" ^
  -Dexec.args="E3 0.8"

pause
