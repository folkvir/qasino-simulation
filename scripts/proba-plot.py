import matplotlib.pyplot as plt
import numpy as np
import csv
import argparse
import os

parser = argparse.ArgumentParser(description='Plot an experiment built using proba-exec.bash (add a relative path to the file)')
parser.add_argument('path', metavar='path', type=str, help='Path to the experiment')
args = parser.parse_args()

pathtoexp = os.getcwd() + '/' + args.path
print(pathtoexp)

rows = []
completeness = []
rounds = []
complete = []
notcomplete = []

with open(pathtoexp,'r') as csvfile:
    plots = csv.reader(csvfile, delimiter=',')
    for row in plots:
        rows.append(row[0])
        rounds.append(row[14])
        comp = (int(row[13]) / int(row[14]) * 100)
        if comp == 100:
            complete.append(row[0])
        else:
            notcomplete.append(row[0])
        completeness.append(comp)

print('Number of complete executions: ', len(complete))
print('Number of incomplete executions: ', len(notcomplete))

sum = 0
for val in completeness:
    sum += val
mean = sum / len(completeness)

print('Mean', mean)

print('Proportion of complete: ', len(complete) / len(rows) * 100)

# fig, ax = plt.subplots()
