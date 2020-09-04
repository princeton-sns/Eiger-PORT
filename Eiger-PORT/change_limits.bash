#!/bin/bash
#
# Launches multiple cassandra instances on KODIAK
#

set -u

if [ $# -ne 1 ]; then
    echo "Usage: "$0" [vicci_dcl_config_file]"
    exit
fi


#clean up all nodes in parallel
echo "------ Change fd soft limit ------"
for i in $(seq 0 $1); do
    ip=h$i-dib
    (ssh -t -t -o StrictHostKeyChecking=no $ip \
sudo sh -c 'echo "* soft nofile 100000" | sudo tee -a /etc/security/limits.conf && echo "* soft nofile 100000" | sudo tee -a /etc/security/limits.conf' 2>&1 | awk '{ print "'$ip': "$0 }' ) 
done

echo "------ Change fd hard limit ------"
#for ip in ${ips[@]}; do
for i in $(seq 0 $1); do
    ip=h$i-dib
    (ssh -t -t -o StrictHostKeyChecking=no $ip \
sudo sh -c 'echo "* hard nofile 100000" | sudo tee -a /etc/security/limits.conf && echo "* hard nofile 100000" | sudo tee -a /etc/security/limits.conf' 2>&1 | awk '{ print "'$ip': "$0 }' ) 
done


