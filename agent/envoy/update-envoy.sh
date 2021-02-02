#!/bin/bash

[ ! -d /etc/envoy ] && mkdir -p /etc/envoy
while :; do
erb /opt/galeb/conf/envoy.yaml.erb > /tmp/envoy.yaml
[ ! -f /etc/envoy/envoy.yaml ] && cp -a /tmp/envoy.yaml /etc/envoy/envoy.yaml
if [ "$(md5sum /tmp/envoy.yaml | cut -d' ' -f1)" != "$(md5sum /etc/envoy/envoy.yaml | cut -d' ' -f1)" ]; then
  mv /tmp/envoy.yaml /etc/envoy/envoy.yaml
  docker stop envoy || true
  sleep 1
  docker run -d --rm --name envoy --net host -v /etc/envoy:/etc/envoy envoyproxy/envoy-dev
  echo "ENVOY: UPDATED" | logger
else
  echo "ENVOY: NOT MODIFIED" | logger
fi
sleep 10
done
