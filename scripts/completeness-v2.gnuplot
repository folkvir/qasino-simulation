set datafile separator ","
set xlabel 'Round'                              # x-axis label

set terminal png size 800,600
set style data linespoints
set yrange [0:100]
# set xrange [0:200]


FILES = system("ls ../savedresults/run1/p200-sonfalse-*.txt")
set output "../savedresults/completeness-v2-sonfalse.png"
set ylabel 'Average query completeness (% of completed queries) (RPS only)'                          # y-axis label
plot for [data in FILES] data u 1:6 w p pt 1

FILES = system("ls ../savedresults/run1/p200-sontrue-*.txt")
set ylabel 'Average query completeness (% of completed queries), SON+RPS'                          # y-axis label
set output "../savedresults/completeness-v2-sontrue.png"
plot for [data in FILES] data u 1:6 w p pt 1

unset yrange
FILES = system("ls ../savedresults/run1/p200-sonfalse-*.txt")
set output "../savedresults/traffic-v2-sonfalse.png"
set ylabel 'Traffic in triples received from neighbours, RPS only'                          # y-axis label
plot for [data in FILES] data u 1:11 w p pt 1

FILES = system("ls ../savedresults/run1/p200-sontrue-*.txt")
set ylabel 'Traffic in triples received from neighbours, SON+RPS'                          # y-axis label
set output "../savedresults/traffic-v2-sontrue.png"
plot for [data in FILES] data u 1:11 w p pt 1