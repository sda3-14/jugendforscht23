#!/bin/bash -v
./RunAll.sh $1 500kbit
./RunAll.sh $1 1000kbit
./RunAll.sh $1 1500kbit 
./RunAll.sh $1 2000kbit
