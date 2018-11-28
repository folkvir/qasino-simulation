set style data linespoints
set datafile separator ","
set terminal png size 800,600
set style fill solid

set xrange [0:100]

set xlabel 'Number of Q'



FILES = system("ls ../savedresults/simulation/*-sort.log")

set output "../savedresults/simulation.png"
set multiplot layout 1, 3

set ylabel 'Rounds to find Q peers (average)'
plot for [d in FILES] d u 5:6 pt 1 notitle

set ylabel 'Q find all peers (approximation baseline)'
plot for [d in FILES] d u 5:7 pt 1 notitle

set ylabel 'Q find all Q (approximation baseline)'
plot for [d in FILES] d u 5:7 pt 1 title system("basename ".d)
unset multiplot
