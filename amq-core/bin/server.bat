echo building AMQ .......

mvn clean compile -DskipTests=true -f pom.xml -P prod
mvn exec:java -Dexec.mainClass="com.artfii.amq.core.AioMqServer" -Dexec.classpathScope=runtime

# http tester :
# wrk -t32 -c64 -d6s http://localhost:8889/hello