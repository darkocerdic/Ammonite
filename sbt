#!/bin/bash

EXPECTED_SBTJAR_MD5="00672c01d5beea62928e33cdeae7b46b"

root=$(
  cd $(dirname $(readlink $0 || echo $0))/..
  /bin/pwd
)

sbtjar=sbt-launch.jar

function sbtjar_md5 {
  openssl md5 < $sbtjar|cut -f2 -d'='|awk '{print $1}'
}

if [ ! -f $sbtjar ] || [ "$(sbtjar_md5)" != "$EXPECTED_SBTJAR_MD5" ]; then
  echo 'downloading '$sbtjar 1>&2
  wget -O "$sbtjar" "https://dl.bintray.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.13.8/$sbtjar"
fi

test -f $sbtjar || exit 1
if [ "$(sbtjar_md5)" != "$EXPECTED_SBTJAR_MD5" ]; then
  echo 'bad sbtjar!' 1>&2
  exit 1
fi

test -f ~/.sbtconfig && . ~/.sbtconfig

java -ea                          \
  $SBT_OPTS                       \
  $JAVA_OPTS                      \
  -Djava.net.preferIPv4Stack=true \
  -XX:+AggressiveOpts             \
  -XX:+UseParNewGC                \
  -XX:+UseConcMarkSweepGC         \
  -XX:+CMSParallelRemarkEnabled   \
  -XX:+CMSClassUnloadingEnabled   \
  -XX:SurvivorRatio=128           \
  -XX:MaxTenuringThreshold=0      \
  -XX:ReservedCodeCacheSize=128m  \
  -Xss8M                          \
  -Xms512M                        \
  -Xmx3G                          \
  -server                         \
  -jar $sbtjar "$@"
