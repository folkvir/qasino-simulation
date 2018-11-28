#!/usr/bin/env bash
FILES=( "$@" )

DATAS=()
d=0

readfile() {
    FILE=$1
    DATA=()
    i=0
    while IFS='' read -r line || [[ -n "$line" ]]; do
        DATA[i]=$line
        i=$i+1
    done < "$FILE"
#    for row in "${DATA[@]}"; do
#        echo $row
#    done
    DATAS[d]=$DATA
    d=$d+1
}

for file in "${FILES[@]}"; do
    echo "Parsing..." $file
    readfile $file
done

for datas in "${DATAS[@]}"; do
    for d in "${datas[@]}"; do
        echo $d
    done
done