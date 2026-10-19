#!/bin/sh
# Retra engine test runner: builds retra-headless and runs every available test ROM.
#
#   engine/host/run_tests.sh            (downloads freely-licensed test ROMs into $ROMS)
#
# Environment:
#   ROMS=/tmp/retra-test-roms   where test ROMs are downloaded/unpacked (never the repo)
#   BUILD=/tmp/retra-host-build host build directory
#   NO_DOWNLOAD=1               only use ROMs already present
#
# Exit status is non-zero if any check fails. Missing ROMs are reported as SKIP.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
ENGINE="$(cd "$HERE/.." && pwd)"
REPO="$(cd "$ENGINE/.." && pwd)"
ROMS=${ROMS:-/tmp/retra-test-roms}
BUILD=${BUILD:-/tmp/retra-host-build}
PY="python3 -I"
PASS=0
FAIL=0
SKIP=0

pass() { PASS=$((PASS + 1)); printf '  PASS  %s\n' "$1"; }
fail() { FAIL=$((FAIL + 1)); printf '  FAIL  %s %s\n' "$1" "${2:-}"; }
skip() { SKIP=$((SKIP + 1)); printf '  SKIP  %s %s\n' "$1" "${2:-}"; }

# ------------------------------------------------------------------ build
echo "== building retra-headless ($BUILD)"
mkdir -p "$BUILD"
if command -v ninja >/dev/null 2>&1; then GEN="-G Ninja"; else GEN=""; fi
( cd "$BUILD" && cmake $GEN "$HERE" -DCMAKE_BUILD_TYPE=Release >/dev/null && cmake --build . -j 2 >/dev/null ) || {
    echo "build failed"; exit 1; }
BIN="$BUILD/retra-headless"

# ------------------------------------------------------------------ fetch test ROMs
mkdir -p "$ROMS"
fetch() {  # fetch URL DEST_DIR (zip archives are unpacked into DEST_DIR)
    url=$1; dest=$2
    [ -d "$dest" ] && return 0
    [ "${NO_DOWNLOAD:-0}" = "1" ] && return 1
    tmp="$ROMS/.dl.$$"
    if curl -sfL --max-time 120 -o "$tmp" "$url"; then
        mkdir -p "$dest"
        case "$url" in
            *.zip|*/master.zip) (cd "$dest" && unzip -q -o "$tmp") ;;
            *) mv "$tmp" "$dest/$(basename "$url")" ;;
        esac
        rm -f "$tmp"
        return 0
    fi
    rm -f "$tmp"
    return 1
}
echo "== test ROMs ($ROMS)"
fetch https://github.com/jsmolka/gba-tests/archive/refs/heads/master.zip "$ROMS/jsmolka" || true
fetch https://github.com/destoer/armwrestler-gba-fixed/archive/refs/heads/master.zip "$ROMS/armwrestler" || true
fetch https://s3.amazonaws.com/mgba/suite-latest.zip "$ROMS/mgba-suite" || true
fetch "https://web.archive.org/web/20240225082201id_/http://www.coranac.com/files/tonc-bin.zip" "$ROMS/tonc" || true

JS="$ROMS/jsmolka/gba-tests-master"
AW="$ROMS/armwrestler/armwrestler-gba-fixed-master/armwrestler-gba-fixed.gba"
SUITE="$ROMS/mgba-suite/suite.gba"
TONC="$ROMS/tonc/bin"
OUT="$BUILD/shots"
mkdir -p "$OUT"

# ------------------------------------------------------------------ jsmolka gba-tests
echo "== jsmolka/gba-tests"
JS_PASS_HASH=481125cc01150243   # "All tests passed" screen
for t in arm/arm thumb/thumb memory/memory bios/bios nes/nes unsafe/unsafe \
         save/none save/sram save/flash64 save/flash128; do
    rom="$JS/$t.gba"
    if [ ! -f "$rom" ]; then skip "jsmolka $t" "(missing)"; continue; fi
    h=$("$BIN" "$rom" --frames 120 --hash --out "$OUT/js_$(basename $t).png" 2>/dev/null | sed -n 's/hash=//p')
    if [ "$h" = "$JS_PASS_HASH" ]; then pass "jsmolka $t"; else fail "jsmolka $t" "(screen hash $h, see $OUT/js_$(basename $t).png)"; fi
