#!/bin/bash

num_clients=$2
exptid=$1
#latency_type=$2
path_prefix=/users/khiem/ro6-nsdi16/experiments/dynamic_threads_per_client/$1/trial1/cops2
#path_prefix=/Users/khiem/results/tworounds/eiger/$1/trial1/cops2
for tpc in 1 2 4 8 16 32 64 128 256 512 1024; do 
#for tpc in 32; do 
	for i in $(seq 0 $((num_clients-1))); do
		echo $tpc $i $(grep "num of 2nd round read txns: " $path_prefix/client$i/*_$tpc\+$tpc\+data.stderr | awk -F"2nd round read txns: " '{print $2}' | sort -n | tail -n 1) $(grep "num of read txns: " $path_prefix/client$i/*_$tpc\+$tpc\+data.stderr | awk -F"num of read txns: " '{print $2}' | sort -n | tail -n 1) \
		$(grep "num of 2nd round read ops: " $path_prefix/client$i/*_$tpc\+$tpc\+data.stderr | awk -F"2nd round read ops: " '{print $2}' | sort -n | tail -n 1) $(grep "num of read ops: " $path_prefix/client$i/*_$tpc\+$tpc\+data.stderr | awk -F"num of read ops: " '{print $2}' | sort -n | tail -n 1) \
		$(grep "num of 2nd round read keys: " $path_prefix/client$i/*_$tpc\+$tpc\+data.stderr | awk -F"2nd round read keys: " '{print $2}' | sort -n | tail -n 1) $(grep "num of read keys: " $path_prefix/client$i/*_$tpc\+$tpc\+data.stderr | awk -F"num of read keys: " '{print $2}' | sort -n | tail -n 1)
		echo -n
		#break
		#echo $tpc $(grep -r "$latency_type" $path_prefix/client$i/*\+$tpc+latency | awk -F": " '{print $2}' | sed 's/50\=\|90\=\|95\=\|99\=\|99.9\=\|\,/\t/g') | awk -v OFS="\t" -F" " '{print $1, $2, $3, $4, $5, $6}'
	done
#break
done
