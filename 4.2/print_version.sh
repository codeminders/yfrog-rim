#!/bin/bash

res="codeminders/yfrog/res/YFrog.rrc"

ver=`cat $res | grep "APP_VERSION#0=\"" | cut -d \" -f 2`
build=`cat $res | grep "APP_VERSION_BUILD#0=\"" | cut -d \" -f 2`
echo "$ver Build $build"
