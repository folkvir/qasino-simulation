#!/usr/bin/env bash
mvn clean package shade:shade
mkdir results
echo "Snob simulation installed. Now run: nohup sh xp.sh > xp.log & and tail -f xp.log"