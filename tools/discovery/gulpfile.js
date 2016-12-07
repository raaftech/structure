"use strict";

const gulp = require('gulp');
const jshint = require('gulp-jshint');
const gutil = require('gulp-util');
const exec = require('child_process').exec;
const run = require('gulp-run');

gulp.task('default', ['watch']);

gulp.task('jshint', () => {
	return gulp.src(['*.js', 'tests/*.js'])
			 .pipe(jshint())
			 .pipe(jshint.reporter('jshint-stylish'));
});

gulp.task('watch', () => {
	gulp.watch(['*.js','tests/*.js'], ['jshint']);
});

gulp.task('raaf', () => {
	run('node combi.js -c 172.17.1.0/24').exec()
	.on('data', (data) => gutil.log(data));
});

gulp.task('test', ['nmapTest', 'ansibleTest']);


gulp.task('nmapTest', (cb) => {
	exec('tape tests/nmapTests.js | tspec', (err, stdout, stderr) => {
		gutil.log(stdout);
		gutil.log(stderr);
		cb(err);
	});
});

gulp.task('ansibleTest', (cb) => {
	exec('tape tests/ansibleLibTest.js | tspec', (err, stdout, stderr) => {
		gutil.log(stdout);
		gutil.log(stderr);
		cb(err);
	});
});
