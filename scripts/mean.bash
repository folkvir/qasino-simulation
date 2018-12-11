#!/usr/bin/env bash
## USE THIS FILE: bash mean.bash ../results/100samples/p*

FILES=( "$@" )

mean() {
    echo $(LC_NUMERIC=en_US.UTF-8 awk -F ',' --bignum "{ total += \$$1 } END { print total/NR }" $2)
}
DIRECTORY=$(dirname $1)
OUTPUT=$DIRECTORY"/global-mean.csv"

ALL="${DIRECTORY}/*-mean.csv"
echo "Removing previous average files... ($ALL)"
rm -rf $ALL

echo "Writing results in: " $OUTPUT

for file in "${FILES[@]}"; do
    echo "Parsing..." $file
    NF=`awk -F ',' 'END { print NF }' $file`
    RES=""
    for i in $(seq 1 $NF); do
        MEAN=$(mean $i $file)
        # echo "Mean: ${i}: ".$MEAN
        RES=$RES$MEAN", "
    done
    Q=$(basename $file | awk -F'-' '{print $2}')
    SON=$(basename $file | awk -F'-' '{print $3}')
    REP=$(basename $file | awk -F'-' '{print $4}')
    TRAFFIC=$(basename $file | awk -F'-' '{print $5}')

    RES=$RES$Q", "$SON", "$REP", "$TRAFFIC

    OUTPUTQUERY=$(dirname $1)"/"$Q$SON$TRAFFIC"-mean.csv"
    OUTPUTQUERY2=$(dirname $1)"/"$SON$TRAFFIC"-mean.csv"
    echo $RES >> "${OUTPUTQUERY2}"
    echo $RES >> "${OUTPUTQUERY}"
    echo $RES >> "${OUTPUT}"
done

echo "Sorting file on the number of replicated queries..."
sort -nk2 "${OUTPUT}" > "${OUTPUT}-tmp"; mv "${OUTPUT}-tmp" "${OUTPUT}"
for file in $DIRECTORY/*-mean.csv; do
    echo "Sorting file: ${file}..."
    sort -nk2 "${file}" > "${file}-tmp"; mv "${file}-tmp" "${file}"
done