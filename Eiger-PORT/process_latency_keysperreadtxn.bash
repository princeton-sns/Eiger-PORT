#!/bin/bash

num_clients=$3
exptid=$1
latency_type=$2
path_prefix=/users/khiem/ro6-nsdi16/experiments/dynamic_keys_per_read_transaction/$1/trial1/cops2

#for tpc in 1 2 4 8 16 32 64 128 256 512 1024; do 
for kpr in 5 10 20 40 80; do 
    	echo "keys_per_read = $kpr"
	for i in $(seq 0 $((num_clients-1))); do
		echo "Client $i" $(grep -r "$latency_type" $path_prefix/client$i/*\+$kpr+latency | awk -F": " '{print $2}' | sed 's/50\=\|90\=\|95\=\|99\=\|99.9\=\|\,/\t/g') | awk -v OFS="\t" -F" " '{print $1, $2, $3, $4, $5, $6, $7}'
	done
	echo ""
done	
