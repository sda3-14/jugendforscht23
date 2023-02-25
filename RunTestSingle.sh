#!/bin/bash -v
# usage: RunTestSingle.sh PROTOCOL LOSS BANDWIDTH
tc qdisc add dev enp7s0 root handle 1: netem loss random $2 rate $3
java @args jugendforscht23.client.$1 > out2.log &
PID=$!
sleep 120
kill $PID
tc qdisc del dev enp7s0 root