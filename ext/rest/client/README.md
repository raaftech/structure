## Client

This is the node webserver running:

- the REST api
- the structure frontend site

## Installing

- change to the `rest` directory
- run `npm install`

## Starting the service

```
node server/server.js
```

The server api will be reachable at:

```
https://[rest-host]:[rest-port]/api/[resource]/[endpoint]
```

An overview of the api can be found at:

```
https://[rest-host]:[rest-port]/explorer
```

## Structure

The server reads the settings of Structure, such as `rest-host` and `rest-port`
(default `localhost` and `3443`), the host and port of Structure, and the Neo4j
user and password, from `../../target/structure.json`. Structure writes this
file at each start, so start Structure first. The server uses the key and the
certificate of Structure, `structure.key` and `structure.crt`.

## Environment

### NODE_EXTRA_CA_CERTS

Structure has a self-signed certificate. To trust it, set `NODE_EXTRA_CA_CERTS`
to `structure.crt`:

```
NODE_EXTRA_CA_CERTS=../../target/structure.crt node server/server.js
```
