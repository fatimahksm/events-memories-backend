#!/bin/bash
set -e

# `su`, even with -p, resets PATH on Debian-based images (per /etc/login.defs)
# rather than actually preserving the caller's — so resolve absolute paths now,
# as root, while PATH is still whatever the image set up correctly.
JAVA_BIN="$(command -v java)"
CLAMD_BIN="$(command -v clamd || true)"
FRESHCLAM_BIN="$(command -v freshclam || true)"

# ClamAV is RAM-hungry (a couple GB just for its signature database). Skip it
# entirely — no freshclam, no clamd — unless the app is actually configured to
# use it, so a small/free-tier instance isn't paying for RAM it never needs.
if [ "$MALWARE_SCAN_ENABLED" = "true" ]; then
  # Fetch virus definitions once before clamd starts, so a fresh deploy always
  # scans against current signatures rather than whatever shipped in the image.
  "$FRESHCLAM_BIN" --quiet || echo "freshclam: initial update failed, clamd will use whatever definitions are already present"

  # Keep definitions current for the life of the container.
  "$FRESHCLAM_BIN" -d --quiet &

  # clamd needs its own low-privilege system user, separate from the app's.
  su -p clamav -s /bin/sh -c "'$CLAMD_BIN'" &

  # Give clamd a bounded amount of time to come up before we start accepting uploads.
  for i in $(seq 1 60); do
    if (exec 3<>/dev/tcp/127.0.0.1/3310) 2>/dev/null; then
      break
    fi
    sleep 1
  done
fi

exec su -p appuser -s /bin/sh -c "exec '$JAVA_BIN' -jar /app/app.jar"
