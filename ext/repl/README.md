# Structure REPL

## How to run

    From the directory where main.js lives do:

    npm i
    npm start

The REPL reads the settings of Structure, such as the hosts and ports
of Structure and the REST server, and the Neo4j user and password,
from ../../target/structure.json. Structure writes this file at each
start, so start Structure first.

Structure and the REST server have the self-signed certificate of
Structure. To trust it, set NODE_EXTRA_CA_CERTS to structure.crt:

    NODE_EXTRA_CA_CERTS=../../target/structure.crt npm start

## Notes:

- type help inside the REPL for the available commands
- the REPL supports tab-completion (even for options to a command)
- the REPL supports persistent history
  - subsequently arrow-up is a relevant key ;)
