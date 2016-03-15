#!/bin/sh

pushd ~/Programming/familyhistoryservice
nohup mvn process-classes exec:exec -Dexec.args="-classpath %classpath org.puyallupfamilyhistorycenter.service.FamilyHistoryCacheServlet" -Dexec.executable=java &
popd
