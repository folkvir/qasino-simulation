#!/usr/bin/env bash
sh install.sh

java -Xms500000m -javaagent:target/snob.jar -jar target/snob.jar snob-50-rps+son.txt
java -Xms500000m -javaagent:target/snob.jar -jar target/snob.jar snob-50-rps.txt
java -Xms500000m -javaagent:target/snob.jar -jar target/snob.jar snob-100-rps+son.txt
java -Xms500000m -javaagent:target/snob.jar -jar target/snob.jar snob-100-rps.txt
java -Xms500000m -javaagent:target/snob.jar -jar target/snob.jar snob-200-rps+son.txt
java -Xms500000m -javaagent:target/snob.jar -jar target/snob.jar snob-200-rps.txt
wait
