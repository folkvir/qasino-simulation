#!/usr/bin/env bash
bash install.bash
HEAP="-Xms50g" # 50go per job

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

execute() {
    for i in $(seq 1 $SAMPLE); do
        CONFIG=$1
        RESULT="${DIR}/${CONFIG}.log"
        RESULTTMP="${DIR}/${CONFIG}-tmp.log"
        java  ${HEAP} ${JAR} --config $CONFIG > $RESULTTMP
        sed -i '1,3d' $RESULTTMP
        cat $RESULTTMP >> $RESULT
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
