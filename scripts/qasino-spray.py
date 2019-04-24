import matplotlib.pyplot as plt
from matplotlib.transforms import blended_transform_factory
import numpy as np
import csv
import argparse
import os
from math import log
from utils import sampleByP, sampleRatioByRand, returnMax, findStopCompleteSeen
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


def plotBarBis(q, query, p):
    result = {
        "left": {"x": [], "y": []},
        "right": {"x": [], "y": []}
    }
    maxSample = 72
    for rep in query:
        succeeded = []
        for sample in query[rep]:
            if int(sample) < maxSample:
                for row in query[rep][sample]:
                    if float(row[4]) > row[16]:
                        succeeded.append(row)
                        break;
            #print(found, row_complete)
        # now count how many of them are complete, and print the row
        completes = []
        for suc in succeeded:
            if float(suc[10]) == 1.0:
                completes.append(suc[2])

        query[rep] = [completes, len(completes), maxSample, rep, q]
        #print(query[rep])
    keys = []
    # sort q
    for k in [*query]:
        keys.append(int(k))
    keys = sorted(keys)

    terminated = []
    botterminatedaverage = []
    botcompletedaverage = []
    for rep in keys:
        terminated.append(int(query[str(rep)][1]) / int(query[str(rep)][2]) * 100)
        if len(query[str(rep)][0]) > 0:
            averagecompleted = 0
            averageterminated = 0
            for val in query[str(rep)][0]:
                #print(row)
                averageterminated += int(val)
            # query[str(rep)][0]) is the array of completed row[2]=rand values [1000, 30042, ...]
            averageterminated = averageterminated / len(query[str(rep)][0])
            botterminatedaverage.append(averageterminated)
        else:
            botterminatedaverage.append(0)

    ind = np.arange(len(keys))
    result["keys"] = keys
    result["right"]["y"] = terminated
    result["right"]["x"] = np.arange(len(keys))
    result["left"]["y"] = botterminatedaverage
    result["left"]["x"] = np.arange(len(keys))
    return result

def plotBar (queries, p):
    print("Plotting plotBar with plotBarBis ...")
    bigfig, axes = plt.subplots(figsize=(16, 12), nrows=5, ncols=2,  sharex=True)
    keys = []
    width=0.5
    i = 0
    print(axes)
    for q in queries:
        result = plotBarBis(q, queries[q].copy(), p)
        xl, yl = result["keys"], result["left"]["y"]
        xr, yr = result["keys"], result["right"]["y"]
        keys = result["keys"]
        ind = np.arange(len(keys))
        # ======= SMALL FIG
        fig, axsmalles = plt.subplots(figsize=(8, 6), nrows=2, ncols=1, sharex=True)
        axsmalles[0].bar(ind, height=yr, width=width, color='SkyBlue', label="Q"+str(q))
        axsmalles[1].bar(ind, height=yl, width=width, color='SkyBlue', label="Q"+str(q))
        axsmalles[0].set_yticks([0, 20, 40, 60, 80, 100])
        axsmalles[0].set_xticks(ind)
        axsmalles[0].set_xticklabels(keys)
        axsmalles[1].set_yscale('log')
        axsmalles[0].set_title('Proportion of completed executions (%)')
        axsmalles[1].set_title('Number of iterations (#rand())')
        axsmalles[1].set_xlabel('Number of nodes executing the query ' + str(q))
        fig.savefig(fname=args.path + '/p'+ str(p) +'-q-' + str(q) + '.png', quality=100, format='png', dpi=100)

        # ======= BIG FIG
        axes[i][0].bar(ind, height=yl, color='SkyBlue', width=width, label="Q"+str(q))
        axes[i][1].bar(ind, height=yr, color='SkyBlue', width=width, label="Q"+str(q))

        axes[i][0].set_yscale("log")

        # axes[i][0].set_xscale("log")
        # axes[i][1].set_xscale("log")

        axes[i][1].set_yticks([0, 20, 40, 60, 80, 100])
        axes[i][0].legend()
        axes[i][1].legend()
        axes[i][0].set_xticks(ind)
        axes[i][0].set_xticklabels(keys)


        i += 1
    bigfig.savefig(fname=args.path + '/p-'+ str(p) +'-all.png', quality=100, format='png', dpi=100)


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

def plotBarStopAndCompleteforQone(queries):
    print("Plotting plotBarStopAndCompleteforQone ...")
    fig, ax = plt.subplots(figsize=(8, 6), nrows=1, ncols=1, sharex=True)
    keys = [*queries]
    ind = np.arange(len(keys))
    ax.set_xticklabels(map(lambda a: "Q"+a, keys))
    ax.set_xticks(ind)
    points = {}
    points = [[], []]
    #ps = [0.9, 0.99, 0.999, 0.9999, 0.99999, 0.999999, 0.9999999, 0.99999999, 0.999999999]
    ps = [0.5, 0.95, 0.97, 0.99, 0.9999]
    colors = "bgrcmykw"

    styles = [(5,2),(2,5),(4,10),(3,3,2,2),(5,2,20,2)]

    print(ind)

    width=0.2

    stop = []
    complete = []
    stopstd = []
    completestd = []
    for q in queries:
        rep = "1"
        average = []
        stopval = []
        completeval = []

        for sample in queries[q][rep]:
            if int(sample) < 40:
                average.append(findStopCompleteSeen(queries[q][rep][sample]))
        for a in average:
            stopval.append(a["stop"])
            completeval.append(a["complete"])
        sum = reduce(lambda a, b: {
            "stop": a["stop"] + b["stop"],
            "complete": a["complete"] + b["complete"]
        }, average)
        sum["stop"] = sum["stop"] / len(average)
        sum["complete"] = sum["complete"] / len(average)
        stop.append(sum["stop"])
        complete.append(sum["complete"])
        stopstd.append(np.std(stopval))
        completestd.append(np.std(completeval))

    ax.set_xlabel("The query executed by one node")
    ax.set_ylabel("Number of iterations (#rand())")
    ax.bar(ind, height=stop, yerr=stopstd, width=width, color="SkyBlue", label="Stop")
    ax.bar(ind, height=complete, yerr=completestd, width=width, color="IndianRed", label="Complete")


    i = 0
    reference_transform = blended_transform_factory(ax.transAxes, ax.transData)
    for p in ps:
        ax.axhline(y=1000*log(1/(1-p)), ls=(0, (1, 1)))
        ax.annotate("p="+str(p),
                    xy=(1, 1000*log(1/(1-p))),
                    xycoords=reference_transform,
                    xytext=(10, 0),
                    textcoords='offset points',
                    color="black",
                    fontsize=8, ha="left",
                    family="monospace")
        i = i + 1

    ax.legend()
    plt.savefig(fname=args.path + '/p-effectiveness.png', quality=100, format='png', dpi=100)


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


    ax.set_xlabel("Proportion of distinct nodes seen (in %)")
    ax.set_ylabel("(log scale) Number of calls to rand()")
    ax.set_yscale('log')

    #ax.yaxis.set_major_formatter(ticker.FormatStrFormatter('%d'))
    ax.legend()
    fig.savefig(fname=args.path + '/ratiobyrand' + str(q) + '.png', quality=100, format='png', dpi=100)


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
    print("Processing data in:" + pathtoexp)
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


    plotBar(queries.copy(), args.p)
    plotBarStopAndCompleteforQone(queries.copy())
    # for q in queries:
    #     #plotBarAverageTermination(q, queries[q].copy(), args.p)
    #     #plotKbyQforDifferentP(q, queries[q].copy())
    #     #completenessByRand(q, queries[q].copy())


