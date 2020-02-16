#!/usr/bin/env bash

# dev 环境启动脚本，可以实现
now_dir=`pwd`
cd `dirname $0`
script_dir=`pwd`
cd ..

cp zelda-builder/build-Release.gradle zelda-builder/build.gradle

./gradlew zelda-builder:assemble

cp zelda-builder/build-Debug.gradle zelda-builder/build.gradle

if [ $? != 0 ] ;then
    echo "builder jar assemble failed"
    exit $?
fi

engineVersionCode=`cat build.gradle | grep zeldaEngineVersion | grep -v zeldaEngineVersionCode | awk '{print $3}' | awk -F "\"" '{print $2}'`
echo engineVersionCode: ${engineVersionCode}

builder_jar_dir=`pwd`/zelda-builder/build/libs/

for file in `ls ${builder_jar_dir}`
do
    if [[ ${file} =~ "ZeldaBuilder" ]] && [[ ${file} =~ ".jar" ]] &&  [[ ${file} =~ ${engineVersionCode} ]];then
        builder_jar=${builder_jar_dir}${file}
    fi
done

if [ -f ${builder_jar} ] ;then
    echo "use ${builder_jar}"
else
    echo "can not find container build jar in path:${builder_jar}"
    echo -1
fi


cd ${now_dir}

out_apk_path=`java -jar ${builder_jar} -t  $*`
if [ $? != 0 ] ;then
    echo "call builder jar failed"
    exit $?
fi
echo "assemble new apk for $*"

java -jar ${builder_jar} -s -w ~/.zelda-working  $*



if [ $? != 0 ] ;then
    echo "assemble zelda apk failed"
    exit $?
fi


echo "the final output apk file is :${out_apk_path}"

