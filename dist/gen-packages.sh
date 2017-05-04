#!/bin/sh

NECTAR_VERSION=0.4.4
NECTAR_SERVER_JAR=Nectar-Server-$NECTAR_VERSION-SNAPSHOT.jar
BUILD_NUMBER=0

if [ $# == 0 ]; then
    BUILD_NUMBER=1
else
    BUILD_NUMBER=$1
fi

# Build Deb
fpm -s dir -t deb -a all -n nectar-server -v $NECTAR_VERSION --iteration $BUILD_NUMBER --after-install nectar-server-install.sh ../target/$NECTAR_SERVER_JAR=/usr/lib/nectar-server/Nectar-Server.jar nectar-server=/usr/bin/nectar-server nectar-server.service=/usr/lib/systemd/system/nectar-server.service ../genkeys.sh=/etc/nectar-server/genkeys.sh
# Build RPM
fpm -s dir -t rpm -a all -n nectar-server -v $NECTAR_VERSION --iteration $BUILD_NUMBER --after-install nectar-server-install.sh ../target/$NECTAR_SERVER_JAR=/usr/lib/nectar-server/Nectar-Server.jar nectar-server=/usr/bin/nectar-server nectar-server.service=/usr/lib/systemd/system/nectar-server.service ../genkeys.sh=/etc/nectar-server/genkeys.sh