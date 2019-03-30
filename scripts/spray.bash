#!/usr/bin/env bash
bash install.bash

CUR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo $CUR

JAR="-jar target/snob.jar"
HEAP="-Xms50g -Xmx50g"


if [[ "$OSTYPE" == "linux-gnu" ]]; then
    DIRNAME=`date | md5sum | cut -d' ' -f1`
elif [[ "$OSTYPE" == "darwin"* ]]; then
    DIRNAME=`date | md5`
fi

DIR="$CUR/../results/spray-${DIRNAME}"
mkdir -p $DIR

START=100
END=$((1000 + $START))
STEP=100
SAMPLE=500

CONFIG="$CUR/../configs/spray.conf"
for size in 10 20 30 40 50 60 70 80 90 100 200 400 600 800 1000 2000; do
    for i in $(seq 1 $SAMPLE); do
        echo "=====SIZE=$size=SAMPLE=$i==================================================================="
        filename=$(basename $CONFIG)
        tmpfile=$(mktemp /tmp/spray.bash.tmpfile.XXXXXX)
        RESULT="${DIR}/${filename}-${size}-${i}.csv"
        RESULTTMP="${DIR}/${filename}-${i}-tmp.txt"
        cp $CONFIG "$tmpfile"
        perl -pi -e "s/random.seed 1237567890/random.seed $i/g" $tmpfile
        perl -pi -e "s/SIZE 100/SIZE $size/g" $tmpfile
        echo "Replacing values (random.seed $i and SIZE $size) in the config file done."
        touch "${RESULTTMP}"
        echo "Executing file:" $tmpfile
        java ${HEAP} ${JAR} --execute="$tmpfile" > "${RESULTTMP}"
        echo "$(awk NF $RESULTTMP)" >> "${RESULT}"
        rm -rf "$RESULTTMP" "$tmpfile"
        echo "========================================================================="
    done
done

# clean tmp files
rm -rf /tmp/spray.bash.*

