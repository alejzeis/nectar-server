#!/bin/sh

# Build Deb
fpm -s dir -t deb -a all -n nectar-server -v 0.2.1 --after-install nectar-server-install.sh ../target/Nectar-Server-0.2.1-SNAPSHOT.jar=/usr/lib/nectar-server/Nectar-Server.jar nectar-server=/usr/bin/nectar-server nectar-server.service=/usr/lib/systemd/system/nectar-server.service ../genkeys.sh=/etc/nectar-server/genkeys.sh
# Build RPM
fpm -s dir -t rpm -a all -n nectar-server -v 0.2.1 --after-install nectar-server-install.sh ../target/Nectar-Server-0.2.1-SNAPSHOT.jar=/usr/lib/nectar-server/Nectar-Server.jar nectar-server=/usr/bin/nectar-server nectar-server.service=/usr/lib/systemd/system/nectar-server.service ../genkeys.sh=/etc/nectar-server/genkeys.sh