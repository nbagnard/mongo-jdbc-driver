./resources/run_adl.sh start && 
./gradlew runDataLoader && ./gradlew clean integrationTest -x test
./resources/run_adl.sh stop 
