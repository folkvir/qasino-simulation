set style data linespoints
set datafile separator ","
set terminal png size 800,600
set style fill solid

FILES = system("ls ")

set output "../results/100samples2/simulation.png"

set xlabel 'Number of replicated queries'
set ylabel 'Number of rounds to see all the network'
plot "../results/100samples2/mean.txt" u 2:16 pt 1 title "Experiment", \
     "../results/100samples2/mean.txt" u 2:20 pt 1 title "Approximation"

set output "../results/100samples2/ratio.png"
set xlabel 'Number of replicated queries'
set ylabel 'Ratio'
plot "../results/100samples2/mean.txt" u 2:21 pt 1 notitle
