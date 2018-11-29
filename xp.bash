#!/usr/bin/env bash
bash install.bash
PARALLEL=$1
PARA="--parallel"
LOG="xp.log"
HEAP="-Xms50000m" # 50go per job
JAR="-jar target/snob.jar"

rm -rf ./results/*.txt

java  ${HEAP} ${JAR} --init

for file in ./configs/generated/p*.conf
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

DIRNAME=`date | md5`
mkdir -p ./savedresults/$DIRNAME
mv ./results/*.txt ./savedresults/$DIRNAME

rm -rf ./results/*.txt
echo "Experiment finished."

wait
