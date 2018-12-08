#!/usr/bin/env bash
bash install.bash

JAR="-jar target/snob.jar"
SAMPLE=100

if [[ "$OSTYPE" == "linux-gnu" ]]; then
    DIRNAME=`date | md5sum | cut -d' ' -f1`
elif [[ "$OSTYPE" == "darwin"* ]]; then
    DIRNAME=`date | md5`
fi

DIR="./results/${DIRNAME}-all"
mkdir -p $DIR

java  ${HEAP} ${JAR} --init

execute() {
    for i in $(seq 1 $SAMPLE); do
        CONFIG=$1
        RESULT="${DIR}/${CONFIG}.txt"
        RESULTTMP="${DIR}/${CONFIG}-${i}-tmp.txt"
        touch "${RESULTTMP}"
        java  ${HEAP} ${JAR} --config "${CONFIG}" > "${RESULTTMP}"
        echo "Reading result from: " $RESULTTMP
        sed -i -e '1,3d' "${RESULTTMP}"
        echo "Writing result into: " $RESULT
        cat "${RESULTTMP}" >> "${RESULT}"
        rm -rf "${RESULTTMP}-e" "${RESULTTMP}"
    done
}

for file in ./configs/generated/p*.conf
do
    if [[ -f $file ]]; then
        execute $(basename "$file") &
    fi
done
echo "Experiment finished."
wait
