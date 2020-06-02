#!/bin/sh
clear

mvn -DskipTests=true -f pom.xml -P prod
mvn exec:java -Dexec.mainClass="com.artfii.amq.tester.TestSend"

# kill
# ps -ef | grep TestSend | grep -v grep | awk '{print $2}' | xargs kill -9


