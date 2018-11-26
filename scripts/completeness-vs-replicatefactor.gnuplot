set style data linespoints
set datafile separator ","
set terminal png size 800,600

#set yrange [0:100]
#set xrange [0:50]

set ylabel 'Completeness'
set xlabel 'Round'

FILES = system("ls ../savedresults/run3/p200-sonfalse-*.txt")
set output "../savedresults/comp-repfactor-sonfalse.png"
plot for [d in FILES] d u 1:6 pt 1 title system("basename ".d)

FILES = system("ls ../savedresults/run3/p200-sontrue-*.txt")
set output "../savedresults/comp-repfactor-sontrue.png"
plot for [d in FILES] d u 1:6 pt 1 title system("basename ".d)
