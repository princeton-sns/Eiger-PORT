local_dir="/local"  # if cloned to /foo, then change this to "/foo"
username="$USER"

sudo apt-get update

sudo apt-get -y install openjdk-8-jdk 

sudo apt-get -y install ant

sudo apt-get -y install maven

sudo chown -R $username:cops $local_dir
