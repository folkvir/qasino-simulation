# snob-v2-simulation
Snob v2, Traffic reduced (IBFL), Incremental evaluation (Iterators), Termination criterion

Support for only SELECT (DISTINCT) conjunctive queries without filter only:
- Projection
- Selection
- And Join (Symmetric Hash Join)

## Build and run

```
mvn clean package shade:shade
java -javaagent:target/snob.jar -jar target/snob.jar --config <filename>
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
mvn clean package shade:shade ## or bash install.bash
java -jar target/snob.jar --config test.conf
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
bash mean ../results/<mygeneratedexperimentfolder>/*conf.txt
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

## Query 22

### Rounds

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q22-simulation-round-traffictrue.png)

### Traffic

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q22-simulation-traffic-triples.png)
![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q22-simulation-traffic-messages.png)

## Query 54

### Rounds

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q54-simulation-round-traffictrue.png)

### Traffic

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q54-simulation-traffic-triples.png)
![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q54-simulation-traffic-messages.png)

## Query 73

### Rounds

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q73-simulation-round-traffictrue.png)

### Traffic

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q73-simulation-traffic-triples.png)
![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q73-simulation-traffic-messages.png)

## Query 87

### Rounds

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q87-simulation-round-traffictrue.png)

### Traffic

![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q87-simulation-traffic-triples.png)
![](results/55f16ca24b08ef2725fbbc5088942a1e-all/q87-simulation-traffic-messages.png)

