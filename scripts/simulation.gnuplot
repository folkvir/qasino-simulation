set style data linespoints
set datafile separator ","
set terminal png size 800,600
set style fill solid

FILES = system("ls ../savedresults/simulation/sim*-sort-c5.log")

set output "../savedresults/simulation.png"

set xlabel 'Number of Q'
set ylabel 'Number of rounds for Q to find Q'
plot for [d in FILES] d u 5:6 pt 1 notitle

set output "../savedresults/simulation-baseline-q-all.png"
set xlabel 'Number of Q'
set ylabel 'Number of rounds to find for to Q to find all (approximation)'
plot for [d in FILES] d u 5:7 pt 1 notitle

set output "../savedresults/simulation-baseline-q-all-q.png"
set xlabel 'Number of Q'
set ylabel 'Ratio'
plot for [d in FILES] d u 5:8 pt 1 title system("basename ".d)
