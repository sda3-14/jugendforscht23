#!/bin/bash -v
# usage: RunTestSingle.sh PROTOCOL LOSS BANDWIDTH

tc qdisc del dev ifb0 root

tc qdisc add dev ifb0 root handle 1: netem rate ${3}kbit delay 15ms 5ms loss random ${2}%

java @args jugendforscht23.server.$1Server &
PID2=$!
sleep 1
rm out2.log
java @args jugendforscht23.client.$1Client > out2.log &
PID1=$!
sleep 300
kill $PID1
kill $PID2
sleep 1

tc qdisc del dev ifb0 root

echo $1 ${2} ${3}kbit $(python avg.py) >> RESULTS.md
