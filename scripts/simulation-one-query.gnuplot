set style data linespoints
set datafile separator ","
set terminal png size 1600,1200 font "Default,30"
set style fill solid


outputdir = "../results/4c4c4d0c2754ff27876d83469e788e99-all/"

set logscale y
do for [indx in "17 22 54 73 87"] {
    q1l = "q".indx
    print q1l
    q1 = outputdir.q1l."sontruetraffictrue-mean.csv"
    q1f = outputdir.q1l."sontruetrafficfalse-mean.csv"
    q1ft = outputdir.q1l."sonfalsetraffictrue-mean.csv"
    q1ff = outputdir.q1l."sonfalsetrafficfalse-mean.csv"

    set output outputdir.q1l."-simulation-round-traffictrue.png"
    set xlabel 'Number of replicated queries (q) For N=1000'
    set ylabel 'Average number of rounds'
    set style data histogram
    plot q1 using 6:xticlabels(2) lt rgb 'red' title "approximation", \
        q1 using 7:xticlabels(2) title q1l." w/ clique", \
        q1 using 8:xticlabels(2) title q1l." w/ clique completed", \
        q1ft using 7:xticlabels(2) title q1l." w/o clique", \
        q1ft using 8:xticlabels(2) title q1l." w/o clique completed"

    set key left top
    set output outputdir.q1l."-simulation-traffic-triples.png"
    set xlabel 'Number of replicated queries (q) For N=1000'
    set ylabel 'Average number of triples received'
    set style data histogram
    plot q1f using 10:xticlabels(2) title q1l." w/ clique normal", \
        q1 using 10:xticlabels(2) title q1l." w/ clique minimized", \
        q1ff using 10:xticlabels(2) title q1l." w/o clique normal", \
        q1ft using 10:xticlabels(2) title q1l." w/o clique minimized"

    set output outputdir.q1l."-simulation-traffic-messages.png"
    set xlabel 'Number of replicated queries (q) For N=1000'
    set ylabel 'Average number of messages sent'
    set style data histogram
    plot q1f using 11:xticlabels(2) title q1l." w/ clique normal", \
        q1 using 11:xticlabels(2) title q1l." w/ clique minimized", \
        q1ff using 11:xticlabels(2) title q1l." w/o clique normal", \
        q1ft using 11:xticlabels(2) title q1l." w/o clique minimized"
}
