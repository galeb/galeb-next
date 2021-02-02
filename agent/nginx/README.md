# GALEB NGINX AGENT

## Requisite
* Ruby (>=2.3.0)

## Setup
```
bundle install
mkdir -p /opt/galeb/{conf,scripts}
cp -av sites.conf.erb /opt/galeb/conf/
cp -av update-nginx.sh /opt/galeb/scripts/
```

## Running
```
nohup /opt/galeb/scripts/update-nginx.sh < /dev/null > /dev/null 2>&1 &
```

## Log
* See syslog
