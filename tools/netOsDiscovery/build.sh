#!/bin/bash

rm discoveryPackage.js
aeon-compiler -p baseModel.js -b aeon.build -o discoveryPackage.js
