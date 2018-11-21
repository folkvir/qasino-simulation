#!/usr/bin/env bash
bash install.sh
PARALLEL=$1
PARA="--parallel"
LOG="xp.log"
HEAP="-Xms100000m"
JAR="-jar target/snob.jar"

java  ${HEAP} ${JAR} --init

for file in ./configs/generated/*.conf
do
    if [[ -f $file ]]; then
        F=$(basename "$file")
        if [[ "$PARALLEL" = "$PARA" ]]
        then
            echo "Running parallel: $F"
            java  ${HEAP} ${JAR} --config ${F} &
        else
            echo "Running: $F"
            java  ${HEAP} ${JAR} --config ${F}
        fi
    fi
done
wait
