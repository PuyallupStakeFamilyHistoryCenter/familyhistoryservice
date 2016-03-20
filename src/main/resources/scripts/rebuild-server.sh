#!/bin/sh

pushd ~/Programming/familyhistoryservice
git pull --rebase
popd

./relaunch-server.sh