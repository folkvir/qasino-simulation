#!/usr/bin/env bash
bash install.bash

JAR="-jar target/snob.jar"
SAMPLE=100

if [[ "$OSTYPE" == "linux-gnu" ]]; then
    DIRNAME=`date | md5sum | cut -d' ' -f1`
elif [[ "$OSTYPE" == "darwin"* ]]; then
    DIRNAME=`date | md5`
fi

DIR="../results/${DIRNAME}variancenq"
mkdir -p $DIR

java -cp ../target/classes/ snob.simulation.VarClique > "${DIR}/varclique.csv" &
java -cp ../target/classes/ snob.simulation.VarNoclique > "${DIR}/varnoclique.csv" &
wait