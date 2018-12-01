# snob-v2-simulation
Snob v2, Traffic reduced (IBFL), Incremental evaluation (Iterators), Termination criterion (CMS estimation)

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

### Run the experiment sequentially
```bash
bash install.bash
bash xp.bash # or nohup bash xp.bash > xp.log &
```

## Results
