# Using Docker for testing deployments

Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) on your machine.

For Windows and macOS, it is an installer to download. For Linux, 
[documentation is given per distribution](https://docs.docker.com/desktop/install/linux-install/).

## Using a Docker image locally

In the MobileAccessGateway directory, run:
```bash
cd /path/to/MobileAccessGateway
docker buildx build --tag "mag:local1" --load -f Dockerfile .
```

_mag:local1_ is the name of the image, it can be changed to whatever you want.

The _--load_ option is used to load the image into the local Docker daemon.

Then, run the image:
```bash
docker container run -p 127.0.0.1:9090:9090/tcp -p 127.0.0.1:9091:9091/tcp \
 -v /path/to/MobileAccessGateway/example-config:/config                    \
 -v /path/to/MobileAccessGateway/example-config:/secret                    \
 mag:local1
```

This will run the image, expose the ports 9090-9091 and mount the _example-config_ directory as /config and /secret 
in the container.

This can also be done directly in Docker Desktop.