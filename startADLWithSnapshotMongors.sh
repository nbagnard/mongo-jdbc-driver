#!/bin/bash
 
DATE=$(date)
 
EXECUTE_GO=cmd/mongohoused/internal/execute.go
SED_LINES="
       sed -i.bak '158,160 s/^/\/\//' $EXECUTE_GO
       sed -i.bak '21 s/^/\/\//' $EXECUTE_GO
       # Possible access issue
       sed -i.bak '4 s/^/\/\//' go.mod
       sed -i.bak '/Modified mongosql-rs/d' $EXECUTE_GO
       # Add a line to verify modified version is running
       sed -i.bak '161i\'$'\n''logger.Info(\"Modified mongosql-rs $DATE\")'$'\n' $EXECUTE_GO
       # Not common to have an update here
       go get -u github.com/10gen/mongosql-rs/go
       go mod tidy"
 
echo "${SED_LINES}" > /tmp/sedlines.txt
cp ./resources/run_adl.sh ./resources/run_adl.back
sed -i.bak "/git pull \$MONGOHOUSE_URI/r /tmp/sedlines.txt" ./resources/run_adl.sh
sed -i.bak '/rm -f $MONGOSQL_LIB/s/^/#/' ./resources/run_adl.sh
 
# Setup ADL directories
./resources/run_adl.sh start 
./resources/run_adl.sh stop
 
cp $MONGOSQLRS_SNAPSHOT_DIR/target/debug/libmongosql.a local_adl/mongohouse/artifacts 
./resources/run_adl.sh start 
