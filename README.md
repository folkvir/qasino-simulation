# snob-v2-simulation
Snob v2, Traffic reduced (IBFL), Incremental evaluation (Symmetric Hash Join using iterators)

Support for only SELECT (DISTINCT) conjunctive queries without filter only:
- Projection
- Selection
- And Join (Symmetric Hash Join)

## Requirements (tested with)

Executing the simulation: `Java version "1.8.0_161"`

Executing Scripts: 
- `NodeJs v11.6.0`
- `GNU bash, version 3.2.57(1)-release`

Plotting: `Gnuplot Version 5.2 patchlevel 2`
 
## Build and run

```bash
mvn clean package
java -javaagent:target/snob.jar -jar target/snob.jar --execute=<filename>
# if you want a list of all options
java -jar target/snob.jar --help
```

Where `<filename>` is the **name** of the file in th folder `./configs/generated/` 


### Test the experiment

This is a simple run for a single query with a minimized traffic using a clique

Parameters:
* N=1000
* q=64
* |Pv|=100
* |ShuffleLength| = 50
* query=17 (query 17 will be run)

```bash
cd snob-v2-simulation/
## Install firstly
mvn clean package ## or bash install.bash
# otherwise
java -jar target/snob.jar --execute=test.conf
```

* Check the configuration file in configs/generated folder.
* Check all queries in datasets/data/diseasome/queries/queries_jena_generated.json
or generated this file by running the test in the GenerateTest file (snob-v2-simulation/src/test/java/snob/simulation/GenerateTest.java).
### Run the experiment (warning it will run a lot of experiment) 

Be carefull, the script will load a lot of experiment in parallel.
180 processes in parallel (use approximately 500go of RAM. 

Xp ran on a machine with:
* Intel(R) Xeon(R) CPU E7-8870 v4 @ 2.10GHz
* 160 cores
* 1,585,239,160 kb (1.5 Tb) RAM

```bash
nohup bash xp.bash &
# check in results folder for the results
# when terminate, generate average on each values, then compute plots
cd scripts/
## if you want averages, install Nodejs
npm install && node mean.js ../results/<mygeneratedexperimentfolder>/
bash sort.bash ../results/<mygeneratedexperimentfolder>/
## Modify in simulation.gnuplot the input with <mygeneratedexperimentfolder>
gnuplot simulation.gnuplot
## Modify in simulation.gnuplot the input with <mygeneratedexperimentfolder>
gnuplot simulation-one-query.gnuplot

## Generate the variations

```


## Variations of N without the clique

````
mvn clean package shade:shade
java -cp target/classes/ snob.simulation.VarNoclique
````

![](scripts/variations/plotN.png)

## Variations of N using the clique

````
mvn clean package shade:shade
java -cp target/classes/ snob.simulation.VarClique
````

![](scripts/variations/plotNClique.png)


## Results

### Query cardinalities
|| TP1 | TP2  | TP3 | TP4  | TP5 | TP6  | TP7 | Total |
|---| --- | ---  | --- | ---  | --- | ---  | --- | --- |
| q17| 1 | 1  |  |   |  |   |  | 2 |
| q22 | 1 | 4213  |  |   |  |   |  | 4214 |
| q54 | 1 | 2889  | 1284 | 1284  |  |   |  | 5458 |
| q73 | 2 | 4213  | 2889 | 9670  | 1284 | 1284  |  | 19342 |
| q87 | 1 | 1  | 1 | 1  | 1 | 1  | 4 | 10 |


### 5 queries

### Using the clique

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/simulation-w-clique-traffictrue.png)

### Without the clique

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/simulation-wo-clique-traffictrue.png)

## Query 17

### Rounds

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q17-simulation-round-traffictrue.png)

### Traffic

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q17-simulation-traffic-triples.png)
![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q17-simulation-traffic-messages.png)

### Comparison between Algorithm 1 and 2
![](results/review2-a13fa1a657bcaed2b0c17154e6b69b2b-all/q17-review2-simulation-round-traffictrue.png)

## Query 22

### Rounds

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q22-simulation-round-traffictrue.png)

### Traffic

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q22-simulation-traffic-triples.png)
![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q22-simulation-traffic-messages.png)

### Comparison between Algorithm 1 and 2
![](results/review2-a13fa1a657bcaed2b0c17154e6b69b2b-all/q22-review2-simulation-round-traffictrue.png)

## Query 54

### Rounds

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q54-simulation-round-traffictrue.png)

### Traffic

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q54-simulation-traffic-triples.png)
![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q54-simulation-traffic-messages.png)

### Comparison between Algorithm 1 and 2
![](results/review2-a13fa1a657bcaed2b0c17154e6b69b2b-all/q54-review2-simulation-round-traffictrue.png)

## Query 73

### Rounds

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q73-simulation-round-traffictrue.png)

### Traffic

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q73-simulation-traffic-triples.png)
![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q73-simulation-traffic-messages.png)

### Comparison between Algorithm 1 and 2
![](results/review2-a13fa1a657bcaed2b0c17154e6b69b2b-all/q73-review2-simulation-round-traffictrue.png)


## Query 87

### Rounds

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q87-simulation-round-traffictrue.png)

### Traffic

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q87-simulation-traffic-triples.png)
![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q87-simulation-traffic-messages.png)

### Comparison between Algorithm 1 and 2
![](results/review2-a13fa1a657bcaed2b0c17154e6b69b2b-all/q87-review2-simulation-round-traffictrue.png)
