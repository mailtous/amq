#!/bin/sh
clear

mvn clean compile -DskipTests=true -f pom.xml -P prod
#mvn exec:exec -Dexec.executable="java" -Dexec.args="-DsystemProperty1=value1 -DsystemProperty2=value2 -XX:MaxPermSize=256m -classpath %classpath com.artfii.amq.core.AioMqServer"
#mvn exec:exec
#cd target/classes/com/artlongs/amq/core
mvn exec:java -Dexec.mainClass="com.artfii.amq.core.AioMqServer" -Dexec.classpathScope=runtime

#ps -ef | grep java | grep -v grepcl
# kill 进程
#ps -ef | grep java | grep -v grep | awk '{print $2}' | xargs kill -9

#echo `ps -ef | grep java | grep -v AioMqServer | awk '{print $2}' `

kill -9 $(lsof -i:8888 -t)
kill -9 $(lsof -i:8889 -t)
echo done

# How to run : sh ./bin/server.sh