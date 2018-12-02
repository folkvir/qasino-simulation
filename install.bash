#!/bin/bash
mvn clean package shade:shade
mkdir results
echo "Snob simulation installed."
echo "Now run: nohup bash xp.bash &"
echo "And: tail -f xp.log"