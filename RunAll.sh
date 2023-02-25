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

#echo $(python avg.py) >> "$1.res"
#./RunTestSingle.sh $1 12.5% $2
#
#echo $(python avg.py) >> "$1.res"
#./RunTestSingle.sh $1 15% $2
#
#echo $(python avg.py) >> "$1.res"
#./RunTestSingle.sh $1 17.5% $2
#
#echo $(python avg.py) >> "$1.res"
#./RunTestSingle.sh $1 20% $2
#
#echo $(python avg.py) >> "$1.res"