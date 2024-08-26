#! /bin/bash

#
# Copyright 2024, Redis Ltd. and Contributors
# All rights reserved.
#
# Licensed under the MIT License.
#

if [ -z ${START_PORT} ]; then
    START_PORT=26379
fi
if [ -z ${END_PORT} ]; then
    END_PORT=26384
fi
if [ ! -z "$3" ]; then
    START_PORT=$2
    START_PORT=$3
fi

for PORT in `seq ${START_PORT} ${END_PORT}`; do
  echo ">>> Starting Redis server at port ${PORT}"
  /opt/redis-stack/bin/redis-server /nodes/$PORT/redis.conf > /nodes/$PORT/console.log
  if [ $? -ne 0 ]; then
    echo "Redis failed to start, exiting."
    continue
  fi
  echo 127.0.0.1:$PORT >> /nodes/nodemap
done

tail -f /redis.log