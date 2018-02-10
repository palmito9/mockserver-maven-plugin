#!/usr/bin/env bash

set -e

export MAVEN_OPTS="$MAVEN_OPTS -XX:MaxPermSize=1024m -Xmx2048m"
export JAVA_OPTS="$JAVA_OPTS -XX:MaxPermSize=1024m -Xmx2048m"
echo
java -version
echo
mvn -version
echo

for folder in mockserver-maven-plugin mockserver-maven-plugin-integration-tests; do
  	cd $folder
	if test "$BUILDKITE_BRANCH" = "master"; then
	    echo "BRANCH: MASTER"
	    mvn clean deploy $1 -Djava.security.egd=file:/dev/./urandom
	else
	    echo "BRANCH: $CURRENT_BRANCH"
	    mvn clean install $1 -Djava.security.egd=file:/dev/./urandom
	fi
  	cd ..
done 