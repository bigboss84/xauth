#!/bin/bash

# Admin User
ADMIN_USER=${ADMIN_USER:-"admin"}
ADMIN_PASS=${ADMIN_PASS:-"admin"}

# Application Database User
APP_DATABASE=${APP_DATABASE:-"admin"}
APP_USERNAME=${APP_USERNAME:-"user"}
APP_PASSWORD=${APP_PASSWORD:-"pass"}

# Wait for MongoDB to boot
RET=1
while [[ RET -ne 0 ]]; do
    echo "=> Waiting for confirmation of MongoDB service startup..."
    sleep 5
    mongo admin --eval "help" >/dev/null 2>&1
    RET=$?
done

# Create the admin user
echo "=> Creating admin user with a password in MongoDB"
mongo admin --eval "db.createUser({user: '$ADMIN_USER', pwd: '$ADMIN_PASS', roles:[{role:'root',db:'admin'}]});"

sleep 3

if [ "$APP_DATABASE" != "admin" ]; then
    echo "=> Creating a ${APP_DATABASE} database user with a password in MongoDB"
    mongo admin -u $ADMIN_USER -p $ADMIN_PASS << EOF
echo "Using $APP_DATABASE database"
use $APP_DATABASE
db.createUser({user: '$APP_USERNAME', pwd: '$APP_PASSWORD', roles:[{role:'dbOwner', db:'$APP_DATABASE'}]})
EOF
fi

sleep 1

touch /data/db/.mongodb_password_set

echo "MongoDB configured successfully. You may now connect to the DB."

