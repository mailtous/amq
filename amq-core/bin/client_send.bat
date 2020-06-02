echo "AMQ client send ...."
mvn -DskipTests=true -f pom.xml -P prod
mvn exec:java -Dexec.mainClass="com.artfii.amq.tester.TestSend"

