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

### Run the experiment

Be carefull, the script will a lot of experiment in parallel. 

Xp ran on a machine with:
* Intel(R) Xeon(R) CPU E7-8870 v4 @ 2.10GHz
* 160 cores
* 1,585,239,160 kb (1.5 Tb) RAM

```bash
bash install.bash
bash xp.bash # or nohup bash xp.bash > xp.log &
```

For just run a single configuration file.
```
mvn clean package shade:shade
java -jar target/snob.jar --init # will create a lot of configuration file in configs/generated/ folder
# the config file must be place in configs/generated folder and put in the command using the basename of the file.
java -jar target/snob.jar --config <config.conf> // will run the experiment using the specified config file. 
```


## Variation of N and Q with K=1 on random pick withs graph

stdout: n q n/q res n*ln(n)/q

x: n in logscale
y: res/(nln(n)/q)


## Results
