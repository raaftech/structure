'use strict';

const EventBus = require('vertx3-eventbus-client');
const R = require('ramda');

const eventBusUrl = R.defaultTo('http://localhost:8080/eventbus', process.env.EVENTBUS);
const eb = new EventBus(eventBusUrl);

eb.onopen = () => console.log('Connected to: %s', eventBusUrl);
eb.onclose = (error) => console.log('Eventbus connection closed: %s', stringify(error));


module.exports = function(Task) {
  Task.queueTask = function(isSequential, jobs, callback) {
      let args = {
          taskId: 'TODO-generate-uuid',
          sequential: isSequential,
          jobs: jobs
      };
      sendEvent(getEventBusAddress(['manager','taskmanager']), 'queueTask', {})
        .then((result) => callback(null, getResultData(result)))
        .catch((ex) => callback(ex));
  };

  Task.remoteMethod('queueTask', {
    accepts: [{arg: 'isSequential', type: 'boolean', required: true},
              {arg: 'jobs', type: 'array', required: true}],
    returns: {arg: 'result', type: 'object'},
    http: {verb: 'post', path: '/queue'},
    description: 'queue a task'
  });

  Task.queueStatus = function(callback) {
      sendEvent(getEventBusAddress(['manager','taskmanager']), 'queueStatus', {})
          .then((result) => callback(null, getResultData(result)))
          .catch((ex) => callback(ex));
  };

  Task.remoteMethod('queueStatus', {
      accepts: [],
      returns: {arg: 'result', type: 'object'},
      http: {verb: 'get', path: '/queue/status'},
      description: 'get the tasks on the queue'
  });

  Task.cancelJob = function(taskId, jobId, callback) {
      sendEvent(getEventBusAddress(['manager','taskmanager']), 'cancelJob',
                                   {taskId: taskId, jobId: jobId})
          .then((result) => callback(null, getResultData(result)))
          .catch((ex) => callback(ex));
  };

  Task.remoteMethod('cancelJob', {
      accepts: [{arg: 'taskId', type: 'string', required: true},
                {arg: 'jobId', type: 'integer', required: true}],
      returns: {arg: 'result', type: 'object'},
      http: {verb: 'put', path: '/:taskId/cancel/:jobId'},
      description: 'cancel a job for a given task'
  });

  Task.cancelTask = function(taskId, callback) {
      sendEvent(getEventBusAddress(['manager','taskmanager']), 'cancelTask', {taskId: taskId})
          .then((result) => callback(null, getResultData(result)))
          .catch((ex) => callback(ex));
  };

  Task.remoteMethod('cancelTask', {
      accepts: [{arg: 'taskId', type: 'string', required: true}],
      returns: {arg: 'result', type: 'object'},
      http: {verb: 'delete', path: '/:taskId'},
      description: 'cancel a task'
  });

  Task.pauseTask = function(taskId, callback) {
      sendEvent(getEventBusAddress(['manager','taskmanager']), 'toggleTaskPause', {taskId: taskId})
          .then((result) => callback(null, getResultData(result)))
          .catch((ex) => callback(ex));
  };

  Task.remoteMethod('pauseTask', {
      accepts: [{arg: 'taskId', type: 'string', required: true}],
      returns: {arg: 'result', type: 'object'},
      http: {verb: 'put', path: '/pause/:taskId'},
      description: 'pause a task'
  });

  Task.liveStatus = function(callback) {
      return new Promise((resolve, reject) => callback(null, 'not implemented yet'));
  };

  Task.remoteMethod('liveStatus', {
      accepts: [],
      returns: {arg: 'result', type: 'object'},
      http: {verb: 'get', path: '/live'},
      description: 'cancel a task'
  });

  Task.discover = function(uuid, callback) {
      sendEvent(getEventBusAddress(['node','component',uuid]), 'runInstrument', {
          instrument: 'IpAddressDiscover',
          timeAllocated: 600000
      })
          .then((result) => callback(null, getResultData(result)))
          .catch((ex) => callback(ex));
  };

    Task.remoteMethod('discover', {
        accepts: [{arg: 'uuid', type: 'string', required: true}],
        returns: {arg: 'result', type: 'object'},
        http: {verb: 'get', path: '/discover/:uuid'},
        description: 'Perform a discovery'
    });


};


function sendEvent(base, cmd, args) {
	return new Promise((resolve, reject) => {
		eb.send(base, createOptions(cmd, args), (error, result) => {
			if(error) reject(error);
			else resolve(result);
		});
	});
}

function createOptions(cmd, args) {
	return R.zipObj(['msgh','args'],[R.defaultTo('help',cmd), R.defaultTo({}, args)]);
}

function getEventBusAddress(parts) {
    return R.join('.',R.prepend('com.raaftech.structure', parts));
}

function getResultData(result) {
	return R.path(['body','result','data'])(result);
}

function stringify(json) {
	return JSON.stringify(json, undefined, 2);
}
