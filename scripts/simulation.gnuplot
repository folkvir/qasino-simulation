set style data linespoints
set datafile separator ","
set terminal png size 800,600
set style fill solid

set xrange [0:100]


FILES = system("ls ../savedresults/simulation/sim*-sort-c5.log")

set output "../savedresults/simulation.png"
#set multiplot layout 3, 1

set xlabel 'Average number of rounds to find all Q'
set ylabel 'Number of similar queries'
plot for [d in FILES] d u 6:5 pt 1 notitle

set output "../savedresults/simulation-baseline-q-all.png"
set ylabel 'Q find all peers (approximation baseline)'
plot for [d in FILES] d u 7:5 pt 1 notitle

set output "../savedresults/simulation-baseline-q-all-q.png"
set ylabel 'Q find all Q (approximation baseline)'
plot for [d in FILES] d u 8:5 pt 1 title system("basename ".d)
#unset multiplot
