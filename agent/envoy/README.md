# GALEB ENVOY AGENT

## Requisite
* Ruby (>= 2.3.0)
* Docker (using envoyproxy/envoy-dev image)

## Setup
```
bundle install
mkdir -p /opt/galeb/{conf,scripts}
cp -av envoy.yaml.erb /opt/galeb/conf/
cp -av update-envoy.sh /opt/galeb/scripts/
```

## Running
```
nohup /opt/galeb/scripts/update-envoy.sh < /dev/null > /dev/null 2>&1 &
```
