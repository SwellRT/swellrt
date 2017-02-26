# SwellRT setup with Docker Compose

Easily setup a complete environment of SwellRT using the provided Docker Compose resources:

- Install Docker compose following [these instructions](https://docs.docker.com/compose/install/)

- Copy this folder and rename it for your SwellRT environment. The folder's name matters.

- Edit SwellRT configuration in `swellrt/config/application.conf` to adjust the domain name, container ports and the mongodb's container name.

- Edit the `ports` section of `docker-compose.yml` accordingly to previous configuration. 

- From this folder, execute `docker-compose up -d` to run the whole environment.
