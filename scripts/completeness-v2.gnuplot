set style data linespoints
set datafile separator ","
set xlabel 'Round'                              # x-axis label
set terminal png size 800,600

set yrange [0:100]
# set xrange [0:200]


FILES = system("ls ../savedresults/run2/traffictrue/p200-sonfalse-*.txt")
set output "../savedresults/completeness-v2-sonfalse.png"
set ylabel 'Average query completeness (% of completed queries) (RPS only)'                          # y-axis label
plot for [d in FILES] d u 1:6 pt 1 title system("basename ".d)

FILES = system("ls ../savedresults/run2/traffictrue/p200-sontrue-*.txt")
set ylabel 'Average query completeness (% of completed queries), SON+RPS'                          # y-axis label
set output "../savedresults/completeness-v2-sontrue.png"
plot for [d in FILES] d u 1:6 pt 1 title system("basename ".d)

unset yrange
FILES = system("ls ../savedresults/run2/traffictrue/p200-sonfalse-*.txt")
set output "../savedresults/traffic-v2-sonfalse.png"
set ylabel 'Traffic in triples received from neighbours, RPS only'                          # y-axis label
plot for [d in FILES] d u 1:11 pt 1 title system("basename ".d)

FILES = system("ls ../savedresults/run2/traffictrue/p200-sontrue-*.txt")
set ylabel 'Traffic in triples received from neighbours, SON+RPS'                          # y-axis label
set output "../savedresults/traffic-v2-sontrue.png"
plot for [d in FILES] d u 1:11 pt 1 title system("basename ".d)