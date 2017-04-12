module.exports = function (app) {
    if (app.dataSources.sqlite3 !== undefined) {
        app.dataSources.sqlite3.isActual(function (err, actual) {
            if (!actual) {
                app.dataSources.sqlite3.autoupdate();
            }
        });
    }
};
