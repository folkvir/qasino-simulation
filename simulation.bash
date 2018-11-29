#!/usr/bin/env bash

SIZE=1000
SAMPLE=1000
K=10
SON=0

mkdir -p ./savedresults/simulation

run() {
    K=$3
    SIZE=$2
    SON=$1
    OUTPUT="./savedresults/simulation/sim-k-${K}-son-${SON}-size-${SIZE}-sample${SAMPLE}.log"
    java -cp target/classes/ snob.simulation.Simulation $SIZE $SAMPLE $K $SON > $OUTPUT
    sort -nk5 $OUTPUT > "${OUTPUT}-sort-c5.log"
    sort -nk1 $OUTPUT > "${OUTPUT}-sort-c1.log"
}

run 0 1000 1

SONS=( 0 1 2 3 4 5 6 7 8 9 )

for s in "${SONS[@]}"; do
    run $s 1000 $K &
done

SIZES=( 10 100 500 1000 2000 3000 4000 5000 10000 100000 )

for s in "${SIZES[@]}"; do
    run 0 $s $K &
done