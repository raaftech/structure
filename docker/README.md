# Backend notes

The backend is essentially PostgreSQL 10+ and PostgREST 5+. To make life easy, You can run the backend using `docker stack deploy`.

## Make sure you have an orchestrator

Docker by default uses Swarm to do its orchestration business. If you want to do that, locally, for example when using Docker for Desktop, you need to run `docker swarm init` at least once. After you've done so, `docker stack` will automatically use Swarm to do orchestration.

Note: You can also use Kubernetes as an orchestrator. Not covered here.

## Running the backend

The backend.yaml file is configured to accept a `PGPASSWORD` variable which is used to initialize the database user (structure)

You can run the backend (ephemerally, throw-away afterwards) as follows:

* Create a random password:
  - `export PGPASSWORD=$(cat /dev/urandom | LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 16 | head -n 1)`
* Create the docker stack:
  - `docker stack deploy -c backend.yaml --prune structure`
* Create the anonymous role:
  - `psql -U structure -h localhost -c 'CREATE USER anonymous'`
* Unset PGPASSWORD variable:
  - `unset PGPASSWORD`

Or as a one-liner:

* `export PGPASSWORD=$(cat /dev/urandom | LC_CTYPE=C tr -dc 'a-zA-Z0-9' | fold -w 16 | head -n 1) && docker stack deploy -c backend.yaml --prune structure && sleep 4 && psql -U structure -h localhost -c 'CREATE USER anonymous' && unset PGPASSWORD`

This will create a PostgreSQL, PostgREST and Swagger service. The services are running on ports:

* 3000: PostgREST service, accessible using HTTP
* 5432: PostgreSQL service, accessible using PSQL
* 8080: SwaggerUI service, accessible using HTTP

## Cleaning up

You can clean up all docker-related artifacts as follows:

* `docker stack rm structure && docker container prune -f && docker volume prune -f`

Of course, be careful with the above command. If you have dangling volumes that you care about (why), don't run the above without thinking.
