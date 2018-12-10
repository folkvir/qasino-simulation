set style data linespoints
set datafile separator ","
set terminal png size 1600,1200
set style fill solid

outputdir = "../results/40b8ed954b5af1c60443a2cd8de8607f-all/"

global = outputdir."global-mean.csv"

q1l = "q17"

set logscale y

q1 = outputdir.q1l."sontruetraffictrue-mean.csv"
q1f = outputdir.q1l."sontruetrafficfalse-mean.csv"
q1ft = outputdir.q1l."sonfalsetraffictrue-mean.csv"
q1ff = outputdir.q1l."sonfalsetrafficfalse-mean.csv"

set output outputdir."simulation-round-traffictrue.png"
set xlabel 'Number of replicated queries (q)'
set ylabel 'Average number of rounds for one query to see all the network'
set style data histogram
plot q1 using ($1*(log($1-$2)+0.557)/($2)):xticlabels(2) lt rgb 'red' title "approximation", \
    q1 using 19:xticlabels(2) title q1l." w/ clique", \
    q1 using 9:xticlabels(2) title q1l." w/ clique completed", \
    q1ft using 19:xticlabels(2) title q1l." wo/ clique", \
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
plot q1f using 11:xticlabels(2) title q1l." w/ clique normal", \
    q1 using 11:xticlabels(2) title q1l." w/ clique minimized", \
    q1ff using 11:xticlabels(2) title q1l." wo/ clique normal", \
    q1ft using 11:xticlabels(2) title q1l." wo/ clique minimized"
