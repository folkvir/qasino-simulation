set style data linespoints
set datafile separator ","
set terminal png size 1600,1200
set style fill solid

outputdir = "../results/1164f01243fa507cb435a422bdeb2a95/"

global = outputdir."global-mean.csv"

q1l = "q0"

q1 = outputdir.q1l."sontruetraffictrue-mean.csv"
q1f = outputdir.q1l."sontruetrafficfalse-mean.csv"
q1ft = outputdir.q1l."sonfalsetraffictrue-mean.csv"
q1ff = outputdir.q1l."sonfalsetrafficfalse-mean.csv"


set output outputdir."ratio.png"
set xlabel 'Number of replicated queries. For N=1000 (RPS+CG) traffic minimized'
set ylabel 'Ratio between approximation and experiment'
set style data histogram
plot q1 using 15:xticlabels(2) title "w/ clique", \
    q1ft using 15:xticlabels(2) title "wo/ clique"

set yrange [0:50]
set output outputdir."simulation-traffictrue.png"
set xlabel 'Number of replicated queries. For N=1000 (RPS+CG) traffic minimized'
set ylabel 'Average number of rounds for one Q to see all the network'
set style data histogram
plot q1 using 14:xticlabels(16) lt rgb 'red' title "approximation", \
    q1 using 7:xticlabels(2) title q1l."w/ clique", \
    q1ft using 7:xticlabels(2) title q1l." wo/ clique"

#set output outputdir."simulation-trafficfalse.png"
#set xlabel 'Number of replicated queries. For N=1000 (only RPS) with traffic normal'
#set ylabel 'Average number of rounds for one Q to see all the network'
#set style data histogram
#plot q1f using 14:xticlabels(16) lt rgb 'red' title "approximation", \
#    q1f using 7:xticlabels(2) title q1l."w/ clique", \
#    q1ff using 7:xticlabels(2) title q1l."wo/ clique"

