#!/usr/bin/env bash

SIZE=1000
SAMPLE=100
K=6
SON=0

mkdir -p ./savedresults/simulation

run() {
    SON=$1
    OUTPUT="./savedresults/simulation/sim-k-${K}-son-${SON}-size-${SIZE}-sample${SAMPLE}.log"
    java -cp target/classes/ snob.simulation.Simulation $SIZE $SAMPLE $K $SON > $OUTPUT
    sort -nk5 $OUTPUT > "${OUTPUT}-sort-c5.log"
    sort -nk1 $OUTPUT > "${OUTPUT}-sort-c1.log"
}

run 0 &
run 1 &
run 2 &
run 3 &
run 4 &
run 5 &
wait