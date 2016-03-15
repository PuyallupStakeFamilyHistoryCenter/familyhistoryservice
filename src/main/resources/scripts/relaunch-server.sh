#!/bin/sh

lsof -i :8080 | grep java | awk '{print $2}' | uniq | xargs kill -9
sleep 1s
./run-server.sh