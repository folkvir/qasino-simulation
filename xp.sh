#!/usr/bin/env sh
# sh install.sh
LOG="xp.log"
HEAP="-Xms500000m"
JAR="-jar target/snob.jar"
xps[0]=snob-50-rps+son.txt
xps[1]=snob-50-rps.txt
xps[2]=snob-50-rps.txt
xps[3]=snob-100-rps+son.txt
xps[4]=snob-100-rps.txt
xps[5]=snob-200-rps+son.txt
xps[6]=snob-200-rps.txt
for xp in ${xps[*]}
do
    echo "Running: " $xp
    java ${HEAP} ${JAR} ${xp}
done
wait
