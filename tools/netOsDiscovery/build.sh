#!/bin/bash

rm discoveryPackage.js
aeon-compiler -p baseModel.js -b aeon.build -o discoveryPackage.js

cd projects/structure
npm i
cd ../../library
npm i