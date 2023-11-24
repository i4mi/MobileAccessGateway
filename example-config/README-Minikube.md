# Using Minikube for testing deployments

Test your deployments with "Minikube" on your own machine. Instructions are for Ubuntu

## Install Docker 
````
sudo snap install docker
````

## Install Minikube 
````
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
sudo usermod -aG docker $USER && newgrp docker
minikube start --driver=docker
minikube kubectl -- get pods -A
````

## Operating Minikube
Restart Minikube:
````
minikube start
````

Open Dashboard in Browser:
````
minikube dashboard
````

Get rid of Minikube
````
minikube delete
````

## Using Docker Image without uploading to registry
Execute in terminal window that you use for building the image:
````
eval $(minikube docker-env)
mvn clean package
docker build --tag gateway .
````

In the kubernetes-config.yml change the "**image**" location and tell Minikube to use the local image by adding imagePullPolicy. This will only work if
you used `eval $(minikube docker-env)` before building the image in the last step.
````
  ...
  image: gateway:latest
  imagePullPolicy: Never
  ...  
````

## kubectl commands
Instead of `kubectl <args>` always use `minikube kubectl -- <args>`
````
minikube kubectl -- create configmap  mobile-access-gateway-configmap --from-file=application.yml=application.yml
minikube kubectl -- create secret generic mobile-access-gateway-secret --from-file=client.jks=client-certificate.jks --from-file=server.p12=server-certificate.jks --from-file=idp.jks=client-certificate.jks
minikube kubectl -- apply -f kubernetes-config.yml
````

## Using the Mobile Access Gateway service locally (in your browser etc...)
````
minikube service mobile-access-gateway-service
````
This will open your default browser with the correct URLs.
