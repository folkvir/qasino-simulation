#!/usr/bin/env bash
#bash install.bash

CUR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo $CUR

JAR="-jar target/snob.jar"
HEAP="-Xms5g -Xmx5g"


if [[ "$OSTYPE" == "linux-gnu" ]]; then
    DIRNAME=`date | md5sum | cut -d' ' -f1`
elif [[ "$OSTYPE" == "darwin"* ]]; then
    DIRNAME=`date | md5`
fi

DIR="$CUR/../results/spray-${DIRNAME}"
mkdir -p $DIR

SAMPLE=1000
for i in $(seq 1 $SAMPLE); do
    for file in $(ls $CUR/../configs/sprays/spray-*.conf); do
        echo "Executing file: "$file
        filename=$(basename $file)
        rm -rf /tmp/spray.bash.tmpfile
        tmpfile=$(mktemp /tmp/spray.bash.tmpfile)
        RESULT="${DIR}/${filename}-${i}.csv"
        RESULTTMP="${DIR}/${filename}-${i}-tmp.txt"
        cat $file | perl -pe "s/random.seed 1237567890/random.seed $i/g" > "$tmpfile"
        cat $tmpfile
        touch "${RESULTTMP}"
        java ${HEAP} ${JAR} --execute="${tmpfile}" > "${RESULTTMP}"
        echo "$(awk NF $RESULTTMP)" >> "${RESULT}"
        rm -rf "$RESULTTMP"
    done
done
