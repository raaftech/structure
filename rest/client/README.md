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
http://[hostname]:3000/api/[resource]/[endpoint]
```

An overview of the api can be found at:

```
http://[hostname]:3000/explorer
```

The default server port, 3000, can be changed by changing the service configuration
in `server/config.json`

## Environment

### EVENTBUS

defaults to: `http://localhost:8080/eventbus`
