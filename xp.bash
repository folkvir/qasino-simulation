#!/usr/bin/env bash
bash install.bash
HEAP="-Xms20g" # 50go per job

JAR="-jar target/snob.jar"
SAMPLE=100

if [[ "$OSTYPE" == "linux-gnu" ]]; then
    DIRNAME=`date | md5sum`
elif [[ "$OSTYPE" == "darwin"* ]]; then
    DIRNAME=`date | md5`
fi

DIR="./results/${DIRNAME}"
mkdir -p $DIR

java  ${HEAP} ${JAR} --init

executions=()
executionsResults=()

execute() {
    for i in $(seq 1 $SAMPLE); do
        CONFIG=$1
        RESULT="${DIR}/${CONFIG}"
        RESULTTMP="${DIR}/${CONFIG}-${i}-tmp.log"
        touch "${RESULTTMP}"
        java  ${HEAP} ${JAR} --config "${CONFIG}" > "${RESULTTMP}" &
        executions[$i]=$!
        executionsResults[$i]="${RESULT}"
    done
}

for file in ./configs/generated/p*.conf
do
    if [[ -f $file ]]; then
        execute $(basename "$file")
    fi
done

i=$(( 1 ))
for pid in ${executions[*]}; do
    wait $pid
    echo "Reading file number ${i}"
    RESULT="${executionsResults[$i]}"
    RESULTTMP="${RESULT}-${i}-tmp.log"
    echo "Reading result from: " $RESULTTMP
    sed -i -e '1,3d' "${RESULTTMP}"
    echo "Writing result into: " $RESULT
    cat "${RESULTTMP}" >> "${RESULT}-result.txt"
    rm -rf "${RESULTTMP}-e" "${RESULTTMP}"
    i=$(( i+1 ))
done

echo "Experiment finished."
wait
