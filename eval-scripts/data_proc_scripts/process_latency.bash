# For calculating latency for each expt
# Latency is the median of the medians of all trials of the medias of all clients

#---- global constant ----
trials=5
clients=8
threads="2 4 8 16 32 64 128 256 512"
p50=4; p75=5; p90=6
#-------------------------

dir=$(pwd)
med_client=$(($clients / 2))
med_trial=$(($trials / 2))
for p in $p50; do
#	echo "======== $p ========="
	for system in cops2 vanilla; do
#		echo "----$system----"
		latency=""
		for thr in $threads; do
			sorted_trials=( $(
			for trial in $(echo trial*); do
				sorted_clis=( $(
					for c in $(echo $trial/$system/client*); do
  						a=$(cat $c/*+$thr+latency | grep Read | awk -v i=$p '{print $i}' | cut -d '=' -f2 | cut -d ',' -f1)
						echo "$a"
					done | sort -n)
				)
				#echo ${sorted_clis[*]}
				client_med=${sorted_clis[$med_client]}
				echo $client_med
			done | sort -n)
			)
			trial_med=${sorted_trials[$med_trial]}
			latency="$latency $trial_med"
  		done
		echo "$latency" >> latency_numbers.txt
  	done
#	echo "====================="
done
