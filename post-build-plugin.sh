!/bin/bash

export PLUGIN_VERSION=1.8.30

cd build/distributions
unzip plugin-${PLUGIN_VERSION}.zip

cd plugin/lib
rm jna-5.5.0.jar jna-platform-5.5.0.jar

cd ../..

zip -r -Z deflate plugin-${PLUGIN_VERSION}-all.zip plugin/*

rm -rf plugin

cd ../..
