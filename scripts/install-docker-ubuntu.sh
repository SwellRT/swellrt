#!/bin/bash
if ! [ -x "$(command -v docker)" ]; then #checking if docker is installed or not
	echo 'Docker is not installed. Installing docker';
	# Now installing docker
	sudo apt-get update;
	sudo apt-get install linux-image-extra-$(uname -r);
    sudo apt-get install linux-image-extra-virtual;
    sudo apt-get install \
    apt-transport-https \
    ca-certificates \
    curl \
    software-properties-common;
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -;
	
	if [[ `dpkg --print-architecture` == "amd64" ]]; then
		sudo add-apt-repository \
	   	"deb [arch=amd64] https://download.docker.com/linux/ubuntu \
	    $(lsb_release -cs) \
	    stable";
	else
		sudo add-apt-repository \
	   	"deb [arch=armhf] https://download.docker.com/linux/ubuntu \
	   	$(lsb_release -cs) \
	   	stable";
	fi
	sudo apt-get update;
	sudo apt-get install docker-ce;
fi
if docker login; then 
docker pull p2pvalue/swellrt &
PID1=$!
wait $PID1
docker pull mongo &
PID2=$!
wait $PID2
docker stop swellrt      # stopping and removing previous containers to avoid port conflict.
docker stop mongo
docker rm swellrt
docker rm mongo                        
docker run -p 27017:27017 --name mongo -d mongo;
docker run -v ~/.swellrt/config:/usr/local/swellrt/config -v ~/.swellrt/log:/usr/local/swellrt/log -v ~/.swellrt/sessions:/usr/local/swellrt/sessions -v ~/.swellrt/avatars:/usr/local/swellrt/avatars -v ~/.swellrt/attachments:/usr/local/swellrt/attachments -p 9898:9898 -h swellrt -d p2pvalue/swellrt
if docker run -e MONGODB_HOST=mongo -e MONGODB_PORT=27017 -e MONGODB_DB=swellrt  -p 9898:9898 -h swellrt --name swellrt --link mongo:mongo -d p2pvalue/swellrt; then
echo "Swellrt setup successful. checkout http://localhost:9898";
fi
echo "The docker image folders to be placed on the host machine are stored at ~/.swellrt";
fi
