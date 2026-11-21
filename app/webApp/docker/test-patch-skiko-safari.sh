#!/bin/sh
# Verifies app/webApp/docker/patch-skiko-safari.sh on synthetic fixtures.
set -eu

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
patch_script="${script_dir}/patch-skiko-safari.sh"

work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT INT TERM

fail() {
    echo "FAIL: $1" >&2
    exit 1
}

# 1. Rewrites unpatched wrappers (with and without trailing semicolon) and preserves other lines.
sample="${work_dir}/skiko.mjs"
cat <<'EOF' > "$sample"
// header comment
let loadedWasm;
export let malloc = (...a) => (malloc = loadedWasm._["malloc"])(...a)
export let org_jetbrains_skia_Canvas__1nClear = (...a) => (org_jetbrains_skia_Canvas__1nClear = loadedWasm._["org_jetbrains_skia_Canvas__1nClear"])(...a);
export function keepMe(x) { return x + 1; }
EOF

expected="${work_dir}/expected.mjs"
cat <<'EOF' > "$expected"
// header comment
let loadedWasm;
export let malloc = (...a) => { try { return (malloc = loadedWasm._["malloc"])(...a); } catch (e) { throw e; } }
export let org_jetbrains_skia_Canvas__1nClear = (...a) => { try { return (org_jetbrains_skia_Canvas__1nClear = loadedWasm._["org_jetbrains_skia_Canvas__1nClear"])(...a); } catch (e) { throw e; } }
export function keepMe(x) { return x + 1; }
EOF

sh "$patch_script" "$sample" >/dev/null 2>&1 || fail "patching sample failed"
diff -u "$expected" "$sample" || fail "patched output did not match expected"

# 2. Idempotent re-run succeeds and keeps output unchanged.
sh "$patch_script" "$sample" >/dev/null 2>&1 || fail "idempotent re-run failed"
diff -u "$expected" "$sample" || fail "idempotent re-run changed output"

# 3. Missing file fails non-zero.
if sh "$patch_script" "${work_dir}/does-not-exist.mjs" >/dev/null 2>&1; then
    fail "expected missing file to fail"
fi

# 4. File with zero matching wrappers fails non-zero and leaves file untouched.
unmatched="${work_dir}/unmatched.mjs"
cat <<'EOF' > "$unmatched"
export const somethingElse = 42;
EOF
unmatched_orig="${work_dir}/unmatched.orig.mjs"
cp "$unmatched" "$unmatched_orig"

if sh "$patch_script" "$unmatched" >/dev/null 2>&1; then
    fail "expected unmatched file to fail"
fi
diff -u "$unmatched_orig" "$unmatched" || fail "unmatched file was modified on failure"

echo "PASS: all patch-skiko-safari.sh tests passed"
