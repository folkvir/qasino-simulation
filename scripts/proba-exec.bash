#!/usr/bin/env bash
#bash install.bash

CUR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo $CUR

JAR="-jar target/snob.jar"
HEAP="-Xms5g -Xmx5g"
SAMPLE=1000

if [[ "$OSTYPE" == "linux-gnu" ]]; then
    DIRNAME=`date | md5sum | cut -d' ' -f1`
elif [[ "$OSTYPE" == "darwin"* ]]; then
    DIRNAME=`date | md5`
fi

DIR="$CUR/../results/${DIRNAME}-all"
mkdir -p $DIR

CONF="stop-proba.conf"
CONFIG="/../configs/generated/$CONF"
echo $CONFIG
cat $CONFIG
for i in $(seq 1 $SAMPLE); do
    RESULT="${DIR}/${CONF}.txt"
    RESULTTMP="${DIR}/${CONF}-${i}-tmp.txt"
    touch "${RESULTTMP}"
    java  ${HEAP} ${JAR} --execute="${CONF}" > "${RESULTTMP}"
    echo "$i, $(awk NF $RESULTTMP)" >> "${RESULT}"
done
