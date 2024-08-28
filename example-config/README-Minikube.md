# Using Minikube for testing deployments

Test your deployments with "Minikube" on your own machine. Instructions are for OSX/Ubuntu, need to adapt for Windows,
see [https://minikube.sigs.k8s.io/docs/start/](https://minikube.sigs.k8s.io/docs/start/) how to uses minkube.

## Install Docker 
````
sudo snap install docker
````

## Install Minikube 
````
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64

minikube start --driver=docker --ports=9090:30090 --ports=9091:30091

minikube kubectl -- get pods -A
````

if the ports are not done the first time you need to make minikube delete

## Operating Minikube
Restart Minikube:
````
minikube start
````

Open Dashboard in Browser:
````
minikube dashboard
````

then you can access the [dashboard](http://127.0.0.1:50188/api/v1/namespaces/kubernetes-dashboard/services/http:kubernetes-dashboard:/proxy/)

Get rid of Minikube
````
minikube delete
````

## Using Docker Image without uploading to registry (2024: not tested yet)
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
minikube kubectl -- create secret generic mobile-access-gateway-secret --from-file=client.jks=client.jks --from-file=server.p12=server.p12
minikube kubectl -- apply -f kubernetes-config.yml
````

if you want to update the certificates
minikube kubectl -- delete secret mobile-access-gateway-secret
minikube kubectl -- delete configmap  mobile-access-gateway-configmap


## Using the Mobile Access Gateway service locally (in your browser etc...)
````
minikube service mobile-access-gateway-service
````
This will open your default browser with the correct URLs.

-> use the port 9090 for localhost: http://localhost:9090/#/

## setup

---> localhost:9090 (http) ---> minikube NodePort 30090 --> port: 9090, targetPort (containerport): 9090
---> localhost:9091 (https) ---> minikube NodePort 30091 --> port: 9091, targetPort (containerport): 9091
