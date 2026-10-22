"use strict";

const R = require("ramda");
const util = require("util");
const path = require("path");
const clc = require("cli-color");
const helper = require(path.join(__dirname, "..", "helper"));

module.exports = function (vorpal, opts) {
  const rest = opts.rest;

  vorpal.command("structure login", "Get an access token for Structure").action(function (args, done) {
    const self = this;
    let qs = [];
    qs.push(helper.generateQuestion("email", "input", "Email: "));
    qs.push(helper.generateQuestion("password", "password", "Password: "));
    self.prompt(qs, function (answers) {
      rest
        .login(answers.email, answers.password)
        .then((user) => {
          //function that handles the error or ok case of requesting a login
          //if the property error is present, print error message else show the acquired AccessToken
          let handler = R.ifElse(
            R.has("error"),
            (error) =>
              self.log(
                util.format(
                  "%s (%s): %s",
                  clc.red.bold(error.error.code),
                  clc.red(error.error.statusCode),
                  error.error.message,
                ),
              ),
            (user) => {
              opts.memoryBank.token = user.id;
              opts.cache.set("structure", "token", user.id);
              opts.cache.save();
              self.log(clc.green("AccessToken: ") + user.id);
            },
          );
          handler(user);
          done();
        })
        .catch(helper.genericCatchHandler(done));
    });
  });

  vorpal.command("structure logout", "Invalidate an access token for Structure").action(function (args, done) {
    const self = this;

    let loggedIn = () => {
      rest
        .logout()
        .then((result) => {
          self.log(result);
          rest.setToken("");
          opts.memoryBank.token = "";
          opts.cache.set("structure", "token", "");
          opts.cache.save();
          done();
        })
        .catch(helper.genericCatchHandler(done));
    };

    helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
  });

  vorpal.command("structure bootstrap", "Initial structure bootstrap").action(function (args, done) {
    const self = this;

    let loggedIn = () => {
      rest
        .bootstrap()
        .then((result) => {
          if (R.has("error")) self.log(result);
          done();
        })
        .catch(helper.genericCatchHandler(done));
    };

    helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
  });

  vorpal.command("structure adduser", "Add a structure user").action(function (args, done) {
    const self = this;
    let qs = [];
    qs.push(helper.generateQuestion("email", "input", "Email: "));
    qs.push(helper.generateQuestion("password", "password", "Password: "));
    qs.push(helper.generateQuestion("confirm", "password", "Re-enter password: "));
    self.prompt(qs, function (answers) {
      //Function to determine the outcome of the createUser request
      let createUserHandler = R.ifElse(
        R.has("error"),
        (error) =>
          self.log(util.format("status code: %s\n%s", clc.red.bold(error.error.statusCode), error.error.message)),
        (user) =>
          self.log(
            util.format('%s "%s" (userid: %s) %s', clc.green("User"), user.email, user.id, clc.green("created")),
          ),
      );
      //Call to the createUser endpoint in the rest API
      let createUser = () =>
        rest
          .createUser(answers.email, answers.password)
          .then((result) => {
            createUserHandler(result);
            done();
          })
          .catch(helper.genericCatchHandler(done));

      //Function to check if the 1st and 2nd password entry are equal.
      //if not equal prints an error message
      //if equal calls the above createUser function.
      let pwdCheck = R.ifElse(R.equals, createUser, () => {
        self.log(clc.red("Passwords to not match"));
        done();
      });
      pwdCheck(answers.password, answers.confirm);
    });
  });

  vorpal.command("structure status", "Show structure status information").action(function (args, done) {
    const self = this;
    //Functions for usage percentage calculation
    let div = R.flip(R.divide);
    let calcUsedPercentage = (used, free) => R.pipe(R.add, div(used), R.divide(1000), Math.round, div(10))(used, free);
    let printPercentage = R.ifElse(
      R.lt(80),
      (p) => util.format("(%s%)", clc.red.bold(p)),
      (p) => util.format("(%s%)", clc.red.green(p)),
    );
    let percentageFn = R.pipe(calcUsedPercentage, printPercentage);

    rest
      .getStructureStatusInfo()
      .then((result) => {
        self.log(clc.green.bold("[JVM info]"));
        self.log(util.format("  Version: %s", result.jvm.version));
        self.log(util.format("  Java version: %s", result.jvm.java_version));
        self.log(util.format("  Cpus: %s", result.jvm.available_processors));
        self.log(util.format(clc.green("  [Memory]")));
        self.log(
          util.format(
            "    Used: %s %s\n    Free: %s",
            result.jvm.memory.used,
            percentageFn(result.jvm.memory.used, result.jvm.memory.free),
            result.jvm.memory.free,
          ),
        );

        self.log(util.format(clc.green.bold("[Structure instance]")));
        self.log(util.format("  Host: %s", result.structure.host));
        if (result.structure.bootstrapped)
          self.log(util.format("  Bootstrapped: %s", clc.green(result.structure.bootstrapped)));
        else self.log(util.format("  Bootstrapped: %s", clc.red(result.structure.bootstrapped)));

        self.log(util.format(clc.green.bold("[Graph info]")));
        self.log(util.format("  Types: %s", result.graph.types));
        self.log(util.format("  Components: %s", result.graph.components));
        self.log(util.format("  Instruments: %s", result.graph.instruments));
        self.log(util.format("  Groupings: %s", result.graph.groupings));
        self.log(util.format("  Results: %s", result.graph.results));
        done();
      })
      .catch(helper.genericCatchHandler(done));
  });

  vorpal.command("structure init", "Purge-Init structure server").action(function (args, done) {
    let loggedIn = () => {
      rest
        .purgeInit()
        .then((result) => {
          console.log(result);
          done();
        })
        .catch(helper.genericCatchHandler(done));
    };

    helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
  });

  vorpal.command("structure reload", "Reload types on the structure server").action(function (args, done) {
    const self = this;

    let loggedIn = () => {
      rest
        .reloadTypes()
        .then((result) => {
          self.log(result);
          done();
        })
        .catch(helper.genericCatchHandler(done));
    };

    helper.genericAuthCommandHandler(done, loggedIn, rest.getToken());
  });
};