done
for t in hello shades stripes; do
    rom="$JS/ppu/$t.gba"
    if [ ! -f "$rom" ]; then skip "jsmolka ppu/$t"; continue; fi
    want=$(sed -n "s/^jsmolka-ppu-$t //p" "$HERE/expected_hashes.txt")
    h=$("$BIN" "$rom" --frames 60 --hash --out "$OUT/js_ppu_$t.png" 2>/dev/null | sed -n 's/hash=//p')
    if [ "$h" = "$want" ]; then pass "jsmolka ppu/$t"; else fail "jsmolka ppu/$t" "(hash $h)"; fi
done

# ------------------------------------------------------------------ armwrestler
echo "== armwrestler-gba-fixed"
if [ -f "$AW" ]; then
    # ARM pages: START from the menu, then START through the 5 pages.
    "$BIN" "$AW" --frames 241 --press "60:START,100:START,140:START,180:START,220:START" --shots "$OUT/aw_arm:40" \
        >/dev/null 2>&1 || mkdir -p "$OUT/aw_arm"
    mkdir -p "$OUT/aw_arm" "$OUT/aw_thumb"
    "$BIN" "$AW" --frames 241 --press "60:START,100:START,140:START,180:START,220:START" --shots "$OUT/aw_arm:40" >/dev/null 2>&1
    "$BIN" "$AW" --frames 300 --press "40:DOWN,60:DOWN,80:DOWN,100:START,140:START,180:START" --shots "$OUT/aw_thumb:20" >/dev/null 2>&1
    for shot in aw_arm/frame_00080 aw_arm/frame_00120 aw_arm/frame_00160 aw_arm/frame_00200 aw_arm/frame_00240 \
                aw_thumb/frame_00120 aw_thumb/frame_00160 aw_thumb/frame_00200; do
        f="$OUT/$shot.png"
        bad=$($PY "$HERE/pngcheck.py" "$f" count 255 0 0 120 2>/dev/null)
        ok=$($PY "$HERE/pngcheck.py" "$f" count 0 198 0 40 2>/dev/null)
        if [ "${bad:-1}" = "0" ] && [ "${ok:-0}" -gt 100 ]; then pass "armwrestler $shot"; else fail "armwrestler $shot" "(bad=$bad ok=$ok)"; fi
    done
else
    skip "armwrestler" "(missing)"
fi

