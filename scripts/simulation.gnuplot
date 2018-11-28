set style data linespoints
set datafile separator ","
set terminal png size 800,600
set style fill solid

set xrange [0:100]

set ylabel 'Average number of round to find Q peers'
set xlabel 'Number of Q'



FILES = system("ls ../savedresults/simulation/*-sort.log")

set output "../savedresults/simulation.png"
plot for [d in FILES] '< sort -nk1 $d' u 1:6 pt 1 title system("basename ".d)
