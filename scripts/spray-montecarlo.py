import argparse
import os
from math import log
import matplotlib.pyplot as plt
from functools import reduce
import numpy as np
import csv

def parseFile(file):
    print("Processing ", file)
    peers = {}
    with open(file, 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        next(plots)
        next(plots)
        next(plots)
        for row in plots:
            if not row[0] in peers: peers[row[0]] = []
            peers[row[0]].append(row)
    return peers

def getProportionsForPs(sample, ps = []):
    result = []
    for p in ps:

        kmax = 1000 * log(1 / (1 - p))
        stop = False
        values = (0, 0, 0)
        for row in sample:
            if not stop and float(row[3]) > kmax:
                values = (int(row[2]), int(row[3]), p)
                stop = True
        if values == (0, 0, 0):
            values = (int(row[2]), int(row[3]), p)
        result.append(values)
    return result

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Plot an experiment built using spray-estimator.conf')
    parser.add_argument('path', metavar='path', type=str, help='Path to the experiment result file')
    args = parser.parse_args()
    pathtoexp = os.getcwd() + '/' + args.path
    peers = parseFile(pathtoexp)

    ps = [0.25, 0.5, 0.75, 0.9, 0.99, 0.999, 0.9999]
    print('Proportions draws:', ps)
    proportions = {}
    theoritical = []
    for peer in peers:
        for val,rand,p in getProportionsForPs(peers[peer], ps):
            if not p in proportions: proportions[p] = []
            proportions[p].append(val)

    ind = np.arange(len([*proportions]))
    keys = sorted([*proportions])

    scatterx = []
    scattery = []

    y = []
    for p in keys:
        print('Nomber of points: ', len(proportions[p]))
        average = reduce(lambda a,b: a+b, proportions[p]) / len(proportions[p])
        for val in proportions[p]:
            scatterx.append(p)
            scattery.append(val)
        y.append(average)
        theoritical.append(1000 * p)
    fig, ax = plt.subplots(figsize=(8, 6), nrows=1, ncols=1)

    keys2 = []
    for k in keys:
        keys2.append(1 - k)

    y2 = []
    for k in y:
        y2.append(1000 - k)

    th2 = []
    for t in theoritical:
        th2.append(1000-t)

    scatter2x = []
    for k in scatterx:
        scatter2x.append(1 - k)

    scatter2y = []
    for k in scattery:
        scatter2y.append(1000 - k)
    ax.scatter(keys2, y2, color="SkyBlue", label = 'Spray')
    #ax.scatter(scatter2x, scatter2y, color="SkyBlue", label = 'Spray')
    ax.plot(keys2, th2, color="IndianRed", label = '1000 * (1 - p)')
    ax.set_xscale('log')
    ax.set_yscale('log')
    ax.legend()
    ax.set_ylabel("Number of distinct nodes yet to be seen")
    ax.set_xlabel("1 - p")
    fig.savefig(fname=args.path + '.montecarlo.png', quality=100, format='png', dpi=100)
