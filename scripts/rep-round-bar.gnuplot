set style data boxes
set datafile separator ","
set terminal png size 800,600
set boxwidth 5
set style fill solid

set xrange [0:100]

set ylabel 'Number of round to finish'
set xlabel 'Replication factor (%)'

FILES = system("ls ../savedresults/r1/p200-sonfalse-*.txt")
set output "../savedresults/rep-round-sonfalse.png"
plot for [d in FILES] d u 3:1 title system("basename ".d)

FILES = system("ls ../savedresults/r1/p200-sontrue-*.txt")
set output "../savedresults/rep-round-sontrue.png"
plot for [d in FILES] d u 3:1 title system("basename ".d)
