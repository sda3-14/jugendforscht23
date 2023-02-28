#!/bin/bash -v

./RunTestSingle.sh $1 0% $2
echo $(python avg.py) >> "$1 $2.res"

./RunTestSingle.sh $1 1.25% $2
echo $(python avg.py) >> "$1 $2.res"

./RunTestSingle.sh $1 2.5% $2
echo $(python avg.py) >> "$1 $2.res"

./RunTestSingle.sh $1 3.75% $2
echo $(python avg.py) >> "$1 $2.res"

./RunTestSingle.sh $1 5% $2
echo $(python avg.py) >> "$1 $2.res"

for loss in $(seq 0 0.5 4)
do
	for rate in $(seq 250 250 1000)
	do
		./RunTestSingle.sh $1 ${loss} ${rate}
	done
done
