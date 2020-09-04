latency_file="latency_numbers.txt"
throughput_file="combined.graph"
num_thr_count=9

# get latency numbers, output is in latency_numbers.txt
./process_latency.bash

cat ${latency_file} | xargs -n1 > latency_nums.txt
cat latency_nums.txt | head -n ${num_thr_count} > latency_cops.txt
cat latency_nums.txt | tail -n ${num_thr_count} > latency_eiger.txt

# get input file of latency-throughput graph for gnuplot
cat ${throughput_file} | awk -F " " '{print $1, $2, $7}' > throughput.txt
paste throughput.txt latency_cops.txt latency_eiger.txt > latency_throughput.txt
