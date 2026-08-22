#!/bin/bash
set -e

# Fetch virus definitions once before clamd starts, so a fresh deploy always
# scans against current signatures rather than whatever shipped in the image.
freshclam --quiet || echo "freshclam: initial update failed, clamd will use whatever definitions are already present"

# Keep definitions current for the life of the container.
freshclam -d --quiet &

# clamd needs its own low-privilege system user, separate from the app's.
su -p clamav -s /bin/sh -c "clamd" &

# Give clamd a bounded amount of time to come up before we start accepting uploads.
for i in $(seq 1 60); do
  if (exec 3<>/dev/tcp/127.0.0.1/3310) 2>/dev/null; then
    break
  fi
  sleep 1
done

exec su -p appuser -s /bin/sh -c "exec java -jar /app/app.jar"
