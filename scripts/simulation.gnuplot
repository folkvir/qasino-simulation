set style data linespoints
set datafile separator ","
set terminal png size 1600,1200
set style fill solid

outputdir = "../results/n=1000-100samples/"

rep1 = outputdir."rep1sontruetraffictrue-mean.txt"
rep50 = outputdir."rep50sontruetraffictrue-mean.txt"
rep100 = outputdir."rep100sontruetraffictrue-mean.txt"

set output outputdir."simulation.png"
set xlabel 'Different conjunctive queries'
set ylabel 'Average number of rounds for one Q to see all the network'
set style data histogram

plot rep1 using 14:xticlabels(16) lt rgb 'red' title "q=1 (approximation)", \
    rep1 using 7:xticlabels(16) title "q=1 (experiment)", \
    rep50 using 14:xticlabels(16) lt rgb 'red' title "q=50 (approximation)", \
    rep50 using 7:xticlabels(16) title "q=50 (experiment)", \
    rep100 using 14:xticlabels(16) lt rgb 'red' title "q=100 (approximation)", \
    rep100 using 7:xticlabels(16) title "q=100 (experiment)"



#set output outputdir."simulation-traffic-messages.png"
#set xlabel 'Number of replicated queries for N=100'
#set ylabel 'Average number of rounds the first query terminate with'
#set style data histogram
#plot file using 10:xticlabels(16) title "Number of Messages"

#set output outputdir."simulation-traffic-triples.png"
#set xlabel 'Number of replicated queries for N=100'
#set ylabel 'Average number of messages sent to neighbours for the first terminated query'
#set style data histogram
#plot file using 13:xticlabels(16) title "Number of triples"

#set yrange [90:110]
#set output outputdir."simulation-completeness.png"
#set xlabel 'Number of replicated queries for N=100'
#set ylabel 'Average completeness for the first terminated query'
#set style data histogram
#plot file using 8:xticlabels(16) notitle
