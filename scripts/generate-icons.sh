#!/usr/bin/env bash
# Makes every icon of the app from one SVG and writes them in place: the Android launcher icons, the iOS app icon and the
# website's favicon, touch icon and manifest.
#
#   scripts/generate-icons.sh [--source assets/secret-santa-icon.svg] [--background "#FFDBD1"]
#
# The artwork is placed on the background colour wherever a platform needs an opaque icon (the default is the app's light
# theme colour, its primary container). Needs `rsvg-convert` and ImageMagick (`magick`): brew install librsvg imagemagick
# Nothing is written unless every icon was made, and running it twice changes nothing.
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"

source_svg=assets/secret-santa-icon.svg
background='#FFDBD1'
theme_color='#8F4C38'   # the app's primary colour, for the browser's address bar

die() { echo "generate-icons: $*" >&2; exit 1; }

usage() {
  # the comment block at the top of this file
  sed -n '2,/^[^#]/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'
  exit "${1:-0}"
}

while [ $# -gt 0 ]; do
  case $1 in
    --source) [ $# -ge 2 ] || die "--source needs a value"; source_svg=$2; shift 2 ;;
    --background) [ $# -ge 2 ] || die "--background needs a value"; background=$2; shift 2 ;;
    -h|--help) usage 0 ;;
    *) echo "generate-icons: unknown option $1" >&2; usage 1 ;;
  esac
done

# --- what is needed, before anything is made ----------------------------------------------------------------------

[ -f "$source_svg" ] || die "the source is missing: $source_svg"
command -v rsvg-convert >/dev/null || die "rsvg-convert is needed: brew install librsvg"
command -v magick >/dev/null || die "ImageMagick is needed: brew install imagemagick"
case $background in
  '#'[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]) ;;
  *) die "the background must look like #FFDBD1 (got: $background)" ;;
esac
background=$(echo "$background" | tr 'a-f' 'A-F')

android_res=app/androidApp/res
ios_icons=app/iosApp/src/Assets.xcassets/AppIcon.appiconset
web_resources=app/webApp/resources

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT INT TERM
out=$work/out

# --- the artwork -----------------------------------------------------------------------------------------------------

rsvg-convert -w 1024 -h 1024 "$source_svg" -o "$work/master.png" || die "could not render $source_svg"
magick "$work/master.png" -trim +repage "$work/art.png"
[ -s "$work/art.png" ] || die "$source_svg shows nothing"

# no dates or other metadata, so that the same artwork always gives the same bytes
png_options=(-depth 8 -strip -define "png:exclude-chunks=date,time" -define png:compression-level=9)

# icon <size> <share of the size the artwork fills> <output> [transparent]: the artwork centred on a square canvas
icon() {
  local size=$1 share=$2 file=$3 fill=$background box
  [ "${4:-}" = transparent ] && fill=none
  box=$(( size * share / 100 ))
  mkdir -p "$(dirname "$out/$file")"
  magick -size "${size}x${size}" "xc:$fill" \( "$work/art.png" -resize "${box}x${box}" \) -gravity center -composite \
    "${png_options[@]}" "$out/$file"
}

# shape <size> <rounded|round> <input> <output>: gives the corners of an opaque icon away
shape() {
  local size=$1 kind=$2 input=$3 file=$4 last=$(( $1 - 1 )) centre=$(( $1 / 2 ))
  if [ "$kind" = round ]; then
    magick -size "${size}x${size}" xc:none -fill white -draw "circle $centre,$centre $centre,0" "$work/mask.png"
  else
    magick -size "${size}x${size}" xc:none -fill white -draw "roundrectangle 0,0 $last,$last $(( size * 18 / 100 )),$(( size * 18 / 100 ))" "$work/mask.png"
  fi
  mkdir -p "$(dirname "$out/$file")"
  magick "$input" "$work/mask.png" -alpha off -compose CopyOpacity -composite "${png_options[@]}" "$out/$file"
}

# --- Android --------------------------------------------------------------------------------------------------------

for density in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  folder=$android_res/mipmap-${density%%:*}
  size=${density##*:}
  icon "$size" 72 "$folder/square-source.png"
  shape "$size" rounded "$out/$folder/square-source.png" "$folder/ic_launcher.png"
  icon "$size" 62 "$folder/round-source.png"
  shape "$size" round "$out/$folder/round-source.png" "$folder/ic_launcher_round.png"
  rm -f "$out/$folder/square-source.png" "$out/$folder/round-source.png"
done

# the adaptive icon: 108dp at xxxhdpi is 432 pixels, and the part every launcher shows safely is the central 66dp (264 pixels)
icon 432 61 "$android_res/drawable-nodpi/ic_launcher_foreground.png" transparent
mkdir -p "$out/$android_res/drawable"
cat > "$out/$android_res/drawable/ic_launcher_background.xml" <<XML
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="$background"
        android:pathData="M0,0h108v108h-108z" />
</vector>
XML

# --- iOS: opaque, 1024 pixels ----------------------------------------------------------------------------------------

icon 1024 72 "$ios_icons/app-icon-1024.png"
magick "$out/$ios_icons/app-icon-1024.png" -background "$background" -alpha remove -alpha off -colorspace sRGB \
  "${png_options[@]}" "$out/$ios_icons/app-icon-1024.png"

# --- the website ------------------------------------------------------------------------------------------------------

mkdir -p "$out/$web_resources"
for size in 16 32 48; do icon "$size" 94 "tab-$size.png" transparent; done
magick "$out/tab-16.png" "$out/tab-32.png" "$out/tab-48.png" -strip "$out/$web_resources/favicon.ico"
rm -f "$out"/tab-*.png
icon 180 72 "$web_resources/apple-touch-icon.png"
icon 192 72 "$web_resources/icon-192.png"
icon 512 72 "$web_resources/icon-512.png"
cat > "$out/$web_resources/manifest.webmanifest" <<JSON
{
  "name": "Open Secret Santa",
  "short_name": "Secret Santa",
  "start_url": ".",
  "display": "standalone",
  "background_color": "$background",
  "theme_color": "$theme_color",
  "icons": [
    { "src": "icon-192.png", "sizes": "192x192", "type": "image/png" },
    { "src": "icon-512.png", "sizes": "512x512", "type": "image/png" }
  ]
}
JSON

# --- every icon was made: now they are put in place ------------------------------------------------------------------

# the default green robot's adaptive foreground is a vector that would win over the new PNG of the same name
rm -f "$android_res/drawable-v24/ic_launcher_foreground.xml"
rmdir "$android_res/drawable-v24" 2>/dev/null || true

( cd "$out" && find . -type f | sort ) | while IFS= read -r file; do
  file=${file#./}
  mkdir -p "$(dirname "$file")"
  cp "$out/$file" "$file"
  echo "wrote $file"
done
