#!/usr/bin/env bash
# Builds a signed release APK of the Android app and sends it to a Firebase App Distribution tester group.
#
#   scripts/distribute-android.sh [--notes "What changed"] [--version 1.4.0] [--group dincolo] [--no-upload]
#
# The signed APK is kept at build/distribution/opensecretsanta-<version>-<code>.apk whether or not it is uploaded (its path
# is printed last), so it can also be sent to someone by hand.
#
# Everything private stays on this machine, in files git ignores (see the README, "Distribute the Android app"):
#   secrets/android-release.keystore       the release key
#   secrets/android-signing.properties     storePassword=, keyAlias=, keyPassword=
#   app/androidApp/google-services.json    the Firebase app file
#   secrets/firebase-service-account.json  optional; without it the CLI uses your own `firebase login`
#
# SECRETS_DIR points somewhere else for the first three; FIREBASE_CLI replaces `npx firebase-tools` (a test uses it).
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"

secrets_dir=${SECRETS_DIR:-$root/secrets}
keystore=$secrets_dir/android-release.keystore
signing_properties=$secrets_dir/android-signing.properties
google_services=app/androidApp/google-services.json
module_file=app/androidApp/module.yaml
module_properties=app/androidApp/keystore.properties
apk_dir=build/tasks/_androidApp_buildAndroidRelease
service_account=$secrets_dir/firebase-service-account.json

notes=
version=
group=dincolo
upload=yes

die() { echo "distribute-android: $*" >&2; exit 1; }

usage() {
  # the comment block at the top of this file
  sed -n '2,/^[^#]/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'
  exit "${1:-0}"
}

while [ $# -gt 0 ]; do
  case $1 in
    --notes) [ $# -ge 2 ] || die "--notes needs a value"; notes=$2; shift 2 ;;
    --version) [ $# -ge 2 ] || die "--version needs a value"; version=$2; shift 2 ;;
    --group) [ $# -ge 2 ] || die "--group needs a value"; group=$2; shift 2 ;;
    --no-upload) upload=no; shift ;;
    -h|--help) usage 0 ;;
    *) echo "distribute-android: unknown option $1" >&2; usage 1 ;;
  esac
done

# --- what is needed, before anything is changed -------------------------------------------------------------------

[ -f "$keystore" ] || die "the release keystore is missing: $keystore (see the README)"
[ -f "$signing_properties" ] || die "the signing properties are missing: $signing_properties (see the README)"
[ -f "$google_services" ] || die "$google_services is missing (download it from the Firebase console)"
command -v python3 >/dev/null || die "python3 is needed"
command -v git >/dev/null || die "git is needed"
[ -x ./kotlin ] || die "run this from a checkout: ./kotlin is missing"

property() { grep -E "^$1=" "$signing_properties" | head -n 1 | cut -d= -f2-; }
store_password=$(property storePassword)
key_alias=$(property keyAlias)
key_password=$(property keyPassword)
[ -n "$store_password" ] || die "storePassword is missing in $signing_properties"
[ -n "$key_alias" ] || die "keyAlias is missing in $signing_properties"
[ -n "$key_password" ] || die "keyPassword is missing in $signing_properties"

build_tools=
for sdk in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
  [ -n "$sdk" ] || continue
  # the newest version folder that has apksigner (the folder also holds files of the toolchain that are no version)
  while IFS= read -r dir; do
    [ -x "${dir}apksigner" ] && build_tools=${dir%/}
  done < <(printf '%s\n' "$sdk"/build-tools/[0-9]*/ | sort -V)
  [ -z "$build_tools" ] || break
done
[ -n "$build_tools" ] || die "the Android SDK build-tools (apksigner) were not found; set ANDROID_HOME"

if [ "$upload" = yes ]; then
  firebase_cli=${FIREBASE_CLI:-"npx --yes firebase-tools@latest"}
  [ -n "${FIREBASE_CLI:-}" ] || command -v npx >/dev/null || die "Node (npx) is needed to upload"
fi

# the build edits this file for its own sake and puts it back, so it must not hold anything of the person's yet
[ -z "$(git status --porcelain -- "$module_file")" ] || die "$module_file has uncommitted changes; commit or discard them first"
[ ! -e "$module_properties" ] || die "$module_properties exists; remove it (this script writes and removes its own)"

if [ -z "$version" ]; then
  version=$(git describe --tags --always --dirty)
fi
case $version in *[!A-Za-z0-9._+-]*|'') die "the version may only hold letters, digits and . _ + - (got: $version)" ;; esac
version_code=$(( $(date +%s) / 60 ))
[ -n "$notes" ] || notes=$(git log -1 --pretty=%s)

# --- the build, with module.yaml and the properties file put back whatever happens -----------------------------------

backup=$(mktemp)
cp "$module_file" "$backup"
cleanup() {
  cp "$backup" "$module_file"
  rm -f "$backup" "$module_properties"
}
trap cleanup EXIT INT TERM

awk -v code="$version_code" -v name="$version" '
  { print }
  /^    applicationId:/ { print "    versionCode: " code; print "    versionName: \"" name "\""; print "    signing:"; print "      enabled: true" }
' "$backup" > "$module_file"
grep -q "versionCode: $version_code" "$module_file" || die "could not set the version in $module_file"

( umask 077
  {
    echo "storeFile=$keystore"
    echo "storePassword=$store_password"
    echo "keyAlias=$key_alias"
    echo "keyPassword=$key_password"
  } > "$module_properties" )

# the build does not remove the APKs of earlier builds, and an old signed one must never pass for this one
rm -f "$apk_dir"/*.apk

echo "Building $version (versionCode $version_code)..."
./kotlin build -m androidApp -v release

apk=$apk_dir/gradle-project-release.apk
[ -f "$apk" ] || die "the build made no signed APK ($apk)"

# --- checking what was built ----------------------------------------------------------------------------------------

certificates=$("$build_tools/apksigner" verify --print-certs "$apk" 2>&1) || die "the APK's signature does not verify"
case $certificates in *"Android Debug"*) die "the APK is signed with the debug key" ;; esac
badging=$("$build_tools/aapt2" dump badging "$apk" 2>&1 | grep -E "^package:")
case $badging in *"versionCode='$version_code'"*"versionName='$version'"*) ;; *) die "the APK does not carry the version $version ($version_code)" ;; esac

mkdir -p build/distribution
out=build/distribution/opensecretsanta-$version-$version_code.apk
cp "$apk" "$out"
echo "Signed APK: $out"
echo "$certificates" | grep -E "certificate DN" | head -n 1

[ "$upload" = yes ] || { echo "Not uploading (--no-upload). The APK to send by hand: $root/$out"; exit 0; }

# --- the upload -----------------------------------------------------------------------------------------------------

app_id=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["client"][0]["client_info"]["mobilesdk_app_id"])' "$google_services") \
  || die "could not read the app id from $google_services"
if [ -f "$service_account" ]; then
  export GOOGLE_APPLICATION_CREDENTIALS=$service_account
fi

echo "Uploading to the group \"$group\"..."
# shellcheck disable=SC2086  # the CLI may be several words ("npx --yes firebase-tools@latest")
$firebase_cli appdistribution:distribute "$out" --app "$app_id" --groups "$group" --release-notes "$notes"
echo "Done: $version ($version_code) was sent to $group."
echo "A copy to send by hand: $root/$out"
