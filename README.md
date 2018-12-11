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

Instrumentation of Object size is made with the primitive Agent available in java.


### Test the experiment

```bash
cd snob-v2-simulation/
## Install firstly
mvn clean package shade:shade ## or bash install.bash
java -jar target/snob.jar --config test.conf
```

Check the configuration file in configs/generated folder.

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


## Variation of N and Q with K=1 on random pick withs graph



## Results
