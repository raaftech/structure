'use strict';

const R = require('ramda');
const loopback = require('loopback');
const boot = require('loopback-boot');
const path = require('path');
const BusHelper = require(path.join(__dirname, '..','common', 'libraries','busHelper'));

const app = module.exports = loopback();

const eventBusUrl = R.defaultTo('http://localhost:8080/eventbus', process.env.EVENTBUS);
app.eventBus = new BusHelper(eventBusUrl);

app.start = function () {
    // start the web server
    return app.listen(function () {
        app.emit('started');
        let baseUrl = app.get('url').replace(/\/$/, '');
        if(R.isNil(process.env.EVENTBUS)) {
            console.log('Environment variable EVENTBUS not set, using default...');
        }
        console.log('Using eventbus url: %s', eventBusUrl);
        console.log('Web server listening at: %s', baseUrl);
        if (app.get('loopback-component-explorer')) {
            let explorerPath = app.get('loopback-component-explorer').mountPath;
            console.log('Browse your REST API at %s%s', baseUrl, explorerPath);
        }
    });
};

// Bootstrap the application, configure models, datasources and middleware.
// Sub-apps like REST API are mounted via boot scripts.
boot(app, __dirname, function (err) {
    if (err) throw err;

    // start the server if `$ node server.js`
    if (require.main === module)
        app.start();
});
