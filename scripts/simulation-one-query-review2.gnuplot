set style data linespoints
set datafile separator ","
set terminal png size 1600,1200 font "Default,25"
set style fill solid


original = "../results/55f16ca24b08ef2725fbbc5088942a1e-all/"

outputdir = "../results/review2-a13fa1a657bcaed2b0c17154e6b69b2b-all/"

networksize=1000
viewsize=10
approximation=networksize*log(networksize)/viewsize
set logscale y
set yrange[0.5:10000]
do for [indx in "17 22 54 73 87"] {
    q1l = "q".indx
    print q1l
    a2 = original.q1l."sonfalsetraffictrue-mean.csv"
    a1 = outputdir.q1l."sonfalsetraffictrue-mean.csv"

    set output outputdir.q1l."-review2-simulation-round-traffictrue.png"
    set xlabel 'Number of replicated queries (q) For N=1000'
    set ylabel 'Average number of rounds'
    set style data histogram
    plot a1 using 6:xticlabels(2) lt rgb 'red' title "approximation (A2)", \
        a2 using 7:xticlabels(2) title q1l." Stop (A2)", \
        a2 using 8:xticlabels(2) title q1l." Completed (A2)", \
        a1 using 7:xticlabels(2) title q1l." Stop (A1)", \
        a1 using 8:xticlabels(2) title q1l." Completed (A1)", \
        approximation with linespoints lt rgb 'red' title " approximation (A1)"
}
