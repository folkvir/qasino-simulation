#!/usr/bin/env bash

SIZE=1000
SAMPLE=100
K=6
SON=0

mkdir -p ./results/simulation

run() {
    SON=$1
    OUTPUT="./results/simulation/sim-k-${K}-son-${SON}-size-${SIZE}-sample${SAMPLE}.log"
    java -cp target/classes/ snob.simulation.Simulation $SIZE $SAMPLE $K $SON > $OUTPUT
    sort -nk1 $OUTPUT > "${OUTPUT}-sort.log"
}

run 1
run 2
run 3
run 4
run 5

wait