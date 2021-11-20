#!/bin/bash
set -m

mongod --auth --bind_ip_all --storageEngine=wiredTiger &

if [ ! -f /data/db/.mongodb_password_set ]; then
    /set_mongodb_password.sh
fi

fg
