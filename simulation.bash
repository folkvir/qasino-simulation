#!/usr/bin/env bash

SIZE=1000
SAMPLE=10
K=6
SON=0

mkdir -p ./results/simulation

java -cp target/classes/ snob.simulation.Simulation $SIZE $SAMPLE $K $SON > "./results/simulation/simk-${K}-son-${SON}-size-${SIZE}-sample${SAMPLE}.log"

SON=1
java -cp target/classes/ snob.simulation.Simulation $SIZE $SAMPLE $K $SON > "./results/simulation/simk-${K}-son-${SON}-ize-${SIZE}-sample${SAMPLE}.log"

SON=2
java -cp target/classes/ snob.simulation.Simulation $SIZE $SAMPLE $K $SON > "./results/simulation/simk-${K}-son-${SON}-size-${SIZE}-sample${SAMPLE}.log"

SON=3
java -cp target/classes/ snob.simulation.Simulation $SIZE $SAMPLE $K $SON > "./results/simulation/simk-${K}-son-${SON}-size-${SIZE}-sample${SAMPLE}.log"

SON=4
java -cp target/classes/ snob.simulation.Simulation $SIZE $SAMPLE $K $SON > "./results/simulation/simk-${K}-son-${SON}-size-${SIZE}-sample${SAMPLE}.log"

SON=5
java -cp target/classes/ snob.simulation.Simulation $SIZE $SAMPLE $K $SON > "./results/simulation/sim-k-${K}-son-${SON}-size-${SIZE}-sample${SAMPLE}.log"

wait