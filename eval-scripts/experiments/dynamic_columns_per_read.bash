#!/bin/bash
#Dynamic Workload

set -u

dynamic_dir=$(echo $0 | awk -F"dynamic" '{ print "dynamic"$2 }' | sed 's/.bash//g')

if [ "$dynamic_dir" == "" ]; then
    echo "autodetect of exp name failed, set dynamic dir manually"
    exit
fi

#######################################
#
# Cluster Setup
#
#######################################

# MacroBenchmark: Big Clusters

set -x

# Initial setup
machine_name=$(uname -n)
username="$USER"

    if [ $# -ne 1 ]; then
	echo "$0: [# servers]"
	exit
    fi

    nservers=$1
    dcl_config=${nservers}_in_kodiak
    client_config=${nservers}_clients_in_kodiak

    #location specific config
    local_dir="/local/Eiger-PORT"
    cops_dir="/users/${username}/Eiger-PORT"
    vanilla_dir="/users/${username}/eiger"
    tools_dir="/users/${username}/eiger"
#    exp_dir="${cops_dir}/experiments"
    exp_dir="${local_dir}/experiments"
    stress_dir="${cops_dir}/tools/stress"
    output_dir_base="${exp_dir}/${dynamic_dir}"
    exp_uid=$(date +%s)
    output_dir="${output_dir_base}/${exp_uid}"
#    mkdir -p ${output_dir}
    sudo mkdir -p ${output_dir}
    sudo chown -R ${username}:cops ${local_dir} 

    rm $output_dir_base/latest
    ln -s $output_dir $output_dir_base/latest 



#    dcl_config_full="${cops_dir}/vicci_dcl_config/${dcl_config}"
    dcl_config_full="${local_dir}/vicci_dcl_config/${dcl_config}"

    all_servers=($(cat $dcl_config_full | grep cassandra_ips | awk -F"=" '{ print $2 }' | xargs))
    all_servers=$(echo "echo ${all_servers[@]}" | bash)
    num_dcs=$(cat $dcl_config_full | grep num_dcs | awk -F"=" '{ print $2 }')

    strategy_properties="DC0:1"
    for i in $(seq 1 $((num_dcs-1))); do
	strategy_properties=$(echo ${strategy_properties}",DC${i}:1")
    done

    num_servers=$(echo $all_servers | wc -w)
    num_servers_per_dc=$((num_servers / num_dcs))
    
    for dc in $(seq 0 $((num_dcs-1))); do
	this_dc_servers=$(echo $all_servers | sed 's/ /\n/g' | head -n $((num_servers_per_dc * (dc+1))) | tail -n $num_servers_per_dc | xargs)
	servers_by_dc[$dc]=${this_dc_servers}
    done
    echo ${servers_by_dc[@]}



#    client_config_full="${cops_dir}/vicci_dcl_config/${client_config}"
    client_config_full="${local_dir}/vicci_dcl_config/${client_config}"

    all_clients=$(cat $client_config_full | grep cassandra_ips | awk -F"=" '{ print $2 }' | xargs)
    all_clients=$(echo "echo ${all_clients[@]}" | bash)

    num_clients=$(echo $all_clients | wc -w)
    num_clients_per_dc=$((num_clients / num_dcs))
    
    for dc in $(seq 0 $((num_dcs-1))); do
	this_dc_clients=$(echo $all_clients | sed 's/ /\n/g' | head -n $((num_clients_per_dc * (dc+1))) | tail -n $num_clients_per_dc | xargs)
	clients_by_dc[$dc]=${this_dc_clients}
    done
    echo ${clients_by_dc[@]}

    kill_all_cmd="${cops_dir}/kodiak_cassandra_killer.bash ${cops_dir}/vicci_dcl_config/${dcl_config}"
    stress_killer="${cops_dir}/kill_stress_kodiak.bash"

    
    source $exp_dir/dynamic_common




#######################################
#
# Actual Experiment
#
#######################################

source ${exp_dir}/dynamic_defaults

# Test: Latency, Throughput of different operations
# Control: # of dependencies

# fixed parameters
run_time=60
trim=15

#echo -e "STARTING $0 $@" >> ${cops_dir}/experiments/progress
echo -e "STARTING $0 $@" >> ${local_dir}/experiments/progress

num_trials=1
for trial in $(seq $num_trials); do

#for thread in 2 4 8 16 32 64 128 256 512; do
#for thread in 64; do
#for thread in 32; do
#for zipfian_constant in 0 0.3 0.5 0.7 0.8 0.9 0.99 1.1 1.2; do 
for zipfian_constant in 0; do
#variable=$thread
variable=$zipfian_constant
	echo -e "Running $0\t$variable at $(date)" >> ${cops_dir}/experiments/progress

	sleep 20
	cops_cluster_start_cmd
	cops_populate_cluster ${total_keys} ${cols_per_key_read} ${value_size} ${cols_per_key_write}
	sleep 5
        cops_run_experiment $total_keys $value_size $cols_per_key_read $cols_per_key_write $keys_per_read $keys_per_write $write_frac $write_trans_frac $run_time $variable $trial $use_zipfian $zipfian_constant
	$kill_all_cmd

	sleep 20

	vanilla_cluster_start_cmd
	vanilla_populate_cluster ${total_keys} ${cols_per_key_read} ${value_size} ${cols_per_key_write}
	sleep 5
	vanilla_run_experiment $total_keys $value_size $cols_per_key_read $cols_per_key_write $keys_per_read $keys_per_write $write_frac $write_trans_frac $run_time $variable $trial $use_zipfian $zipfian_constant
	$kill_all_cmd

	sleep 5
	gather_results
#for x in node-1 node-2 node-3 node-4 node-5 node-6 node-33 node-8; do ssh -t -t -o StrictHostKeyChecking=no $x "cat /local/cassandra_var/$x/cassandra* | grep -i error"; done >> ~/error_log
    done
done

#echo -e "FINISHED $0\tat $(date)" >> {$cops_dir}/experiments/progress
echo -e "FINISHED $0\tat $(date)" >> {$local_dir}/experiments/progress

#######################################
#
# Cleanup Experiment
#
#######################################
set +x
#$kill_all_cmd
set -x

#######################################
#
# Process Output
#
#######################################
cd $exp_dir
./dynamic_postprocess_full.bash . ${output_dir} ${run_time} ${trim} shuffle



