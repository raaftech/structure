var test = require('tape');
var path = require('path');
var R = require('ramda');
var helper = require(path.join(__dirname, '..','helper'));

var notNil = R.compose(R.not, R.isNil);
var mkData = (options) => {
    var obj = {body: {result: {data: {}}}};
    obj.body.result.data = R.merge(obj.body.result.data, options);
    return obj;
};

test('generateQuestion tests', function(tt) {
    tt.test('generate question with options', function(t) {
        var obj = helper.generateQuestion('thename', 'input','a message',{nick: 'test',test:'kees'});
        var conditions = [R.has('name'),R.has('type'),R.has('message'),R.has('nick'),R.has('test')];
        var conditions2 = [
            R.propEq('name','thename'), R.propEq('type', 'input'),
            R.propEq('message', 'a message'), R.propEq('nick', 'test'), R.propEq('test','kees')
        ];

        t.ok(R.allPass(conditions)(obj), 'Should have all properties set');
        t.ok(R.allPass(conditions2,obj), 'Should have the correct property values');
        t.end();
    });

    tt.test('generate question withOUT options', function(t) {
        var obj = helper.generateQuestion('thename', 'input','a message');
        var conditions = [R.has('name'),R.has('type'),R.has('message')];
        var conditions2 = [R.propEq('name','thename'), R.propEq('type', 'input'), R.propEq('message', 'a message')];

        t.ok(R.allPass(conditions)(obj), 'Should have all properties set');
        t.ok(R.allPass(conditions2,obj), 'Should have the correct property values');
        t.end();
    });

    tt.test('generate question with only two args', function(t) {
        var obj = helper.generateQuestion('thename', 'input');
        var conditions = [R.has('name'),R.has('type')];
        var conditions2 = [R.propEq('name','thename'), R.propEq('type', 'input')];

        t.ok(R.allPass(conditions)(obj), 'Should have all properties set');
        t.ok(R.allPass(conditions2,obj), 'Should have the correct property values');
        t.end();
    });
});


test('createOptions tests', function(tt) {
    tt.test('createOptions happy flow', function(t) {
        var obj = helper.createOptions('myCommand', {test:'kees'});
        t.equal(obj.msgh, 'myCommand', 'should be myCommand command');
        t.ok(notNil(obj.args.test), 'argument object should contain property test');
        t.equal(obj.args.test, 'kees', 'value should be kees');
        t.end();
    });

    tt.test('createOptions no args', function(t) {
        var obj = helper.createOptions('myCommand');
        t.equal(obj.msgh, 'myCommand', 'should be myCommand command');
        t.ok(notNil(obj.args), 'should have default args object {}');
        t.ok(R.isEmpty(obj.args), 'should be default empty object.');
        t.end();
    });

    tt.test('createOptions no cmd or args', function(t) {
        var obj = helper.createOptions();
        t.equal(obj.msgh, 'help', 'should default to help command');
        t.ok(notNil(obj.args), 'should have default args object {}');
        t.ok(R.isEmpty(obj.args), 'should be default empty object.');
        t.end();
    });
});


test('getResultData tests', function(tt) {
    tt.test('getResultData happy flow', function(t) {
        var obj = mkData({test: 'data', name: 'kees'});
        var result = helper.getResultData(obj);
        t.ok(R.has('test', result), 'should have ')
        t.end();
    });

    tt.test('getResultData unexpected structure', function(t) {
        var obj = {foo:'bar'};
        var result = helper.getResultData(obj);
        t.equal(result, undefined, 'should be undefined');
        t.end();
    });
});