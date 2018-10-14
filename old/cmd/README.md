Structure REPL
=========
How to run
----------
    From the directory where scli.js lives do:

    npm i
    optionally run: npm test
    npm start


Environment variable:

(these are the defaults):

RESTHOST=localhost
RESTPORT=3000
EVENTBUS=http://localhost:8080/eventbus
NEO4JURL=http://localhost:7474


## Notes:

* type help inside the REPL for the available commands
* the REPL supports tab-completion (even for options to a command)
* the REPL supports persistent history
  * subsequently arrow-up is a relevant key ;)


## Testing

make sure the following npm modules are installed globally:

* npm i tape tap-spec -g

then run:

npm test