for thr in 2 4 8 16 32 64 128 256 512; do
	echo ${thr} >> latency-thread-base.txt
done	

for sys in cops eiger; do
	for zipf in 0 03 05 07 08 09 99 12; do
		paste latency-thread-base.txt latency-zipf${zipf}/latency_${sys}.txt > tmp.txt
		mv tmp.txt latency-thread-base.txt
	done
done

for base in 1 1 1 1 1 1 1 1 1; do
	echo ${base} >> latency-thread-1.txt
done

paste latency-thread-base.txt latency-thread-1.txt > latency_to_plot.txt
