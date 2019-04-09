import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
import csv
import argparse
import os
from math import log, exp
from math import exp
from utils import sampleByP, sampleRatioByRand, returnMax
import sys
from functools import reduce

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



def plotBarAverageTermination(q, query, p):
    fig, ax = plt.subplots(figsize=(8, 6), nrows=1, ncols=1, sharex=True)
    keys = []
    for k in [*query]:
        keys.append(int(k))
    keys = sorted(keys)
    print(keys)
    ind = np.arange(len(keys))
    ax.set_xticklabels(keys)
    ax.set_xticks(ind)
    points = {}
    points = [[], []]
    for rep in keys:
        rep = str(rep)
        values = []
        for sample in query[rep]:
            row_complete = []
            last = []
            for row in query[rep][sample]:
                if float(row[3]) == 1000.0 and len(row_complete) == 0: row_complete = row
                last = row
            if len(row_complete) == 0:
                row_complete = [0, 0, 0, 0]
            #print(row_complete, last)
            values.append([row_complete[2], last[2]])
        # now count how many of them are complete, and print the row
        average = [0, 0]
        for row in values:
            average[0] += int(row[0])
            average[1] += int(row[1])
        average[0] = average[0] / len(values)
        average[1] = average[1] / len(values)
        points[0].append(average[0])
        points[1].append(average[1])

    width=0.4
    r1 = ax.bar(ind, height=points[1], width=width, color="SkyBlue", label="#calls for terminating")
    r2 = ax.bar(ind, height=points[0], width=width, color="IndianRed", label="#calls for completion")
    ax.legend()
    autolabel(r1, ax)
    autolabel(r2, ax)
    ax.set_xlabel("Number of replicated queries")
    ax.set_ylabel("Number of call to rand()")
    ax.set_yscale('log')
    plt.suptitle("Network size: N = 1000 and Expected proportion of the network seen: p = 0.999999999")
    plt.savefig(fname=args.path + '/global-query-' + str(q) + '.png', quality=100, format='png', dpi=100)

def plotKbyQforDifferentP(q, query):
    keys = []
    for k in [*query]:
        if k != "1000":
            keys.append(int(k))
    keys = sorted(keys)
    ind = np.arange(len(keys))
    replicated = {}
    fig, ax = plt.subplots(figsize=(8, 6), nrows=1, ncols=1, sharex=True)
    ps = [0.9, 0.99, 0.999, 0.9999,
          0.99999, 0.999999, 0.9999999, 0.99999999,
          0.999999999]
    for rep in keys:
        if rep not in replicated: replicated[rep] = {}
        for p in ps:
            replicated[rep][p] = {"rand": [], "distinct": []}
        for sample in query[str(rep)]:
            indexRand = 2
            indexDistinct = 3
            for p in ps:
                row = sampleByP(query[str(rep)][sample], p)
                if len(row) > 0:
                    replicated[rep][p]["rand"].append(int(row[indexRand]))
                    replicated[rep][p]["distinct"].append(int(row[indexDistinct]))
                else:
                    print("Someone has no value for p=" + str(p))
                    sys.exit(1)
    byProportion = {}
    for p in ps:
        if not p in byProportion: byProportion[p] = {"x": [], "y":[]}
        x = keys
        y = []
        for rep in keys:
            randvalues = replicated[rep][p]["rand"]
            average = reduce(lambda a,b : a+b, randvalues)
            average = average / len(randvalues)
            y.append(average)

        ax.loglog(x, y, label="p="+str(p))
    ax.set_xticks(keys)
    ax.set_xticklabels(keys)
    #ax.ticklabel_format(axis='y', style='plain')
    #ax.set_yscale("log")
    ax.set_ylabel("(log scale) Number of calls to rand() for terminating the query " + str(q))
    ax.set_xlabel("(log scale) Number of nodes executing the query " + str(q))
    ax.legend()
    #plt.ylim(bottom=0)
    plt.savefig(fname=args.path + '/randbyqforp' + str(q) + '.png', quality=100, format='png', dpi=100)