# ------------------------------------------------------------------ mGBA test suite
echo "== mGBA suite (score >= recorded baseline)"
if [ -f "$SUITE" ]; then
    i=0
    while read -r name min; do
        [ -z "$name" ] && continue
        spec=""; f=20; k=0
        while [ $k -lt $i ]; do spec="$spec$f-$((f+1)):DOWN,"; f=$((f+6)); k=$((k+1)); done
        spec="${spec}$((f+10))-$((f+11)):A"
        res=$("$BIN" "$SUITE" --frames $((f+1500)) --press "$spec" --debug-log 2>/dev/null | sed -n 's/.*END: \([0-9]*\)\/\([0-9]*\).*/\1 \2/p' | head -1)
        got=${res%% *}; total=${res##* }
        if [ -n "$got" ] && [ "$got" -ge "$min" ]; then pass "suite $name $got/$total"; else fail "suite $name" "(${got:-?}/${total:-?}, baseline $min)"; fi
        i=$((i + 1))
    done < "$HERE/suite_baseline.txt"
else
    skip "mGBA suite" "(missing)"
fi

# ------------------------------------------------------------------ tonc demos (regression hashes)
echo "== tonc demos (frame 120 screenshots vs. visually verified hashes)"
if [ -d "$TONC" ]; then
    grep '^tonc-' "$HERE/expected_hashes.txt" | while read -r key want; do
        n=${key#tonc-}
        rom="$TONC/$n.gba"
        h=$("$BIN" "$rom" --frames 120 --hash --out "$OUT/tonc_$n.png" 2>/dev/null | sed -n 's/hash=//p')
        if [ "$h" = "$want" ]; then echo "  PASS  tonc $n"; else echo "  FAIL  tonc $n (hash $h, see $OUT/tonc_$n.png)"; fi
    done > "$OUT/tonc.log"
    cat "$OUT/tonc.log"
    PASS=$((PASS + $(grep -c '  PASS' "$OUT/tonc.log")))
    FAIL=$((FAIL + $(grep -c '  FAIL' "$OUT/tonc.log")))
else
    skip "tonc demos" "(missing)"
fi

# ------------------------------------------------------------------ Retra's own test ROMs
echo "== Retra test ROMs (RTC, EEPROM, sound)"
if command -v arm-none-eabi-gcc >/dev/null 2>&1; then
    OWN="$BUILD/testroms"
    "$HERE/testroms/build.sh" "$OWN" >/dev/null
    for t in rtc_test eeprom8k_test eeprom512_test; do
        if "$BIN" "$OWN/$t.gba" --frames 60 --debug-log 2>/dev/null | grep -q "RESULT PASS"; then pass "$t"; else fail "$t"; fi
    done
    "$BIN" "$OWN/sound_test.gba" --frames 180 --wav "$OUT/sound_test.wav" >/dev/null 2>&1
    r=$($PY "$HERE/wavcheck.py" "$OUT/sound_test.wav")
    l=$(echo "$r" | sed -n 's/left: freq=\([0-9]*\).*/\1/p'); rr=$(echo "$r" | sed -n 's/right: freq=\([0-9]*\).*/\1/p')
    if [ "${l:-0}" -ge 1020 ] && [ "${l:-0}" -le 1028 ]; then pass "sound DirectSound A 1024 Hz ($l)"; else fail "sound DirectSound" "($r)"; fi
    if [ "${rr:-0}" -ge 436 ] && [ "${rr:-0}" -le 444 ]; then pass "sound PSG ch2 440 Hz ($rr)"; else fail "sound PSG" "($r)"; fi
else
    skip "own test ROMs" "(arm-none-eabi-gcc not installed)"
fi

# ------------------------------------------------------------------ Mochi Hop (bundled original game)
echo "== Mochi Hop"
MH="$REPO/app/src/main/assets/games/mochi_hop.gba"
if [ -f "$MH" ]; then
    "$BIN" "$MH" --frames 120 --out "$OUT/mochi_title.png" >/dev/null 2>&1
    "$BIN" "$MH" --frames 700 --press "200:START,300:START,400:START,500-690:RIGHT" --out "$OUT/mochi_play.png" \
        --wav "$OUT/mochi.wav" >/dev/null 2>&1
    ct=$($PY "$HERE/pngcheck.py" "$OUT/mochi_title.png" colors)
    cp=$($PY "$HERE/pngcheck.py" "$OUT/mochi_play.png" colors)
    if [ "${ct:-0}" -gt 20 ]; then pass "mochi title screen ($ct colours)"; else fail "mochi title" "($ct colours)"; fi
    if [ "${cp:-0}" -gt 20 ] && ! cmp -s "$OUT/mochi_title.png" "$OUT/mochi_play.png"; then
        pass "mochi gameplay ($cp colours)"; else fail "mochi gameplay"; fi
    peak=$($PY "$HERE/wavcheck.py" "$OUT/mochi.wav" 1 | sed -n 's/left: .*peak=\([0-9]*\)/\1/p')
    if [ "${peak:-0}" -gt 500 ]; then pass "mochi music audible (peak $peak)"; else fail "mochi music" "(peak $peak)"; fi
    if "$BIN" "$MH" --frames 800 --press "200:START,300:START,400:START,500-700:RIGHT,560:A" --state-test 2>/dev/null | grep -q PASS; then
        pass "save-state round trip (mochi)"; else fail "save-state round trip (mochi)"; fi
    "$BIN" "$MH" --frames 2000 --press "200:START,300:START,400:START,500-1900:RIGHT" --bench 2>/dev/null | sed 's/^/  BENCH mochi_hop: /'
else
    skip "mochi hop" "(missing)"
fi
if [ -f "$TONC/m7_ex.gba" ]; then
    "$BIN" "$TONC/m7_ex.gba" --frames 2000 --bench 2>/dev/null | sed 's/^/  BENCH tonc m7_ex: /'
fi

echo
echo "== summary: $PASS passed, $FAIL failed, $SKIP skipped (screenshots in $OUT)"
[ "$FAIL" -eq 0 ]
