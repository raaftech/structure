#!/bin/bash

echo test {{ test }} $@
for i in "$@"
do
	echo $i
done
