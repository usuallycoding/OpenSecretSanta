#!/usr/bin/env bash
# Builds a signed release APK of the Android app and sends it to a Firebase App Distribution tester group.
#
#   scripts/distribute-android.sh [--notes "What changed"] [--version 1.4.0] [--group dincolo] [--no-upload]
#
# The version is --version, or else APP_VERSION in app/shared/src/AppVersion.kt, the number the app shows; edit that line to release.
#
# The signed APK is kept at build/distribution/opensecretsanta-<version>-<code>.apk whether or not it is uploaded (its path
# is printed last), so it can also be sent to someone by hand.
#
# Everything private stays on this machine, in files git ignores (see the README, "Distribute the Android app"):
#   secrets/android-release.keystore       the release key
#   secrets/android-signing.properties     storePassword=, keyAlias=, keyPassword=
#   app/androidApp/google-services.json    the Firebase app file
#   secrets/firebase-service-account.json  optional; without it the CLI uses your own `firebase login`
#   secrets/web-app-host                   the host of the web app the emails link to, one line (santa.example.org)
#
# The checkout names that host as santa.example.com in the app's manifest and in AppLinks.kt; the build writes the real one
# there from secrets/web-app-host and puts the files back, so the real host is never committed.
#
# SECRETS_DIR points somewhere else for the files in secrets/; FIREBASE_CLI replaces `npx firebase-tools` (a test uses it).
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"

secrets_dir=${SECRETS_DIR:-$root/secrets}
keystore=$secrets_dir/android-release.keystore
signing_properties=$secrets_dir/android-signing.properties
google_services=app/androidApp/google-services.json
module_file=app/androidApp/module.yaml
manifest_file=app/androidApp/src/AndroidManifest.xml
app_links_file=app/androidApp/src/AppLinks.kt
placeholder_host=santa.example.com
web_app_host_file=$secrets_dir/web-app-host
module_properties=app/androidApp/keystore.properties
app_version_file=app/shared/src/AppVersion.kt
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
[ -f "$web_app_host_file" ] || die "the web app's host is missing: $web_app_host_file (one line, such as santa.example.org; see the README)"
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

web_app_host=$(grep -v '^[[:space:]]*#' "$web_app_host_file" | head -n 1 | tr -d '[:space:]' || true)
case $web_app_host in
  ''|*[!A-Za-z0-9.-]*|.*|*.|*..*) die "$web_app_host_file must hold a host name such as santa.example.org, and nothing else" ;;
esac
[ "$web_app_host" != "$placeholder_host" ] || die "$web_app_host_file still holds the placeholder $placeholder_host"

# the build edits these files for its own sake and puts them back, so they must not hold anything of the person's yet
for file in "$module_file" "$manifest_file" "$app_links_file"; do
  [ -z "$(git status --porcelain -- "$file")" ] || die "$file has uncommitted changes; commit or discard them first"
done
for file in "$manifest_file" "$app_links_file"; do
  [ "$(grep -cF "$placeholder_host" "$file")" = 1 ] || die "$file must name $placeholder_host on exactly one line"
done
[ ! -e "$module_properties" ] || die "$module_properties exists; remove it (this script writes and removes its own)"

if [ -z "$version" ]; then
  [ -f "$app_version_file" ] || die "$app_version_file is missing; pass --version"
  [ "$(grep -cE '^const val APP_VERSION = ' "$app_version_file")" = 1 ] || die "$app_version_file must have exactly one line 'const val APP_VERSION = \"...\"'"
  version=$(sed -n 's/^const val APP_VERSION = "\(.*\)"[[:space:]]*$/\1/p' "$app_version_file")
  [ -n "$version" ] || die "could not read APP_VERSION from $app_version_file"
fi
case $version in *[!A-Za-z0-9._+-]*|'') die "the version may only hold letters, digits and . _ + - (got: $version)" ;; esac
version_code=$(( $(date +%s) / 60 ))
[ -n "$notes" ] || notes=$(git log -1 --pretty=%s)

# --- the build, with the edited files and the properties file put back whatever happens -----------------------------------

backup=$(mktemp)
manifest_backup=$(mktemp)
app_links_backup=$(mktemp)
cp "$module_file" "$backup"
cp "$manifest_file" "$manifest_backup"
cp "$app_links_file" "$app_links_backup"
cleanup() {
  cp "$backup" "$module_file"
  cp "$manifest_backup" "$manifest_file"
  cp "$app_links_backup" "$app_links_file"
  rm -f "$backup" "$manifest_backup" "$app_links_backup" "$module_properties"
}
trap cleanup EXIT INT TERM

# the host holds only letters, digits, dots and hyphens, so it is safe in a sed replacement
sed "s/santa\.example\.com/$web_app_host/" "$manifest_backup" > "$manifest_file"
sed "s/santa\.example\.com/$web_app_host/" "$app_links_backup" > "$app_links_file"
grep -qF "\"$web_app_host\"" "$manifest_file" || die "could not set the web app's host in $manifest_file"
grep -qF "\"$web_app_host\"" "$app_links_file" || die "could not set the web app's host in $app_links_file"

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

# the link filter of the built app names the real host, not the placeholder
manifest_hosts=$("$build_tools/aapt2" dump xmltree --file AndroidManifest.xml "$apk" 2>&1 | grep -F "host(" || true)
case $manifest_hosts in *"\"$web_app_host\""*) ;; *) die "the APK's link filter does not name $web_app_host" ;; esac
case $manifest_hosts in *"\"$placeholder_host\""*) die "the APK's link filter still names the placeholder $placeholder_host" ;; esac

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
