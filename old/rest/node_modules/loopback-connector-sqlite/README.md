## loopback-connector-sqlite [![Build Status](https://travis-ci.org/Synerzip/loopback-connector-sqlite.svg)](https://travis-ci.org/Synerzip/loopback-connector-sqlite)
[**LoopBack**](http://loopback.io/) is a highly-extensible, open-source Node.js framework that enables you to create dynamic end-to-end REST APIs with little or no coding. It also enables you to access data from major relational databases, MongoDB, SOAP and REST APIs.

**loopback-connector-sqlite** is the SQLite3 connector module for [loopback-datasource-juggler](https://github.com/strongloop/loopback-datasource-juggler).

Checkout **[wiki pages](https://github.com/Synerzip/loopback-connector-sqlite/wiki)** to get updated information about this connector and how to use it with a LoopBack Application to create an API.

## Basic usage

#### Installation
Install the module using the command below in your projects root directory:
```sh
npm i loopback-connector-sqlite
```

#### Configuration
The SQLite3 database can be configured to operate in 2 ways: with a DB file name or with an anonymous in-memory DB.
This connector needs 2 configuration parameters:
* `file_name`(string): It can be a file at the root of the project itself if preceded by `./`, e.g., `./test.sqlite3`, OR, it can be an absolute file path(e.g. `/home/[user]/Desktop/test.sqlite3` or `C:\Users\[user]\Desktop\test.sqlite3`) to generate/use the SQLite file. It can also contain `null` for SQLite3 in-memory usage.
* `debug`(boolean): Used for disabling and enabling logging.

A DataSource with basic settings can be defined as shown below:
```javascript
var DataSource = require('loopback-datasource-juggler').DataSource;
var dataSource = new DataSource(require('../index'), {
  file_name: './dev.sqlite3',
  debug: false
});
```

Checkout `examples` folder to get the idea of basic usage.
Run the examples from the root directory as follows:
```sh
node examples/[example_file]
```

#### NOTE: Defining Models
The model names must not be among one of the keywords used in SQLite3. Checkout [all SQLite3 keywords here](https://sqlite.org/lang_keywords.html). It's stated there that _these **keywords** may **not be used** as the **names of tables, indices, columns, databases, user-defined functions, collations, virtual table modules, or any other named object**._

#### Dependencies
* [sqlite3](https://www.npmjs.com/package/sqlite3)
* [loopback-connector](https://www.npmjs.com/package/loopback-connector)
* [debug](https://www.npmjs.com/package/debug)
* [async](https://www.npmjs.com/package/async)

## SQLite3 configuration for tests
The `.loopbackrc` file holds the settings for the tests. It's in JSON format and has following content:
* For file based SQLite testing
```JSON
{
  "sqlite": {
    "test": {
      "file_name": "./test.sqlite3",
      "debug": false
    }
  }
}
```
* For anonymous in-memory SQLite testing
```JSON
{
  "sqlite": {
    "test": {
      "file_name": null,
      "debug": false
    }
  }
}
```
The `file_name` is the name of the sqlite3 DB file, which will be created, or, used if already present.
The `debug` value is to set debugging mode.

## Running the tests
* execute `npm install` for installing all the dependencies.
* execute `npm test` to run all the tests.
