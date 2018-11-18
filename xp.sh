#!/usr/bin/env bash
sh install.sh

java -javaagent:target/snob.jar -jar target/snob.jar snob-50-rps+son.txt > snob50son.log &
java -javaagent:target/snob.jar -jar target/snob.jar snob-50-rps.txt > snob50son.log &
java -javaagent:target/snob.jar -jar target/snob.jar snob-100-rps+son.txt > snob100son.log &
java -javaagent:target/snob.jar -jar target/snob.jar snob-100-rps.txt > snob100rps.log &
java -javaagent:target/snob.jar -jar target/snob.jar snob-200-rps+son.txt > snob200son.log &
java -javaagent:target/snob.jar -jar target/snob.jar snob-200-rps.txt > snob200rps.log &
wait
