#!/bin/bash
# usage: RunTestSingle.sh PROTOCOL LOSS BANDWIDTH
tc qdisc add dev enp7s0 root handle 1: netem loss random $2 rate $3
/usr/bin/env /usr/lib/jvm/java-17-openjdk-17.0.5.0.8-1.fc37.x86_64/bin/java @args jugendforscht23.client.$1 > out2.log &
PID=$!
sleep 60
kill $PID
tc qdisc del dev enp7s0 root