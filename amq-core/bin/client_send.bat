echo "AMQ client send ...."
mvn -DskipTests=true -f pom.xml -P prod
mvn exec:java -Dexec.mainClass="com.artlongs.amq.tester.TestSend"

