#!/usr/bin/env bash

cd `dirname $0` # make sure we're in the same directory as this script

find . -iname pom.xml | while read l ; do mvn -f $l -DskipTests clean install ; done

mvn -f flow-service/pom.xml -DskipTests -Pnative native:compile && ./flow-service/target/flow-service
