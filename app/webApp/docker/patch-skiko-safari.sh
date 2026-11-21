#!/bin/sh
# Works around WebKit bug 329421 (JetBrains CMP-10986), where Safari's strict-mode Proper Tail Calls
# corrupt numeric/float arguments passed through Skiko's lazy Wasm export wrappers into skiko.wasm,
# causing a blank white canvas on macOS Safari and iOS browsers.
#
# Rewrites each line of the form:
#   export let NAME = (...a) => (NAME = loadedWasm._["NAME"])(...a)
# into a non-tail-call wrapper:
#   export let NAME = (...a) => { try { return (NAME = loadedWasm._["NAME"])(...a); } catch (e) { throw e; } }
set -eu

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <path-to-skiko.mjs>" >&2
    exit 1
fi

target="$1"
if [ ! -f "$target" ]; then
    echo "patch-skiko-safari: file not found: $target" >&2
    exit 1
fi

tmp="${target}.tmp.$$"
trap 'rm -f "$tmp"' EXIT INT TERM

awk '
{
    line = $0
    sub(/\r$/, "", line)
    if (NF >= 6 && $1 == "export" && $2 == "let" && $4 == "=") {
        name = $3
        unpatched = "export let " name " = (...a) => (" name " = loadedWasm._[\"" name "\"])(...a)"
        patched_line = "export let " name " = (...a) => { try { return (" name " = loadedWasm._[\"" name "\"])(...a); } catch (e) { throw e; } }"
        if (line == unpatched || line == unpatched ";") {
            print patched_line
            patched++
            next
        }
        if (line == patched_line || line == patched_line ";") {
            print $0
            already++
            next
        }
    }
    print $0
}
END {
    if (patched == 0 && already == 0) {
        print "patch-skiko-safari: no Skiko tail-call export wrappers found; inspect skiko.mjs format" > "/dev/stderr"
        exit 2
    }
    printf "patch-skiko-safari: patched=%d already_patched=%d\n", patched + 0, already + 0 > "/dev/stderr"
}
' "$target" > "$tmp"

mv "$tmp" "$target"
trap - EXIT INT TERM
