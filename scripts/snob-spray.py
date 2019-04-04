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

for root, dirs, files in os.walk(pathtoexp):
    for filename in files:
        pathfilename = pathtoexp + filename
        name,ext = os.path.splitext(pathfilename)
        if ext == ".csv":
            # only process with traffic enabled
            if "enabled" in name:
                with open(pathfilename, 'r') as csvfile:
                    plots = csv.reader(csvfile, delimiter=',')
                    data = []
                    peers = {}
                    N=1000
                    k = float(N) * log(1/(1 - p))
                    for row in plots:
                        # add k into the row
                        row.append(k)
                        # add the row into queries[number][r=X]
                        if not row[18] in queries: queries[row[18]] = {}
                        if not row[16] in queries[row[18]]: queries[row[18]][row[16]] = []
                        queries[row[18]][row[16]].append(row)



                    #rand=np.random.randint(low=0, high=len(peers.keys()))
                    #vrand=[*peers][rand]

print(queries["17"]["100"])