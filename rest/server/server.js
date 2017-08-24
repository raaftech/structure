'use strict';

const R = require('ramda');
const loopback = require('loopback');
const boot = require('loopback-boot');
const path = require('path');
const pem = require('pem');
const https = require('https');
const fs = require('fs');
const BusHelper = require(path.join(__dirname, '..','common', 'libraries','busHelper'));

const app = module.exports = loopback();

const eventBusUrl = R.defaultTo('http://localhost:8080/eventbus', process.env.EVENTBUS);
app.eventBus = new BusHelper(eventBusUrl);

/**
 * Check whether we find a ssl key and certificate at a file location[0]. If they are available
 * use them to setup the https server. If we don't find them, generate a certificate and use that
 * to setup the https server.
 *
 * @returns {Promise} An object containing a `cert` and `key` if successfull, an error string otherwise.
 */
const getSSL = () => {
    return new Promise((resolve, reject) => {
        const sslDir = R.defaultTo('/opt/structure/ssl', process.env.STRUCTURE_SSLDIR);
        const sslKey = sslDir + "/" + "structure.key";
        const sslCrt = sslDir + "/" + "structure.pem";

        const fileReadable = (fileLocation) => {
            try {
                fs.accessSync(fileLocation, fs.constants.R_OK);
            } catch (ex) {
                return false;
            }
            return true;
        };

        let generateSSL = !(fileReadable(sslKey) && fileReadable(sslCrt));

        let sslObj = {};
        if (generateSSL) {
            console.log("Couldn't find %s or %s, generating certificate.", sslKey, sslCrt);
            pem.createCertificate({days: 1000, selfSigned: true}, (err, keys) => {
                if (err) {
                    reject("Error while generating self-signed certificate " +
                        "exiting.");
                }
                console.log("Generated self-signed certificate.");
                sslObj.key = keys.serviceKey;
                sslObj.cert = keys.certificate;

                resolve(sslObj);
            });
        } else {
            console.log("Using certificate at location \"" + sslCrt + "\".");
            sslObj.key = fs.readFileSync(sslKey).toString();
            sslObj.cert = fs.readFileSync(sslCrt).toString();

            resolve(sslObj);
        }
    });
};

app.start = function () {
    getSSL()
        .then(ssl => {
            const server = https.createServer(ssl, app);
            server.listen(app.get('port'), function() {
                var baseUrl = ('https://' + app.get('host') + ':' + app.get('port'));
                app.emit('started', baseUrl);

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
            return server;
        })
        .catch(ex => {
            console.log(ex);
            process.exit(1);
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
