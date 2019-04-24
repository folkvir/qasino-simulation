#!/usr/bin/env bash

CUR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo $CUR

JAR="-jar target/qasino.jar"
HEAP="-Xms10g -Xmx10g"


if [[ "$OSTYPE" == "linux-gnu" ]]; then
    DIRNAME=`date | md5sum | cut -d' ' -f1`
elif [[ "$OSTYPE" == "darwin"* ]]; then
    DIRNAME=`date | md5`
fi

DIR="$CUR/../results/qasino-spray-both-${DIRNAME}"
mkdir -p $DIR

SAMPLE=1
CONFIG="$CUR/../configs/template-qasino-spray.conf"

execute() {
    replicate=$1
    query=$2
    if [ $query -eq 73 ]; then
        HEAP="-Xms40g -Xmx40g"
    else
        HEAP="-Xms10g -Xmx10g"
    fi
    for size in 1000; do
        for i in $(seq 1 $SAMPLE); do
            ########### WITH TRAFFIC ENABLED
            echo "==QUERY:$query==R:$replicate=SIZE=$size=SAMPLE=$i==================================================================="
            filename=$(basename $CONFIG)
            tmpfile=$(mktemp /tmp/qasino-spray-both.bash.tmpfile.XXXXXX)
            RESULT="${DIR}/${filename}-traffic-enabled-${size}-${i}-q${query}-r${replicate}.csv"
            RESULTTMP="${DIR}/${filename}-traffic-enabled-${i}-q${query}-r${replicate}-tmp.txt"
            cp $CONFIG "$tmpfile"
            perl -pi -e "s/random.seed 1237567890/random.seed $i/g" $tmpfile
            perl -pi -e "s/SIZE 1000/SIZE $size/g" $tmpfile
            perl -pi -e "s/control.observer.querytoreplicate 17/control.observer.querytoreplicate $query/g" $tmpfile
            perl -pi -e "s/control.observer.replicate 50/control.observer.replicate $replicate/g" $tmpfile
            perl -pi -e "s/control.observer.stopcond lasvegas/control.observer.stopcond both/g" $tmpfile
            echo "Replacing values (random.seed $i and SIZE $size) in the config file done."
            touch "${RESULTTMP}"
            cat $tmpfile
            echo "Executing file:" $tmpfile
            java ${HEAP} ${JAR} --execute="$tmpfile" > "${RESULTTMP}"
            echo "$(awk NF $RESULTTMP)" >> "${RESULT}"
            rm -rf "$RESULTTMP" "$tmpfile"
            echo "========================================================================="
            ########### WITH TRAFFIC DISABLED
            echo "==QUERY:$query==R:$replicate=SIZE=$size=SAMPLE=$i==================================================================="
            filename=$(basename $CONFIG)
            tmpfile=$(mktemp /tmp/qasino-spray-both.bash.tmpfile.XXXXXX)
            RESULT="${DIR}/${filename}-traffic-disabled-${size}-${i}-q${query}-r${replicate}.csv"
            RESULTTMP="${DIR}/${filename}-traffic-disabled-${size}-${i}-q${query}-r${replicate}-tmp.txt"
            cp $CONFIG "$tmpfile"
            perl -pi -e "s/random.seed 1237567890/random.seed $i/g" $tmpfile
            perl -pi -e "s/SIZE 1000/SIZE $size/g" $tmpfile
            perl -pi -e "s/control.observer.querytoreplicate 17/control.observer.querytoreplicate $query/g" $tmpfile
            perl -pi -e "s/control.observer.replicate 50/control.observer.replicate $replicate/g" $tmpfile
            perl -pi -e "s/control.observer.stopcond lasvegas/control.observer.stopcond both/g" $tmpfile
            perl -pi -e "s/protocol.qasinospray.traffic true/protocol.qasinospray.traffic false/g" $tmpfile
            echo "Replacing values (random.seed $i and SIZE $size) in the config file done."
            touch "${RESULTTMP}"
            cat $tmpfile
            echo "Executing file:" $tmpfile
            java ${HEAP} ${JAR} --execute="$tmpfile" > "${RESULTTMP}"
            echo "$(awk NF $RESULTTMP)" >> "${RESULT}"
            rm -rf "$RESULTTMP" "$tmpfile"
            echo "========================================================================="
        done
    done
}

for replicate in 500; do # 1 2 5 10 50 100 500 1000; do
    for query in 17; do #22 54 73 87; do
        execute $replicate $query &
    done
done


wait
# clean tmp files
rm -rf /tmp/qasino*

