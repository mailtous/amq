echo "AMQ client recv ...."

mvn -DskipTests=true -f pom.xml -P prod
mvn exec:java -Dexec.mainClass="com.artfii.amq.tester.TestRecv1"
