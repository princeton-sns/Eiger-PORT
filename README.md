# Eiger-PORT
## 1. Background
Eiger-PORT implements performance-optimal read-only transactions on top of Eiger. Being performance-optimal, read-only transactions require only one-round of messsages between the client and servers, are non-blocking, e.g., no distributed locks for concurrency control, and each transaction request only requires a constant amount of metadata for processing. The base system Eiger (NSDI '13) has sub-optimal read-only transactions and write-only transactions. Eiger-PORT makes its read-only transactions performance-optimal without adding overhead to write transactions. For details, please see our paper published in OSDI '20.

## 2. System Requirements
We highly recommend re-creating the experimental environment close to the settings explained in the paper, which include the following specifications:

2.1 Machine type: Emulab d710 nodes (one 2.4 GHz Quad-Core Xeon CPU, 12 GB RAM, and a 1 Gbps network interface).

2.2 Operating System: Ubuntu 16.04 STD

## 3. Build
3.1 Clone this repository onto a machine. We suggest using an empty directory. The scripts in the package will work seamlessly if the repo is cloned into "/local".

    xxx:/local$ git clone https://github.com/princeton-sns/Eiger-PORT.git    
   
   The above command will clone this repo into /local with a new repo directory named "Eiger-PORT".

3.2 Install dependencies by running 

    xxx:/local/Eiger-PORT$ ./install-dependencies.bash
    
Note: if the repository has to be cloned to somewhere different from /local, then please see 6.1 below.

3.3 Build the source code. There are two pieces of source code in the repo. One is in the Eiger-PORT/Eiger-PORT subdirectory, which is the source code for Eiger-PORT; the other is in the Eiger-PORT/eiger subdirectory, which is the source code for the baseline system Eiger. We need to build both.
   
   Build Eiger-PORT: 
   
    xxx:/local/Eiger-PORT/Eiger-PORT$ ant     (build system code)
   
    xxx:/local/Eiger-PORT/Eiger-PORT/tools/stress$ ant   (build the stress tool for load generating)
   
   Build eiger:      
   
    xxx:/local/Eiger-PORT/eiger$ ant     (build system code)
                     
    xxx:/local/Eiger-PORT/eiger/tools/stress$ ant   (build the stress tool for load generating)          
   
####  (Note: "ant" might encounter failures for the first time of building when it dowloands its own dependencies. Please try "ant" multiple times if that happens, and it should succeed within 3 attempts.)  
   
3.4 Build on a cluster of nodes: please repeat the steps 3.1 to 3.3 on all the machines Eiger-PORT will be run on. An easier option is to make an image that contains a built Eiger-PORT system (e.g., after done 3.3) and start all the other machines by loading the image (Cloud platforms like Emulab and Azure normally have this image creation functionality). Another option is to repeat the above steps on every single node.

## 4. Run Experiments

To run experiments, we only need to work on the control node, e.g., ip0. That is, from now on, we should not have to touch any other nodes in the clusters. 
Now let us log onto the control node for the rest of this document, e.g., via ssh ip0, assuming ip0 is the ip/alias of the control node.

#### All the scripts used to run Eiger-PORT experiments are in xxx:/local/Eiger-PORT/eval-scripts/

4.1 Set up clusters. 

Suppose we want to set up a cluster with 8 servers and 8 clients in it as described in the paper. Then, we need 16 machines for each cluster and 2 clusters totally. One cluster is the active cluster for processing transactions and the other cluster is used as a replica, which passively receives replicated writes from the active cluster. We do not need to pay much attention to the replica cluster. We also need one extra machine for being the control node. Therefore, to create an 8-server-8-client environment, we need 33 machines totally (2 clusters, 16 machines in each, and 1 control node).

Modify the following two files to setup the clusters: 

    xxx:/local/Eiger-PORT/eval-scripts/vicci_dcl_config/16_in_kodiak and xxx:/local/Eiger-PORT/eval-scripts/vicci_dcl_config/16_clients_in_kodiak. 
                    
16_in_kodiak is the file that contains all the ssh'able name of the server machines, e.g., ip address or alias. 16_clients_in_kodiak contains the naming for client machines. "16" is the number of machines in each cluster. For example, to create a 1-server-1-client environment, then we need 5 machines totally. Say their ip addresses are ip0, ip1, ..., ip4. Then we need to create two files in xxx:/local/Eiger-PORT/eval-scripts/vicci_dcl_config/. One is named 2_in_kodiak with the following content:

    num_dcs=2

    cassandra_ips=ip1

    cassandra_ips=ip2

And create the 2_clients_in_kodiak file with the following content:

    num_dcs=2

    cassandra_ips=ip3

    cassandra_ips=ip4

This setup means node ip1 and ip2 are used as servers while ip3 and ip4 are clients. ip1 and ip3 are in the active cluster; ip2 and ip4 is in the replica cluster.

This package has included the sample files for an 8-server-8-client environment (may need to update the ips/alias to match your machines).

4.2 Experiment parameters
The parameters, e.g., number of keys, value sizes, are specified in: 
    
    xxx:/local/Eiger-PORT/eval-scripts/experiments/dynamic_defaults (it has detailed comments in it)

Note: we do not have to modify those parameters before running an experiment

4.3 Run an experiment. The script file used to run experiments is:

    xxx:/local/Eiger-PORT/eval-scripts/experiments/latency_throughput.bash
    
To run a simple experiment:

    xxx:/local/Eiger-PORT/eval-scripts/experiments/latency_throughput.bash 16
    
This command will run a simple experiment: 60 seconds, single trial, 4 threads/client (specified in latency_throughput.bash), with the default parameters specified in dynamic_default. It will first run Eiger-PORT and then eiger. 

"16" is the number of machines in each cluster, corresponding to the "16" in the file name of "16_in_kodiak." By providing this argument ("16"), latency_throughput.bash will look for 16_in_kodiak and 16_clients_in_kodiak in vicci_dcl_config/ to understand the cluster setting. So for the simple example above with 2 machines in each cluster, provide latency_throughput.bash with 2 as the argument.

## 5. Understand the Results
Each experiment will be given a unique name generated via system time, e.g., "1599241223". All experiment results are stored in 

    xxx:/local/Eiger-PORT/eval-scripts/experiments/dynamic/

5.1 A summary of the throughputs is in the file:

    xxx:/local/Eiger-PORT/eval-scripts/experiments/dynamic/1599241223/combined.graph  (assuming 1599241223 is the experiment id)
    
A sample combined.graph has the following content:

    4	24823	124119	620601	79436159	4	15861	79309	396547	50758186	1.565
    
This shows the experiment was run with 4 threads on each client, Eiger-PORT has a throughput of 24823 tps and eiger has a throughput of 15861 tps. Eiger-PORT's throughput is 1.565 of that of eiger's.

5.2 Raw latency numbers are stored in each client log, e.g.,

    xxx:/local/Eiger-PORT/eval-scripts/experiments/dynamic/1599241223/trial1/cops2/client0/1000000_128_5_5_5_5_0.1_1_60+4+latency (client0's latencies for Eiger-PORT)
    xxx:/local/Eiger-PORT/eval-scripts/experiments/dynamic/1599241223/trial1/vanilla/client0/1000000_128_5_5_5_5_0.1_1_60+4+latency (client0's latencies for eiger)
    
(Note: for legacy issues, cops2 means Eiger-PORT, vanilla means eiger)

5.3 This package also provides some data processing scripts in:

    xxx:/local/Eiger-PORT/eval-scripts/data_proc_scripts/  (use them inside experiment result directory, e.g., xxx:/local/Eiger-PORT/eval-scripts/experiments/dynamic/1599241223/)
    
## 6. Other Notes
6.1 If the repository has to be cloned to a place different from /local/, e.g., /foo/, then the variable "local_dir" needs to be updated accordingly in the following 6 files:

    /foo/Eiger-PORT/install-dependencies.bash
    /foo/Eiger-PORT/eval-scripts/latency-throughput.bash
    /foo/Eiger-PORT/Eiger-PORT/kodiak_dc_launcher.bash
    /foo/Eiger-PORT/eiger/kodiak_dc_launcher.bash
    /foo/Eiger-PORT/Eiger-PORT/kodiak_cassandra_killer.bash
    /foo/Eiger-PORT/eiger/kodiak_cassandra_killer.bash
    
6.2 When the experiment is running, detailed logs are output to the screen, e.g., 

    Nodes up and normal: 
    Number of normal nodes:  X
    
X should be 16 if running an 8-server-8-client experiment.

6.3 To re-produce Figures in the paper, we need to update dynamic_defaults and latency_throughput.bash accordingly. For instance, to re-create Figure 5a, we need to uncomment  line 120 and comment out line 121 in latency_throughput.bash.  

6.4 If experimental settings are quite different from the ones specified in 2, then it could be possible for the performance to vary. The major cause is due to the configuration of Java's garbage collection. Then, it might be necessary to tweak 

    Eiger-PORT/Eiger-PORT/conf/cassandra-env.sh
    Eiger-PORT/eiger/conf/cassandra-env.sh
    
For instance, MAX_HEAP_SIZE, HEAP_NEWSIZE, etc.   

