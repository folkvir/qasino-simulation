#!/usr/bin/env bash
bash install.bash

CUR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo $CUR

JAR="-jar target/snob.jar"
HEAP="-Xms200g -Xmx200g"


if [[ "$OSTYPE" == "linux-gnu" ]]; then
    DIRNAME=`date | md5sum | cut -d' ' -f1`
elif [[ "$OSTYPE" == "darwin"* ]]; then
    DIRNAME=`date | md5`
fi

DIR="$CUR/../results/spray-${DIRNAME}"
mkdir -p $DIR

for file in $(ls $CUR/../configs/sprays/spray-{100,1k,10k}.conf); do
    echo "Executing file: "$file
    filename=$(basename $file)
    RESULT="${DIR}/${filename}.txt"
    RESULTTMP="${DIR}/${filename}-tmp.txt"
    touch "${RESULTTMP}"
    java ${HEAP} ${JAR} --execute="${file}" > "${RESULTTMP}"
    echo "$(awk NF $RESULTTMP)" >> "${RESULT}"
    rm -rf "$RESULTTMP"
done
