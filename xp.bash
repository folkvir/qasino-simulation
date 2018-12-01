#!/usr/bin/env bash
bash install.bash
HEAP="-Xms5000m" # 50go per job

JAR="-jar target/snob.jar"
SAMPLE=100
DIRNAME=`date | md5`
DIR="./results/${DIRNAME}"
mkdir -p $DIR

java  ${HEAP} ${JAR} --init

execute() {
    for i in $(seq 1 $SAMPLE); do
        CONFIG=$1
        RESULT="${DIR}/${CONFIG}.log"
        RESULTTMP="${DIR}/${CONFIG}-tmp.log"
        java  ${HEAP} ${JAR} --config $CONFIG > $RESULTTMP
        sed '1,3d' $RESULTTMP >> $RESULT
    done
}

for file in ./configs/generated/p*.conf
do
    if [[ -f $file ]]; then
        execute $(basename "$file")
    fi
done

echo "Experiment finished."
wait
