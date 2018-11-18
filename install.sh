#!/bin/bash
mvn clean package shade:shade
mkdir results
echo "Snob simulation installed."
echp "Now run: nohup sh xp.sh > xp.log &"
echo "And: tail -f xp.log"