FILES = system("ls ../results/p200-sonfalse-*.txt")

set datafile separator ","
set xlabel 'Round'                              # x-axis label
set ylabel 'Average query completeness (% of completed queries)'                          # y-axis label
set terminal png size 800,600
set output "./savedresults/completeness-v2.png"


set multiplot layout 1,3
set style data linespoints
set yrange [0:100]
# set xrange [0:200]
plot for [data in FILES] data u 1:6 w p pt 1 lt rgb 'black' notitle
unset yrange
unset multiplot