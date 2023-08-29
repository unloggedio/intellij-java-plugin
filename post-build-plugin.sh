pwd
ls -lah
cd build/distributions
unzip plugin-${PLUGIN_VERSION}.zip

cd plugin/lib
rm jna-5.5.0.jar jna-platform-5.5.0.jar

cd ../..

rm plugin-${PLUGIN_VERSION}.zip
zip -r -Z deflate plugin-${PLUGIN_VERSION}.zip plugin/*

rm -rf plugin

cd ../..
ls -lah
ls -lah build
ls -lah build/distributions

