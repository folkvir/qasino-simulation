set style data linespoints
set datafile separator ","
set terminal png size 1600,1200
set style fill solid

outputdir = "../results/1-all/"

global = outputdir."global-mean.csv"

q1l = "q17"
q2l = "q22"
q3l = "q54"
q4l = "q73"
q5l = "q87"

q1 = outputdir.q1l."sontruetraffictrue-mean.csv"
q2 = outputdir.q2l."sontruetraffictrue-mean.csv"
q3 = outputdir.q3l."sontruetraffictrue-mean.csv"
q4 = outputdir.q4l."sontruetraffictrue-mean.csv"
q5 = outputdir.q5l."sontruetraffictrue-mean.csv"

q1f = outputdir.q1l."sontruetrafficfalse-mean.csv"
q2f = outputdir.q2l."sontruetrafficfalse-mean.csv"
q3f = outputdir.q3l."sontruetrafficfalse-mean.csv"
q4f = outputdir.q4l."sontruetrafficfalse-mean.csv"
q5f = outputdir.q5l."sontruetrafficfalse-mean.csv"

q1ft = outputdir.q1l."sonfalsetraffictrue-mean.csv"
q2ft = outputdir.q2l."sonfalsetraffictrue-mean.csv"
q3ft = outputdir.q3l."sonfalsetraffictrue-mean.csv"
q4ft = outputdir.q4l."sonfalsetraffictrue-mean.csv"
q5ft = outputdir.q5l."sonfalsetraffictrue-mean.csv"

q1ff = outputdir.q1l."sonfalsetrafficfalse-mean.csv"
q2ff = outputdir.q2l."sonfalsetrafficfalse-mean.csv"
q3ff = outputdir.q3l."sonfalsetrafficfalse-mean.csv"
q4ff = outputdir.q4l."sonfalsetrafficfalse-mean.csv"
q5ff = outputdir.q5l."sonfalsetrafficfalse-mean.csv"

set logscale y
set output outputdir."ratio.png"
set xlabel 'Number of replicated queries. For N=1000 (RPS+CG) traffic minimized'
set ylabel 'Ratio between approximation and experiment'
set style data histogram
plot q1 using 15:xticlabels(2) lt rgb 'red' title "ratio"


set output outputdir."simulation-w-clique-traffictrue.png"
set xlabel 'Number of replicated queries. For N=1000 (RPS+CG) traffic minimized'
set ylabel 'Average number of rounds for one Q to see all the network'
set style data histogram
plot q1 using 14:xticlabels(16) lt rgb 'red' title "approximation", \
    q1 using 7:xticlabels(2) title q1l, \
    q2 using 7:xticlabels(2) title q2l, \
    q3 using 7:xticlabels(2) title q3l, \
    q4 using 7:xticlabels(2) title q4l, \
    q5 using 7:xticlabels(2) title q5l

set output outputdir."simulation-wo-clique-traffictrue.png"
set xlabel 'Number of replicated queries. For N=1000 (only RPS) with traffic minimized'
set ylabel 'Average number of rounds for one Q to see all the network'
set style data histogram
plot q1ft using 14:xticlabels(16) lt rgb 'red' title "approximation", \
    q1ft using 7:xticlabels(2) title q1l, \
    q2ft using 7:xticlabels(2) title q2l, \
    q3ft using 7:xticlabels(2) title q3l, \
    q4ft using 7:xticlabels(2) title q4l, \
    q5ft using 7:xticlabels(2) title q5l

set output outputdir."simulation-w-clique-trafficfalse.png"
set xlabel 'Number of replicated queries. For N=1000 (RPS+CG) with traffic normal'
set ylabel 'Average number of rounds for one Q to see all the network'
set style data histogram
plot q1f using 14:xticlabels(16) lt rgb 'red' title "approximation", \
    q1f using 7:xticlabels(2) title q1l, \
    q2f using 7:xticlabels(2) title q2l, \
    q3f using 7:xticlabels(2) title q3l, \
    q4f using 7:xticlabels(2) title q4l, \
    q5f using 7:xticlabels(2) title q5l

set output outputdir."simulation-wo-clique-trafficfalse.png"
set xlabel 'Number of replicated queries. For N=1000 (only RPS) with traffic normal'
set ylabel 'Average number of rounds for one Q to see all the network'
set style data histogram
plot q1ff using 14:xticlabels(16) lt rgb 'red' title "approximation", \
    q1ff using 7:xticlabels(2) title q1l, \
    q2ff using 7:xticlabels(2) title q2l, \
    q3ff using 7:xticlabels(2) title q3l, \
    q4ff using 7:xticlabels(2) title q4l, \
    q5ff using 7:xticlabels(2) title q5l

