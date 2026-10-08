#!/bin/sh
set -e

# Resolve upload root directory, defaulting to /app/uploads
UPLOAD_DIR="${APP_UPLOAD_DIR:-/app/uploads}"

# If running as root, prepare persistent volume ownership and drop privileges to appuser
if [ "$(id -u)" -eq 0 ]; then
    mkdir -p "$UPLOAD_DIR/avatars" "$UPLOAD_DIR/services" "$UPLOAD_DIR/articles"
    chown -R appuser:appgroup "$UPLOAD_DIR"
    chmod 775 "$UPLOAD_DIR" "$UPLOAD_DIR/avatars" "$UPLOAD_DIR/services" "$UPLOAD_DIR/articles"

    if [ "$#" -eq 0 ]; then
        exec su-exec appuser:appgroup sh -c 'exec java $JAVA_OPTS -jar app.jar'
    fi

    if [ "${1#-}" != "$1" ]; then
        exec su-exec appuser:appgroup sh -c 'exec java $JAVA_OPTS -jar app.jar "$@"' -- "$@"
    fi

    exec su-exec appuser:appgroup "$@"
fi

# Fallback when container is already started as non-root
mkdir -p "$UPLOAD_DIR/avatars" "$UPLOAD_DIR/services" "$UPLOAD_DIR/articles" 2>/dev/null || true

if [ "$#" -eq 0 ]; then
    exec sh -c 'exec java $JAVA_OPTS -jar app.jar'
fi

if [ "${1#-}" != "$1" ]; then
    exec sh -c 'exec java $JAVA_OPTS -jar app.jar "$@"' -- "$@"
fi

exec "$@"
