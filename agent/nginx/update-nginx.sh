#!/bin/bash

[ ! -d /etc/nginx/conf.d ] && mkdir -p /etc/nginx/conf.d
while :; do
erb /opt/galeb/conf/sites.conf.erb > /tmp/sites.conf
[ ! -f /etc/nginx/conf.d/sites.conf ] && cp -a /tmp/sites.conf /etc/nginx/conf.d/sites.conf
if [ "$(md5sum /tmp/sites.conf | cut -d' ' -f1)" != "$(md5sum /etc/nginx/conf.d/sites.conf | cut -d' ' -f1)" ]; then
  mv /tmp/sites.conf /etc/nginx/conf.d/sites.conf
  /etc/init.d/nginx reload
  echo "NGINX: UPDATED" | logger
else
  echo "NGINX: NOT MODIFIED" | logger
fi
sleep 10
done
