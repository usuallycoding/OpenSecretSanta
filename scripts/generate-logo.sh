#!/usr/bin/env bash
# Makes the logo's two pictures, for the light and the dark theme, from one SVG and writes them in place, in the shared
# module's Compose resources, where the sign-in screen shows them.
#
#   scripts/generate-logo.sh [--source assets/app-logo.svg]
#
# The source draws its words in a font that only some computers have, so the words are first turned into outlines, which
# look the same everywhere. The dark picture is the same artwork with every black outline made light (#FFE9E4), so that it
# reads on a dark background. Both are transparent and 840 pixels wide, three times the width they are shown at.
# Needs `python3`, `inkscape`, `rsvg-convert` and ImageMagick (`magick`): brew install --cask inkscape && brew install librsvg imagemagick
# Nothing is written unless both pictures were made, and running it twice changes nothing.
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"

source_svg=assets/app-logo.svg
light_edge='#FFE9E4'
output_dir=app/shared/composeResources/drawable

die() { echo "generate-logo: $*" >&2; exit 1; }

usage() {
  # the comment block at the top of this file
  sed -n '2,/^[^#]/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'
  exit "${1:-0}"
}

while [ $# -gt 0 ]; do
  case $1 in
    --source) [ $# -ge 2 ] || die "--source needs a value"; source_svg=$2; shift 2 ;;
    -h|--help) usage 0 ;;
    *) echo "generate-logo: unknown option $1" >&2; usage 1 ;;
  esac
done

# --- what is needed, before anything is made ----------------------------------------------------------------------

[ -f "$source_svg" ] || die "the source is missing: $source_svg"
command -v python3 >/dev/null || die "python3 is needed"
command -v inkscape >/dev/null || die "inkscape is needed: brew install --cask inkscape"
command -v rsvg-convert >/dev/null || die "rsvg-convert is needed: brew install librsvg"
command -v magick >/dev/null || die "ImageMagick is needed: brew install imagemagick"

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT INT TERM

# --- the artwork -----------------------------------------------------------------------------------------------------

# words to outlines, so that no picture depends on a font
inkscape --export-text-to-path --export-plain-svg --export-type=svg --export-filename="$work/light.svg" "$source_svg" >/dev/null 2>&1 \
  || die "could not outline the words of $source_svg"
[ -s "$work/light.svg" ] || die "$source_svg gave no outlined artwork"
grep -q '<text' "$work/light.svg" && die "words are left in the outlined artwork"

# the dark one: the black that borders the background becomes the light edge, whether it is written as a colour of a stroke or a
# fill, or is only what a shape with no fill of its own, here or in a group around it, is drawn in (the outlines of the words
# are such shapes). The outlines of white words stay black: those words sit on the hat, where the black is what shows them
python3 - "$work/light.svg" "$work/dark.svg" "$light_edge" <<'PY' || die "could not make the dark artwork"
import re
import sys
import xml.etree.ElementTree as ET

source, target, edge = sys.argv[1:4]
SVG = "http://www.w3.org/2000/svg"
ET.register_namespace("", SVG)
ET.register_namespace("svg", SVG)
BLACK = re.compile(r"^\s*(#000000|#000|black|rgb\(\s*0\s*,\s*0\s*,\s*0\s*\))\s*$", re.I)
SHAPES = {"path", "rect", "circle", "ellipse", "polygon", "polyline", "line"}


def properties(style):
    return {k.strip(): v.strip() for k, v in (item.split(":", 1) for item in style.split(";") if ":" in item)}


def is_white(value):
    value = (value or "").strip().lstrip("#")
    return len(value) == 6 and all(int(value[i:i + 2], 16) >= 0xE8 for i in (0, 2, 4))


def has_white_words(node):
    return any(is_white(properties(child.get("style", "")).get("fill", child.get("fill"))) for child in node)


def paint(node, inherited, keep=False):
    style = properties(node.get("style", ""))
    fill = style.get("fill", node.get("fill", inherited))
    for name in ("fill", "stroke"):
        value = style.get(name, node.get(name))
        if value is not None and BLACK.match(value) and not keep:
            style[name] = edge
            node.attrib.pop(name, None)
            if name == "fill":
                fill = edge
    if node.tag.split("}")[-1] in SHAPES and fill is None and not keep:
        style["fill"] = edge
    if style:
        node.set("style", ";".join(f"{k}:{v}" for k, v in style.items()))
    keep_children = has_white_words(node)
    for child in node:
        paint(child, fill, keep_children)


tree = ET.parse(source)
paint(tree.getroot(), None)
tree.write(target, encoding="utf-8", xml_declaration=True)
PY
cmp -s "$work/light.svg" "$work/dark.svg" && die "the source has no black to make light"

# no dates or other metadata, so that the same artwork always gives the same bytes
png_options=(-depth 8 -strip -define "png:exclude-chunks=date,time" -define png:compression-level=9)

for variant in light dark; do
  rsvg-convert -w 1200 "$work/$variant.svg" -o "$work/$variant-big.png" || die "could not render the $variant picture"
  magick "$work/$variant-big.png" -trim +repage -resize 840x "${png_options[@]}" "$work/app_logo_$variant.png"
  [ -s "$work/app_logo_$variant.png" ] || die "the $variant picture is empty"
done

# --- written together, only now ---------------------------------------------------------------------------------------

mkdir -p "$output_dir"
cp "$work/app_logo_light.png" "$work/app_logo_dark.png" "$output_dir/"
echo "generate-logo: wrote $output_dir/app_logo_light.png and app_logo_dark.png"
