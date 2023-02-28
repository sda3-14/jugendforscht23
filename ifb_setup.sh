#!/bin/bash
modprobe ifb
ip link set dev ifb0 up
tc qdisc add dev enp37s0.99 ingress handle ffff:
tc filter add dev enp37s0.99 parent ffff: protocol ip u32 match u32 0 0 action mirred egress redirect dev ifb0
