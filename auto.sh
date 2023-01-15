#!/bin/bash

./test.sh $1 0%
echo $(python avg.py)
./test.sh $1 2.5%
echo $(python avg.py)
./test.sh $1 5%
echo $(python avg.py)
./test.sh $1 7.5%
echo $(python avg.py)
./test.sh $1 10%
echo $(python avg.py)
./test.sh $1 12.5%
echo $(python avg.py)
./test.sh $1 15%
echo $(python avg.py)
./test.sh $1 17.5%
echo $(python avg.py)
./test.sh $1 20%
echo $(python avg.py)