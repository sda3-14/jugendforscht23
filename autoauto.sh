#!/bin/bash

echo "TCP:"
#./auto.sh TCPClient
echo ""
read -p "Kcp"

echo "KCP:"
./auto.sh KCPClient
echo ""
read -p "Unet"

echo "Unet:"
./auto.sh UnetClient
echo ""
read -p "Steam"

echo "Steamp2P:"
./auto.sh SteamP2PClient
echo ""