# Structure REPL

## How to run

    From the directory where main.js lives do:

    npm i
    npm start

Environment variable:

(these are the defaults):

RESTHOST=localhost
RESTPORT=3000
EVENTBUS=http://localhost:8080/eventbus
NEO4JURL=http://localhost:7474
STRUCTURE_NEO4J_USERNAME=neo4j
STRUCTURE_NEO4J_PASSWORD has no default; the neo commands need it

## Notes:

- type help inside the REPL for the available commands
- the REPL supports tab-completion (even for options to a command)
- the REPL supports persistent history
  - subsequently arrow-up is a relevant key ;)
