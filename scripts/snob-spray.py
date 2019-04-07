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
parser.add_argument('p', metavar='p', type=str, help='Proportion of the network desired')
args = parser.parse_args()

p=float(args.p)
pathtoexp = os.getcwd() + '/' + args.path
fig, ax = plt.subplots()

trafficEnabled = []
trafficDisabled = []

queries={}

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
        print(values)
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


def plotBar (q, query, p):
    for rep in query:
        succeeded = []
        for sample in query[rep]:
            row_complete = []
            found = False
            for row in query[rep][sample]:
                if float(row[4]) <= row[16] and float(row[10]) == 1.0 and len(row_complete) == 0: row_complete = row
                if float(row[4]) > row[16]:
                    if len(row_complete) == 0: row_complete = row
                    row.append(row_complete)
                    succeeded.append(row)
                    found = True
                    break;
            # print(found, row_complete)
        # now count how many of them are complete, and print the row
        completes = []
        for suc in succeeded:
            if float(suc[10]) == 1.0:
                completes.append(suc)

        # now we have only complete answer we can only keep what we need in average
        # the number fo shuffle done
        # the number of rand() done
        # the number of shuffles and rand for completion
        saved = []
        for row in completes:
            tosave = [row[1], row[2]]
            if len(row[17]) > 0:
                tosave.append(row[17][1])
                tosave.append(row[17][2])
            saved.append(tosave)
        query[rep] = [saved, len(saved), len(query[rep]), rep, q]
        #print(q, rep, queries[q][rep])
    rep = query
    keys = []
    # sort q
    for k in [*rep]:
        keys.append(int(k))
    keys = sorted(keys)

    terminated = []
    notterminated = []
    botterminatedaverage = []
    botcompletedaverage = []
    for k in keys:
        terminated.append(rep[str(k)][1])
        notterminated.append(rep[str(k)][2])
        if len(rep[str(k)][0]) > 0:
            averagecompleted = 0
            averageterminated = 0
            for row in rep[str(k)][0]:
                averageterminated += int(row[1])
                averagecompleted += int(row[3])
            averageterminated = averageterminated / len(rep[str(k)][0])
            averagecompleted = averagecompleted / len(rep[str(k)][0])
            botterminatedaverage.append(averageterminated)
            botcompletedaverage.append(averagecompleted)
        else:
            botterminatedaverage.append(0)
            botcompletedaverage.append(0)
    ind = np.arange(len(keys))
    width= 0.4
    print(len(botcompletedaverage), len(botterminatedaverage), len(keys))
    fig, (top, bottom) = plt.subplots(figsize=(10, 6), nrows=2, ncols=1, sharex=True)
    top.bar(ind, height=terminated, width=width, color="SkyBlue")
    bottom.bar(ind, height=botterminatedaverage, width=width, color="SkyBlue", label="#calls for terminating")
    bottom.bar(ind, height=botcompletedaverage, width=width, color="IndianRed", label="#calls for completion")
    bottom.legend()
    bottom.set_xticks(ind)
    bottom.set_xticklabels(keys)
    bottom.set_yscale('log')
    bottom.set_xlabel("Number of replicated queries")
    top.set_title("Number of completed and terminated executions (" + str(notterminated[0]) + " samples)")
    bottom.set_title("Average call to rand() for terminating and completing the execution (" + str(notterminated[0]) + " samples)")
    # ax.legend()
    plt.suptitle("Network size: N = 1000 and Expected proportion of the network seen: p = " + str(p))
    plt.savefig(fname=args.path + '/p-' + p+ '-query-' + str(q) + '.png', quality=100, format='png', dpi=100)

def plotGlobalDistinct

for q in queries:
    plotBarAverageTermination(q, queries[q], args.p)
    plotBar(q, queries[q], args.p)

