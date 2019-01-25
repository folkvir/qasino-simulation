#!/usr/bin/env bash
echo "Sorting file on the number of replicated queries from the directory: " $1
for file in $1*-mean.csv; do
    echo "Sorting file: ${file}..."
    sort -t ',' -n -nk2 "${file}" > "${file}-tmp"; mv "${file}-tmp" "${file}"
done