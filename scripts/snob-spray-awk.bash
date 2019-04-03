#!/usr/bin/env bash

awk -F ',' '{
    if (($4 == "1000.0") && ($10 == "1")) {
        print
    }
}' $@ > $(dirname $1)/global.csv