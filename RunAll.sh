#!/bin/bash

./test.sh $1 0% 100kbit
echo $(python avg.py)
./test.sh $1 2.5% 100kbit
echo $(python avg.py)
./test.sh $1 5% 100kbit
echo $(python avg.py)
./test.sh $1 7.5% 100kbit
echo $(python avg.py)
./test.sh $1 10% 100kbit
echo $(python avg.py)
./test.sh $1 12.5% 100kbit
echo $(python avg.py)
./test.sh $1 15% 100kbit
echo $(python avg.py)
./test.sh $1 17.5% 100kbit
echo $(python avg.py)
./test.sh $1 20% 100kbit
echo $(python avg.py)