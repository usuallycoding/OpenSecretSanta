#!/bin/sh
# Writes the web app's config.json from PUBLIC_SERVER_URL (and, when push is set up, the public PUBLIC_FIREBASE_*
# settings) each time the container starts, so the one built image works for any server address.
set -eu

# a value goes into JSON as it is, so it must not contain anything that would need escaping
check() {
    case "$2" in
        *\"* | *\\* | *[[:space:]]*)
            echo "$1 must not contain quotes, backslashes or spaces: $2" >&2
            exit 1
            ;;
    esac
}

url="${PUBLIC_SERVER_URL:-}"
if [ -z "$url" ]; then
    echo "PUBLIC_SERVER_URL is required: the address the browser uses to reach the server, for example http://localhost:8081" >&2
    exit 1
fi
check PUBLIC_SERVER_URL "$url"

# The file Android reads to verify that this web address belongs to the app, written from the release certificate's
# fingerprint; without the setting there is no file, and a stale one from an earlier start is removed.
assetlinks_dir=/usr/share/nginx/html/.well-known
fingerprint="${PUBLIC_ANDROID_CERT_SHA256:-}"
if [ -z "$fingerprint" ]; then
    rm -f "$assetlinks_dir/assetlinks.json"
else
    # 32 hexadecimal pairs separated by colons, which is what keytool prints
    if ! printf '%s' "$fingerprint" | grep -Eq '^([0-9A-Fa-f]{2}:){31}[0-9A-Fa-f]{2}$'; then
        echo "PUBLIC_ANDROID_CERT_SHA256 must be a SHA-256 fingerprint: 32 hexadecimal pairs separated by colons (AA:BB:...), not: $fingerprint" >&2
        exit 1
    fi
    fingerprint="$(printf '%s' "$fingerprint" | tr 'a-f' 'A-F')"
    mkdir -p "$assetlinks_dir"
    printf '[\n  {\n    "relation": ["delegate_permission/common.handle_all_urls"],\n    "target": {\n      "namespace": "android_app",\n      "package_name": "com.usuallycoding.opensecretsanta",\n      "sha256_cert_fingerprints": ["%s"]\n    }\n  }\n]\n' "$fingerprint" > "$assetlinks_dir/assetlinks.json"
    echo "assetlinks.json: the Android app's certificate is $fingerprint"
fi

api_key="${PUBLIC_FIREBASE_API_KEY:-}"
project_id="${PUBLIC_FIREBASE_PROJECT_ID:-}"
sender_id="${PUBLIC_FIREBASE_MESSAGING_SENDER_ID:-}"
app_id="${PUBLIC_FIREBASE_APP_ID:-}"
vapid_key="${PUBLIC_FIREBASE_VAPID_KEY:-}"

config=/usr/share/nginx/html/config.json
if [ -z "$api_key$project_id$sender_id$app_id$vapid_key" ]; then
    printf '{\n  "baseUrl": "%s"\n}\n' "$url" > "$config"
    echo "config.json: baseUrl is $url (no push settings)"
    exit 0
fi

# push needs all five or none: a half-filled set would only fail later in the browser
for name in PUBLIC_FIREBASE_API_KEY PUBLIC_FIREBASE_PROJECT_ID PUBLIC_FIREBASE_MESSAGING_SENDER_ID PUBLIC_FIREBASE_APP_ID PUBLIC_FIREBASE_VAPID_KEY; do
    eval "value=\${$name:-}"
    if [ -z "$value" ]; then
        echo "$name is required when any PUBLIC_FIREBASE_* setting is set" >&2
        exit 1
    fi
    check "$name" "$value"
done

printf '{\n  "baseUrl": "%s",\n  "firebase": {\n    "apiKey": "%s",\n    "projectId": "%s",\n    "messagingSenderId": "%s",\n    "appId": "%s",\n    "vapidKey": "%s"\n  }\n}\n' \
    "$url" "$api_key" "$project_id" "$sender_id" "$app_id" "$vapid_key" > "$config"
echo "config.json: baseUrl is $url, push settings for project $project_id"
