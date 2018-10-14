# Backend notes

The backend is essentially PostgreSQL 10+ and PostgREST 5+. To make life easy, You can run the backend using `docker stack deploy`.

## Make sure you have an orchestrator

Docker by default uses Swarm to do its orchestration business. If you want to do that, locally, for example when using Docker for Desktop, you need to run `docker swarm init` at least once. After you've done so, `docker stack` will automatically use Swarm to do orchestration.

Note: You can also use Kubernetes as an orchestrator. Not covered here.

## Running the backend

The backend.yaml file is configured to require two environment variables:

* APP_NAME (a name for the database backend default user)
* APP_PASS (a password for the aforementioned user)

You can run the backend as follows:

`APP_NAME=postgres APP_PASS=secret docker stack deploy -c backend.yaml structure`

This will create a PostgreSQL, PostgREST and Swagger service. The services are running on ports:

* 3000: PostgREST service, accessible using HTTP
* 5432: PostgeSQL service, accessible using PSQL
* 8080: SwaggerUI service, accessible using HTTP