set output outputdir."simulation-traffic-messages-w-clique-traffictrue.png"
set xlabel 'Number of replicated queries for N=1000 (RPS+CG)'
set ylabel 'Average number of rounds the first query terminate with'
set style data histogram
plot q1 using 11:xticlabels(2) title q1l, \
    q2 using 11:xticlabels(2) title q2l, \
    q3 using 11:xticlabels(2) title q3l, \
    q4 using 11:xticlabels(2) title q4l, \
    q5 using 11:xticlabels(2) title q5l

set output outputdir."simulation-traffic-messages-wo-clique-traffictrue.png"
set xlabel 'Number of replicated queries for N=1000 (RPS)'
set ylabel 'Average number of rounds the first query terminate with'
set style data histogram
plot q1ft using 11:xticlabels(2) title q1l, \
    q2ft using 11:xticlabels(2) title q2l, \
    q3ft using 11:xticlabels(2) title q3l, \
    q4ft using 11:xticlabels(2) title q4l, \
    q5ft using 11:xticlabels(2) title q5l

set output outputdir."simulation-traffic-messages-w-clique-trafficfalse.png"
set xlabel 'Number of replicated queries for N=1000 (RPS+CG)'
set ylabel 'Average number of rounds the first query terminate with'
set style data histogram
plot q1f using 11:xticlabels(2) title q1l, \
    q2f using 11:xticlabels(2) title q2l, \
    q3f using 11:xticlabels(2) title q3l, \
    q4f using 11:xticlabels(2) title q4l, \
    q5f using 11:xticlabels(2) title q5l

set output outputdir."simulation-traffic-messages-wo-clique-trafficfalse.png"
set xlabel 'Number of replicated queries for N=1000 (RPS)'
set ylabel 'Average number of rounds the first query terminate with'
set style data histogram
plot q1ff using 11:xticlabels(2) title q1l, \
    q2ff using 11:xticlabels(2) title q2l, \
    q3ff using 11:xticlabels(2) title q3l, \
    q4ff using 11:xticlabels(2) title q4l, \
    q5ff using 11:xticlabels(2) title q5l

set output outputdir."simulation-traffic-triples-w-clique-traffictrue.png"
set xlabel 'Number of replicated queries for N=1000 (RPS+CG)'
set ylabel 'Average number of triples received from neighbours.'
set style data histogram
plot q1 using 13:xticlabels(2) title q1l, \
    q2 using 13:xticlabels(2) title q2l, \
    q3 using 13:xticlabels(2) title q3l, \
    q4 using 13:xticlabels(2) title q4l, \
    q5 using 13:xticlabels(2) title q5l

set output outputdir."simulation-traffic-triples-wo-clique-traffictrue.png"
set xlabel 'Number of replicated queries for N=1000 (only RPS)'
set ylabel 'Average number of triples received from neighbours.'
set style data histogram
plot q1ft using 13:xticlabels(2) title q1l, \
    q2ft using 13:xticlabels(2) title q2l, \
    q3ft using 13:xticlabels(2) title q3l, \
    q4ft using 13:xticlabels(2) title q4l, \
    q5ft using 13:xticlabels(2) title q5l

set output outputdir."simulation-traffic-triples-w-clique-trafficfalse.png"
set xlabel 'Number of replicated queries for N=1000 (RPS+CG)'
set ylabel 'Average number of triples received from neighbours.'
set style data histogram
plot q1f using 13:xticlabels(2) title q1l, \
    q2f using 13:xticlabels(2) title q2l, \
    q3f using 13:xticlabels(2) title q3l, \
    q4f using 13:xticlabels(2) title q4l, \
    q5f using 13:xticlabels(2) title q5l

set output outputdir."simulation-traffic-triples-wo-clique-trafficfalse.png"
set xlabel 'Number of replicated queries for N=1000 (only RPS)'
set ylabel 'Average number of triples received from neighbours.'
set style data histogram
plot q1ff using 13:xticlabels(2) title q1l, \
    q2ff using 13:xticlabels(2) title q2l, \
    q3ff using 13:xticlabels(2) title q3l, \
    q4ff using 13:xticlabels(2) title q4l, \
    q5ff using 13:xticlabels(2) title q5l


