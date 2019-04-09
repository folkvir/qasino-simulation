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
# spray experiment for different network size
bash scripts/spray.bash
# qasino full (montecarlo and las vegas with and without IBLT) (warning it requires around 600go of ram)
bash scripts/snob-spray-both.bash
# generate traffic plots with python3
python3 scripts/traffic.py results/<myxpdir>/
# generate other plots still with python3
python3 scripts/snob-spray.py results/<myxpdir>/
```

## Generate the variations

```


## Results

### Query cardinalities
|| TP1 | TP2  | TP3 | TP4  | TP5 | TP6  | TP7 | Total |
|---| --- | ---  | --- | ---  | --- | ---  | --- | --- |
| q17| 1 | 1  |  |   |  |   |  | 2 |
| q22 | 1 | 4213  |  |   |  |   |  | 4214 |
| q54 | 1 | 2889  | 1284 | 1284  |  |   |  | 5458 |
| q73 | 2 | 4213  | 2889 | 9670  | 1284 | 1284  |  | 19342 |
| q87 | 1 | 1  | 1 | 1  | 1 | 1  | 4 | 10 |
