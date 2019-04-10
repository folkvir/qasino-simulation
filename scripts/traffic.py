import matplotlib.pyplot as plt
import numpy as np
import csv
import argparse
import os
from math import log
from math import exp
import sys

parser = argparse.ArgumentParser(description='Plot an experiment built using snob-spray.bash (add a relative path to the dir)')
parser.add_argument('path', metavar='path', type=str, help='Path to the experiment')
args = parser.parse_args()

pathtoexp = os.getcwd() + '/' + args.path

queries={}

for root, dirs, files in os.walk(pathtoexp):
    for filename in files:
        pathfilename = pathtoexp + filename
        name,ext = os.path.splitext(pathfilename)
        if ext == ".csv":
            # only process with traffic enabled
            with open(pathfilename, 'r') as csvfile:
                plots = csv.reader(csvfile, delimiter=',')
                peers = {}
                for row in plots:
                        try:
                            #if row[16] != "1000":
                                # add the row into queries[number][r=X]
                                if not row[15] in queries: queries[row[15]] = {}
                                if not row[13] in queries[row[15]]: queries[row[15]][row[13]] = {}
                                if not row[14] in queries[row[15]][row[13]]: queries[row[15]][row[13]][row[14]] = []

                                if float(row[3]) == float(row[11]) and not(row[0] in peers):
                                    queries[row[15]][row[13]][row[14]].append(row)
                                    peers[row[0]] = row
                        except:
                            print("One file has an empty row")



for q in queries:
    for replicate in queries[q]:
        for traffic in queries[q][replicate]:
            # now make the average on all the column of index 6
            average = 0;
            for row in queries[q][replicate][traffic]:
                average += float(row[6])
            queries[q][replicate][traffic] = average / len(queries[q][replicate][traffic])

width = 0.35  # the width of the bars

def autolabel(rects, ax, format='{}', xpos='center'):
    """
    Attach a text label above each bar in *rects*, displaying its height.

    *xpos* indicates which side to place the text w.r.t. the center of
    the bar. It can be one of the following {'center', 'right', 'left'}.
    """

    xpos = xpos.lower()  # normalize the case of the parameter
    ha = {'center': 'center', 'right': 'left', 'left': 'right'}
    offset = {'center': 0.5, 'right': 0.57, 'left': 0.43}  # x_txt = x + w*off

    for rect in rects:
        height = rect.get_height()
        ax.text(rect.get_x() + rect.get_width()*offset[xpos], 1.01*height,
                format.format(height), ha=ha[xpos], va='bottom')

def plotBar(query, values):
    fig, ax = plt.subplots(figsize=(8, 6), nrows=1, ncols=1, sharex=True)
    keys = []
    for k in values.keys():
        keys.append(int(k))
    keys = sorted(keys)
    ind = np.arange(len(keys))
    trues=[]
    falses=[]
    xticks = []
    for rep in keys:
        trues.append(values[str(rep)]["true"])
        falses.append(values[str(rep)]["false"])
        xticks.append(str(rep))

    r1 = ax.bar(ind - width/2, height=trues, width=width, color="SkyBlue", label="With IBLTs")
    r2 = ax.bar(ind + width/2, height=falses, width=width, color="IndianRed", label="Without IBLT")

        #r1 = ax.bar(ind - width/2, height=values[rep]["true"], width=width, color='SkyBlue', label="Enabled", tick_label=str(rep))
        #r2 = ax.bar(ind + width/2, height=values[rep]["false"], width=width, color='IndianRed', label="Disabled", tick_label=str(rep))
    autolabel(r1, ax, "{:.0f}", 'left')
    autolabel(r2, ax, "{:.0f}", 'right')
    ax.set_xticks(ind)
    ax.set_xticklabels(keys)
    ax.set_yscale('log')
    ax.set_ylabel("Number of triples received")
    ax.set_xlabel("Number of nodes executing the same query")
    #ax.set_title("Average number of triples received for the query " + str(query))
    ax.legend()
    plt.savefig(fname=args.path + '/query-' + str(query) + '.png', quality=100, format='png', dpi=100)


for q in queries:
    print("Plotting traffic of query: ", q)
    plotBar(q, queries[q])

