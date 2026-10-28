#!/bin/sh
# Writes the web app's config.json from PUBLIC_SERVER_URL each time the container starts,
# so the one built image works for any server address.
set -eu

url="${PUBLIC_SERVER_URL:-}"
if [ -z "$url" ]; then
    echo "PUBLIC_SERVER_URL is required: the address the browser uses to reach the server, for example http://localhost:8081" >&2
    exit 1
fi
case "$url" in
    *\"* | *\\* | *[[:space:]]*)
        echo "PUBLIC_SERVER_URL must not contain quotes, backslashes or spaces: $url" >&2
        exit 1
        ;;
esac

printf '{\n  "baseUrl": "%s"\n}\n' "$url" > /usr/share/nginx/html/config.json
echo "config.json: baseUrl is $url"
