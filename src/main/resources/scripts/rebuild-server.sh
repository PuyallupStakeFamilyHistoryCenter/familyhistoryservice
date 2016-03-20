#!/bin/sh

pushd ~/Programming/familyhistoryservice
git pull --rebase
mvn -Dmaven.test.skip=true install
popd

./relaunch-server.sh