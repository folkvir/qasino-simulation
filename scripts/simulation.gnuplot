set style data linespoints
set datafile separator ","
set terminal png size 800,600
set style fill solid

set output "../results/100samples/simulation.png"
set xlabel 'Number of replicated queries for N=100'
set ylabel 'Number of rounds to see all the network'
set style data histogram
plot "../results/100samples/mean.txt" using 20:xticlabels(2) title "Approximation", \
    "../results/100samples/mean.txt" using 16:xticlabels(2) title "Experiment (100 samples)"

set output "../results/100samples/simulation-traffic-messages.png"
set xlabel 'Number of replicated queries for N=100'
set ylabel 'Average number of rounds the first query terminate with'
set style data histogram
plot "../results/100samples/mean.txt" using 18:xticlabels(2) title "Number of Messages"

set output "../results/100samples/simulation-traffic-triples.png"
set xlabel 'Number of replicated queries for N=100'
set ylabel 'Average number of messages sent to neighbours for the first terminated query'
set style data histogram
plot "../results/100samples/mean.txt" using 19:xticlabels(2) title "Number of triples"

set yrange [90:110]
set output "../results/100samples/simulation-completeness.png"
set xlabel 'Number of replicated queries for N=100'
set ylabel 'Average completeness for the first terminated query'
set style data histogram
plot "../results/100samples/mean.txt" using 17:xticlabels(2) notitle
