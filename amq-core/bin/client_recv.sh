#!/bin/sh
clear

mvn -DskipTests=true -f pom.xml -P prod
mvn exec:java -Dexec.mainClass="com.artfii.amq.tester.TestRecv1"

# kill
# ps -ef | grep TestRecv1 | grep -v grep | awk '{print $2}' | xargs kill -9