def completenessByRand(q, query):
    keys = []
    for k in [*query]:
        if k != "1000":
            keys.append(int(k))
    keys = sorted(keys)
    ind = np.arange(len(keys))
    replicated = {}
    fig, ax = plt.subplots(figsize=(8, 6), nrows=1, ncols=1)
    fig1, ax1 = plt.subplots(figsize=(8, 6), nrows=1, ncols=1)
    for rep in keys:
        values = {
            "rand": [],
            "ratio": []
        }
        maximum = 0
        interpolatedSample = {}
        for sample in query[str(rep)]:
            maximum = max(returnMax(query[str(rep)][sample]), maximum)
            # tmpvalues = sampleRatioByRand(query[str(rep)][sample], rep)
            # values["rand"].extend(tmpvalues["rand"])
            # values["ratio"].extend(tmpvalues["ratio"])
        x = range(0, maximum) # rand
        y = []
        for sample in query[str(rep)]:
            tmpvalues = sampleRatioByRand(query[str(rep)][sample], rep)
            interpolatedSample[sample] = np.interp(x, xp=tmpvalues["rand"], fp=tmpvalues["ratio"])
        for key in x:
            sum = 0
            for sample in query[str(rep)]:
                sum += interpolatedSample[sample][key]
            average = sum/len(query[str(rep)])
            y.append(average)

        ax.plot(y, x, label="q="+str(rep))
        reversed(x)
        y.reverse()
        ax1.plot(y, x, label="q="+str(rep))


    ax.set_xlabel("Ratio of distinct nodes seen by the network size (in %)")
    ax.set_ylabel("(log scale) Number of calls to rand()")
    ax.set_yscale('log')

    ax1.set_xlabel("Ratio of distinct nodes seen by the network size (in %)")
    ax1.set_ylabel("(log scale) Number of calls to rand()")
    #ax1.set_yscale('log')
    #ax1.set_ylim(bottom=1, top=1000*(log(1000)+0.577))

    ax1.invert_xaxis()
    #ax1.set_xscale('log')

    ax1.set_yscale('log')
    #ax1.ticks(ticks=ax1.get_xticks(), labels=xticks)
    ax1.set_xlim(left=0, right=100)

    print(ax1.get_xticks())
    ax1.xaxis.set_ticks([0, 100])
    #ax.yaxis.set_major_formatter(ticker.FormatStrFormatter('%d'))
    ax.legend()
    fig.savefig(fname=args.path + '/ratiobyrand' + str(q) + '.png', quality=100, format='png', dpi=100)
    fig1.savefig(fname=args.path + '/zoom-ratiobyrand' + str(q) + '.png', quality=100, format='png', dpi=100)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Plot an experiment built using snob-spray.bash (add a relative path to the dir)')
    parser.add_argument('path', metavar='path', type=str, help='Path to the experiment')
    parser.add_argument('p', metavar='p', type=str, help='Proportion of the network desired')
    args = parser.parse_args()
    p=float(args.p)
    pathtoexp = os.getcwd() + '/' + args.path
    trafficEnabled = []
    trafficDisabled = []
    queries={}
    for root, dirs, files in os.walk(pathtoexp):
        for filename in files:
            pathfilename = pathtoexp + filename
            name,ext = os.path.splitext(pathfilename)
            if ext == ".csv":
                # only process with traffic enabled
                if "enabled" in name:
                    with open(pathfilename, 'r') as csvfile:
                        plots = csv.reader(csvfile, delimiter=',')
                        N=1000
                        k = float(N) * log(1/(1 - p))
                        #print("Reading: " + filename)
                        run = filename.split('-')[6]
                        for row in plots:
                            # add k into the row
                            row.append(k)
                            # add the row into queries[number][r=X]
                            if not row[15] in queries: queries[row[15]] = {}
                            if not row[13] in queries[row[15]]: queries[row[15]][row[13]] = {}
                            if not run in queries[row[15]][row[13]]: queries[row[15]][row[13]][run] = []
                            queries[row[15]][row[13]][run].append(row)
    for q in queries:
        #start parallel process
        #plotBarAverageTermination(q, queries[q].copy(), args.p)
        #plotKbyQforDifferentP(q, queries[q].copy())
        completenessByRand(q, queries[q].copy())

