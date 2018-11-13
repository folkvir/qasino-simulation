#!/usr/bin/env bash
sh install.sh

mvn exec:java@snob-50 &
mvn exec:java@snob-100 &
mvn exec:java@snob-200 &
mvn exec:java@snob-50-rps &
mvn exec:java@snob-100-rps &
mvn exec:java@snob-200-rps &
wait
