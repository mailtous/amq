echo "AMQ client recv ...."

mvn -DskipTests=true -f pom.xml -P prod
mvn exec:java -Dexec.mainClass="com.artlongs.amq.tester.TestRecv1"
