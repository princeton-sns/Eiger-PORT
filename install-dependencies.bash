local_dir="/local"  # if cloned to /foo, then change this to "/foo"

echo "The Eiger-PORT repository has been cloned to ${local_dir}, and you are currently at ${local_dir}/Eiger-PORT/"
echo "If this does not look right, please update local_dir accordingly."
sleep 3

username="$USER"
echo "username is ${username}"
sleep 3

echo "Adding a new group: cops ..."
sudo groupadd cops
echo "Done."
sleep 3

echo "Adding ${username} to group cops ..."
sudo adduser ${username} cops
echo "Done."
sleep 3

echo "Updating ownership of ${local_dir} to ${username}:cops"
sudo chown -R ${username}:cops $local_dir
echo "Done."
sleep 3

echo "Start installing dependencies ... "

sudo apt-get update

echo "Installing Java ..."
sudo apt-get -y install openjdk-8-jdk 
echo "Done."
sleep 3

echo "Installing ant ..."
sudo apt-get -y install ant
echo "Done."
sleep 3

echo "Installing maven ..."
sudo apt-get -y install maven
echo "Done."
sleep 3

echo "Updating path in build/build-dependencies.xml ..."
sed -i 's+/users/hl0325+'$HOME'+g' ${local_dir}/Eiger-PORT/Eiger-PORT/build/build-dependencies.xml
sed -i 's+/users/hl0325+'$HOME'+g' ${local_dir}/Eiger-PORT/eiger/build/build-dependencies.xml
echo "Done."
sleep 3

echo "Installing cobertura-1.9.4.1.jar ..."
mvn install:install-file -DgroupId=net.sourceforge.cobertura -DartifactId=cobertura -Dversion=1.9.4.1 -Dpackaging=jar -Dfile=${local_dir}/Eiger-PORT/Eiger-PORT/build/lib/jars/cobertura-1.9.4.1.jar
echo "Done."
sleep 3

echo "Dependencies are all installed."
