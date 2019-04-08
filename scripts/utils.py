from math import log

# Return the line where kmax is reached for the list of row
# if kmax is not reached return the last line
# eg of row: 730,5,20,19,20,40.0,0,70,0,1,0.0,1000,7,1,false,17
def sampleByP(sample = [], p = 0.99):
    sizeIndex = 11
    kIndex = 4
    result = []
    for row in sample:
        kmax = float(row[sizeIndex]) * log(1/(1 - p))
        if float(row[kIndex]) > kmax:
            result = row
            break;
    if len(result) == 0:
        return [] # sample[len(sample) - 1]
    else:
        return result


def sampleRatioByRand(sample = [], rep = 1):
    result = {
        "rand": [],
        "ratio": []
    }
    randIndex = 2
    distinctIndex = 3
    sizeIndex = 11
    for row in sample:
        result["rand"].append(int(row[randIndex]))
        ratio = int(row[distinctIndex]) / int(row[sizeIndex]) * 100
        result["ratio"].append(ratio)
    return result


def returnMax(sample):
    max = 0
    randIndex = 2
    for row in sample:
        if int(row[randIndex]) > max:
            max = int(row[randIndex])
    return max