How to build a static winexe
============================

Why?
----

Winexe has some big dependencies which you don't always want to install on
machines that need winexe. These dependencies are samba, gnutls and their
dependencies.
So we build a static winexe. This is quite a hassle, so this text documents
the steps that are needed to create that static winexe.

Requirements
------------

* docker
* access to this repo so you can download the buildscript

How?
----

I like to use docker as my building environment because it's quick, and can
be disposed of easily and cleanly when you're done.

1. Run `docker run -ti --rm centos:6 /bin/bash` (make sure not to
stop the container, as it will be removed because of `--rm`
* Get the build script from the structure repo `doc/buildwinexe.sh` and get
it on the build environment (`docker cp`, `curl`, whatever)
* Run `buildwinexe.sh` on the build machine
* Grab some coffee
* Static `winexe` is now located at `/usr/src/winexe/source/build/winexe-static`
