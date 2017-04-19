#!/bin/sh

BUILD_NUMBER=0

if [ $# == 0 ]; then
    BUILD_NUMBER=1
else
    BUILD_NUMBER=$1
fi

# Build Deb
fpm -s dir -t deb -a all -n nectar-server -v 0.2.1 --iteration $BUILD_NUMBER --after-install nectar-server-install.sh ../target/Nectar-Server-0.2.1-SNAPSHOT.jar=/usr/lib/nectar-server/Nectar-Server.jar nectar-server=/usr/bin/nectar-server nectar-server.service=/usr/lib/systemd/system/nectar-server.service ../genkeys.sh=/etc/nectar-server/genkeys.sh
# Build RPM
fpm -s dir -t rpm -a all -n nectar-server -v 0.2.1 --iteration $BUILD_NUMBER --after-install nectar-server-install.sh ../target/Nectar-Server-0.2.1-SNAPSHOT.jar=/usr/lib/nectar-server/Nectar-Server.jar nectar-server=/usr/bin/nectar-server nectar-server.service=/usr/lib/systemd/system/nectar-server.service ../genkeys.sh=/etc/nectar-server/genkeys.sh