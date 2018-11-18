# snob-v2-simulation
Snob v2, Traffic reduced (IBFL), Incremental evaluation (Iterators), Termination criterion (CMS estimation)

```
mvn clean package shade:shade
java -javaagent:target/snob.jar -jar target/snob.jar snob.txt
```

Instrumentation of Object size is made with the primitive Agent available in java.

## Run the experiment.
```bash
bash install.bash
bash xp.bash # or nohup bash xp.bash > xp.log &
```