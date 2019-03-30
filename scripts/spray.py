import matplotlib.pyplot as plt
import numpy as np
import csv
import argparse
import os
from math import log

parser = argparse.ArgumentParser(description='Plot an experiment built using spray.bash (add a relative path to the file)')
parser.add_argument('path', metavar='path', type=str, help='Path to the experiment')
args = parser.parse_args()

globalx = []
globaly = []
points = []
pathtoexp = os.getcwd() + '/' + args.path
fig, ax = plt.subplots()
for root, dirs, files in os.walk(pathtoexp):
    for filename in files:
        pathfilename = pathtoexp + filename
        name,ext = os.path.splitext(pathfilename)
        if ext == ".csv":
            x = []
            meanx = 0
            y = []
            meany = 0
            with open(pathfilename, 'r') as csvfile:
                plots = csv.reader(csvfile, delimiter=',')
                for row in plots:
                    x.append(row[2])
                    y.append(row[3])
            for tmpx in x:
                meanx += int(tmpx)
            meanx = meanx / len(x)
            for tmpy in y:
                meany += int(tmpy)
            meany = meany / len(y)

            points.append([meanx, meany])
            #ax.plot(x, y)


def takeSecond(elem):
    return elem[0]

points2 = {}
pointssize = {}
for pt in points:
    points2[int(pt[0])] = 0
    pointssize[int(pt[0])] = 0

for pt in points:
    points2[int(pt[0])] += pt[1]
    pointssize[int(pt[0])] += 1

points = []
for key in points2:
    points.append([key, points2[key]/pointssize[key]])

points.sort(key=takeSecond)
# compute the approximation of n log n
globalxappro = []
globalyappro = []
# x * n where x = ln(1 / (1-p)) where p = 0.9990
globalxappro2 = []
globalyappro2 = []
# x * n where x = ln(1 / (1-p)) where p = 0.99
globalxappro3 = []
globalyappro3 = []
# x * n where x = ln(1 / (1-p)) where p = 0.99999999999
globalxappro4 = []
globalyappro4 = []

# bleu sur deux rouges
bleu2rougex = []
bleu2rougey = []

points.sort(key=takeSecond)
for p in points:
    globalx.append(p[0])
    globaly.append(p[1])
    globalxappro.append(p[0])
    globalyappro.append(p[0] * (log(p[0]) + 0.5772156649))
    globalxappro2.append(p[0])
    globalyappro2.append(p[0] * log(1/(1-0.9999)))
    globalxappro3.append(p[0])
    globalyappro3.append(p[0] * log(1/(1-0.99)))
    globalxappro4.append(p[0])
    globalyappro4.append(p[0] * log(1/(1-0.99999)))
    bleu2rougex.append(p[0])
    bleu2rougey.append(p[1] / ((p[0] * (log(p[0]) + 0.5772156649))))


# ax.plot(globalx, globaly, color="blue", label="Spray")
# ax.plot(globalxappro, globalyappro, color="red", label="n*(ln(n)+\u03B3)")
# ax.plot(globalxappro4, globalyappro4, color="purple", label="n*ln(1/(1-p)) with p=0.99999")
# ax.plot(globalxappro2, globalyappro2, color="green", label="n*ln(1/(1-p)) with p=0.9999")
# ax.plot(globalxappro3, globalyappro3, color="orange", label="n*ln(1/(1-p)) with p=0.99")
ax.legend()
plt.xlabel("Number of Nodes")
plt.ylabel("Number of Rounds")


ax.plot(bleu2rougex, bleu2rougey)

plt.show()