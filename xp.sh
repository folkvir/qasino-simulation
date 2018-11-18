#!/usr/bin/env bash
rm -rf xp.log
sh install.sh
LOG="xp.log"
HEAP="-Xms500000m"
JAR="-jar target/snob.jar"
xps=(snob-50-rps+son.txt snob-50-rps.txt snob-50-rps.txt snob-100-rps+son.txt snob-100-rps.txt snob-200-rps+son.txt snob-200-rps.txt)
for xp in ${xps[*]}
do
    echo "Running: " $xp
    java $(HEAP) $(JAR) $xp > $(LOG)
done
wait
