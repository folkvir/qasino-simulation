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

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Plot an experiment built using tests/estimator.conf')
    parser.add_argument('path', metavar='path', type=str, help='Path to the experiment result file')
    args = parser.parse_args()
    pathtoexp = os.getcwd() + '/' + args.path
    peers = parseFile(pathtoexp)

    fig, ax = plt.subplots(figsize=(8, 6), nrows=1, ncols=1)
    fig2, ax2 = plt.subplots(figsize=(8, 6), nrows=1, ncols=1)
    xticks = []
    lasty = []
    for peer in peers:
        x = []
        y2 = []
        y = []
        for row in peers[peer]:
            x.append(int(row[1]))
            y2.append(int(row[5]))
            y.append(int(row[6]))
        ax.scatter(x=x, y=y, s=4)
        ax2.scatter(x=x, y=y2, s=4)
        xticks = x
        lasty = y
    ax.set_ylim(top=1100, bottom=900)
    #ax2.set_ylim(top=15, bottom=0)
    #ax.set_yscale('log')
    ax.set_xlabel("Cycles")
    ax.set_ylabel("Network estimation size")
    ax2.set_xlabel("Cycles")
    ax2.set_ylabel("Estimator instances")
    #ax.set_title("Convergence time for the network size estimator for a network of 1000 peers")
    ax.axhline(y=1000, ls=(0, (1, 1)), color="red", label="Real size")
    ax.legend()
    #ax2.legend()
    fig.savefig(fname=args.path + '-est.png', quality=100, format='png', dpi=100)
    fig2.savefig(fname=args.path + '-instances.png', quality=100, format='png', dpi=100)
