#!/bin/sh

pkill -9 -f "FamilyHistoryCacheService.ja[r]"
sleep 1s
nohup java -jar FamilyHistoryCacheService.jar&