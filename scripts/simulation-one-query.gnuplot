set style data linespoints
set datafile separator ","
set terminal png size 1600,1200
set style fill solid

outputdir = "../results/all/"

global = outputdir."global-mean.csv"

q1l = "q63"

q1 = outputdir.q1l."sontruetraffictrue-mean.csv"
q1f = outputdir.q1l."sontruetrafficfalse-mean.csv"
q1ft = outputdir.q1l."sonfalsetraffictrue-mean.csv"
q1ff = outputdir.q1l."sonfalsetrafficfalse-mean.csv"

set logscale y
set output outputdir."ratio.png"
set xlabel 'Number of replicated queries. For N=1000 (RPS+CG) traffic minimized'
set ylabel 'Ratio between approximation and experiment for one query'
set style data histogram
plot q1 using 15:xticlabels(2) title "w/ clique", \
    q1ft using 15:xticlabels(2) title "wo/ clique"

set output outputdir."simulation-round-traffictrue.png"
set xlabel 'Number of replicated queries.'
set ylabel 'Average number of rounds for one query to see all the network'
set style data histogram
plot q1 using 14:xticlabels(16) lt rgb 'red' title "approximation", \
    q1 using 7:xticlabels(2) title q1l." w/ clique", \
    q1 using 9:xticlabels(2) title q1l." w/ clique completed", \
    q1ft using 7:xticlabels(2) title q1l." wo/ clique", \
    q1ft using 9:xticlabels(2) title q1l." wo/ clique completed"

set output outputdir."simulation-traffic-triples.png"
set xlabel 'Number of replicated queries.'
set ylabel 'Average number of triples received'
set style data histogram
plot q1f using 13:xticlabels(2) title q1l." w/ clique normal", \
    q1 using 13:xticlabels(2) title q1l." w/ clique minimized", \
    q1ff using 13:xticlabels(2) title q1l." wo/ clique normal", \
    q1ft using 13:xticlabels(2) title q1l." wo/ clique minimized"

set output outputdir."simulation-traffic-messages.png"
set xlabel 'Number of replicated queries. For N=1000 (RPS+CG) traffic minimized'
set ylabel 'Average number of messages sent'
set style data histogram
plot q1f using 10:xticlabels(2) title q1l." w/ clique normal", \
    q1 using 10:xticlabels(2) title q1l." w/ clique minimized", \
    q1ff using 10:xticlabels(2) title q1l." wo/ clique normal", \
    q1ft using 10:xticlabels(2) title q1l." wo/ clique minimized"
