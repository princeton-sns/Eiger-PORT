# Eiger-PORT
## 1. Background
Eiger-PORT implements performance-optimal read-only transactions on top of Eiger. Being performance-optimal, read-only transactions require only one-round of messsages between the client and servers, are non-blocking, e.g., no distributed locks for concurrency control, and each transaction request only requires constant amount of metadata for processing. The base system Eiger (NSDI '13) has sub-optimal read-only transactions and write-only transactions. Eiger-PORT makes its read-only transactions performance-optimal without adding overhead to write transactions. For details, please see our paper published in OSDI '20.

## 2. System Requirements
We highly recommend re-creating the experiment environment close to the settings explained in the paper, which include the following specifications:

2.1 Machine type: Emulab d710 nodes (one 2.4 GHz Quad-Core Xeon CPU, 12 GB RAM, and a 1 Gbps network interface).

2.2 Operating System: Ubuntu 16.04 STD

## 3. Build
3.1 Clone this repository onto a machine. We suggest using an empty directory. The scripts in the package will work seamlessly if the repo is cloned into "/local".

                     xxx:/local$ git clone https://github.com/princeton-sns/Eiger-PORT.git    
   
   The above command will clone this repo into /local with a new repo directory named "Eiger-PORT".

3.2 Install dependencies by running 

                     xxx:/local/Eiger-PORT$ ./install-dependencies.bash

3.3 Build the source code. There are two pieces of source code in the repo. One is in the Eiger-PORT/Eiger-PORT subdirectory, which is the source code for Eiger-PORT; the other is in the Eiger-PORT/eiger subdirectory, which is the source code for the baseline system Eiger. We need to build both.
   
   Build Eiger-PORT: 
   
                     xxx:/local/Eiger-PORT/Eiger-PORT$ ant     (build system code)
   
                     xxx:/local/Eiger-PORT/Eiger-PORT/tools/stress$ ant   (build the stress tool for load generating)
   
   Build eiger:      
   
                     xxx:/local/Eiger-PORT/eiger$ ant     (build system code)
                     
                     xxx:/local/Eiger-PORT/eiger/tools/stress$ ant   (build the stress tool for load generating)          
   
   (Note: "ant" might encounter failures for the first time of building when it dowloands its own dependencies. Please try "ant" multiple times if that happens, and it should succeed within 3 attempts.)  
   
3.4 Build on a cluster of nodes: please repeat the steps 3.1 to 3.3 on all the machines Eiger-PORT will be run on. An easier option is to make an image that contains a built Eiger-PORT system (e.g., after done 3.3) and start all the other machines by loading the image (Cloud platforms like Emulab and Azure normally have this image creation functionality). Another option is to use scp to copy everything in the /local directory to the /local directory on the other nodes (need to install dependencies on other nodes first, e.g., step 3.2).

## 4. Run Experiments
