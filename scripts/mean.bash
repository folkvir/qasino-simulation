#!/usr/bin/env bash
## USE THIS FILE: bash mean.bash ../results/100samples/p*

FILES=( "$@" )

mean() {
    echo $(LC_NUMERIC=en_US.UTF-8 awk -F ',' "{ total += \$$1 } END { print total/NR }" $2)
}
OUTPUT=$(dirname $1)"/mean.txt"
rm -rf "${OUTPUT}"
echo "Writing results in: " $OUTPUT

for file in "${FILES[@]}"; do
    echo "Parsing..." $file
    NF=`awk -F ',' 'END { print NF }' $file`
    RES=""
    for i in $(seq 1 $NF); do
        MEAN=$(mean $i $file)
        RES=$RES$MEAN", "
    done
    echo $RES >> "${OUTPUT}"
done

sort -nk2 "${OUTPUT}" > "${OUTPUT}-tmp"; mv "${OUTPUT}-tmp" "${OUTPUT}"

