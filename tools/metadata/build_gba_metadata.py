#!/usr/bin/env python3
"""Builds Retra's bundled GBA metadata from libretro-database (CC BY-SA 4.0), pinned to one commit.

Outputs (deterministic, gzip mtime 0):
  app/src/main/assets/metadata/gba_games.tsv.gz   crc32<TAB>serial<TAB>No-Intro name (box-art lookup)
  app/src/main/assets/metadata/gba_cheats.json.gz built-in cheat books (schema below)

Cheat codes are regrouped into one code per line and validated with the same rules as the app's
CheatParser (raw AAAAAAAA:VV, CodeBreaker "XXXXXXXX YYYY", GameShark/Action Replay "XXXXXXXX YYYYYYYY").
Fan-game cheat files are mapped to Retra catalog ids, never by serial (a hack shares its base serial).

    python3 tools/metadata/build_gba_metadata.py [--cache /tmp/lrdb]
"""
import argparse
import concurrent.futures
import gzip
import io
import json
import os
import re
import sys
import urllib.parse
import urllib.request

SHA = "12c21cf79f1e20eff6ac68b80875816c717d0a7a"
RAW = "https://raw.githubusercontent.com/libretro/libretro-database/" + SHA + "/"
CHT_DIR = "cht/Nintendo - Game Boy Advance/"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "metadata")

# Hack cheat files -> Retra catalog ids (catalog/manifest.json). Checked by hand.
HACKS = {
    "Pokemon Unbound": ["pokemon-unbound"],
}

RAW_RE = re.compile(r"^[0-9A-F]{8}:([0-9A-F]{2}|[0-9A-F]{4}|[0-9A-F]{8})$")
CB_RE = re.compile(r"^[0-9A-F]{8} [0-9A-F]{4}$")
GS_RE = re.compile(r"^[0-9A-F]{8} [0-9A-F]{8}$")
TAG_RE = re.compile(r"\s*\((?:Code ?Breaker|Gameshark|GameShark|Action Replay|diff)\)", re.I)
MASTER_RE = re.compile(r"master|\(m\)|must be on|enabler|\bm code\b", re.I)


def fetch(path, cache):
    local = os.path.join(cache, path.replace("/", "__"))
    if not os.path.exists(local):
        with urllib.request.urlopen(RAW + urllib.parse.quote(path), timeout=60) as r:
            data = r.read()
        with open(local, "wb") as fh:
            fh.write(data)
    with open(local, "rb") as fh:
        return fh.read()


def list_cht(cache):
    local = os.path.join(cache, "tree-" + SHA + ".json")
    if not os.path.exists(local):
        url = "https://api.github.com/repos/libretro/libretro-database/git/trees/" + SHA + "?recursive=1"
        with urllib.request.urlopen(url, timeout=60) as r:
            open(local, "wb").write(r.read())
    tree = json.load(open(local))
    return sorted(t["path"] for t in tree["tree"] if t["path"].startswith(CHT_DIR) and t["type"] == "blob")


def parse_dat(text):
    games = []
    for block in re.findall(r"game \((.*?)\n\)", text, re.S):
        name = re.search(r'\bname "([^"]*)"', block)
        crc = re.search(r"\bcrc ([0-9A-Fa-f]{8})", block)
        serial = re.search(r'\bserial "([^"]*)"', block)
        if name and crc:
            games.append((crc.group(1).upper(), (serial.group(1) if serial else "").strip()[:4], name.group(1)))
    return games


def regroup(raw):
    tokens = [t for t in re.split(r"[+\s,;]+", raw.upper()) if t]
    out, i = [], 0
    while i < len(tokens):
        t = tokens[i]
        nxt = tokens[i + 1] if i + 1 < len(tokens) else None
        if ":" in t:
            out.append(t)
        elif len(t) in (16, 12):
            out.append(t[:8] + " " + t[8:])
        elif len(t) == 8 and nxt and ":" not in nxt and len(nxt) in (8, 4):
            out.append(t + " " + nxt)
            i += 1
        else:
            out.append(t)
        i += 1
    return out


def valid(lines):
    return bool(lines) and all(RAW_RE.match(l) or CB_RE.match(l) or GS_RE.match(l) for l in lines)


def parse_cht(text):
    desc, code = {}, {}
    for line in text.splitlines():
        m = re.match(r'^\s*cheat(\d+)_(desc|code)\s*=\s*"?(.*?)"?\s*$', line)
        if m:
            (desc if m.group(2) == "desc" else code)[int(m.group(1))] = m.group(3)
    out, dropped = [], 0
    for i in sorted(code):
        lines = regroup(code[i])
        if not valid(lines):
            dropped += 1
            continue
        name = re.sub(r"\s+", " ", desc.get(i, "").replace('\\"', '"')).strip()[:60] or "Cheat %d" % (i + 1)
        out.append({"name": name, "code": "\n".join(lines), "master": bool(MASTER_RE.search(name))})
    return out, dropped


def norm(s):
    return re.sub(r"[^a-z0-9]", "", s.lower().replace("&", "and"))


def base_title(s):
    return norm(re.sub(r"\s*[\(\[][^\)\]]*[\)\]]", "", s))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cache", default="/tmp/lrdb-cache")
    ap.add_argument("--engine-check", default="/tmp/retra-host-build/retra-headless",
                    help="retra-headless used to confirm the engine accepts every code (skipped if missing)")
    args = ap.parse_args()
    os.makedirs(args.cache, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)

    dat = parse_dat(fetch("metadat/no-intro/Nintendo - Game Boy Advance.dat", args.cache).decode("utf-8", "replace"))
    dat.sort()
    tsv = "crc32\tserial\tname\n" + "".join("%s\t%s\t%s\n" % g for g in dat)
    write_gz(os.path.join(OUT, "gba_games.tsv.gz"), tsv.encode())

    by_name = {}
    by_base = {}
    for crc, serial, name in dat:
        by_name.setdefault(norm(name), []).append((crc, serial, name))
        by_base.setdefault(base_title(name), []).append((crc, serial, name))

    paths = list_cht(args.cache)
    with concurrent.futures.ThreadPoolExecutor(12) as ex:
        texts = list(ex.map(lambda p: fetch(p, args.cache).decode("utf-8", "replace"), paths))

    games, audit, dropped_total, unmatched = [], [], 0, []
    for path, text in zip(paths, texts):
        fname = os.path.basename(path)
        title = fname[:-4] if fname.endswith(".cht") else fname
        cheats, dropped = parse_cht(text)
        dropped_total += dropped
        if not cheats:
            continue
        entry = {"title": title, "crc32s": [], "serials": [], "catalogIds": [], "cheats": cheats}
        if title in HACKS:
            entry["catalogIds"] = HACKS[title]
            how = "hack"
        else:
            clean = TAG_RE.sub("", title).strip()
            hits = by_name.get(norm(clean))
            how = "exact"
            if not hits:
                cands = by_base.get(base_title(clean), [])
                region = re.findall(r"\(([^)]*)\)", clean)
                if region and cands:
                    want = norm(region[0])
                    narrowed = [c for c in cands if want and want in norm(c[2])]
                    cands = narrowed or cands
                serials = {c[1] for c in cands if c[1]}
                if len(serials) == 1:
                    hits, how = cands, "title"
                else:
                    us = [c for c in cands if "(USA" in c[2]]
                    hits, how = (us, "title-usa") if len({c[1] for c in us}) == 1 else ([], "unmatched")
            if hits:
                entry["title"] = hits[0][2] if len(hits) == 1 else clean
                entry["crc32s"] = sorted({h[0] for h in hits})
                entry["serials"] = sorted({h[1] for h in hits if h[1]})
            else:
                unmatched.append(title)
                continue
        games.append(entry)
        audit.append((how, title, ",".join(entry["serials"] or entry["catalogIds"]), len(cheats)))

    engine_rejected = engine_check(args.engine_check, games)
    if engine_rejected:
        for g in games:
            g["cheats"] = [c for c in g["cheats"] if c["code"] not in engine_rejected]
        games = [g for g in games if g["cheats"]]

    pack = {"schema": 1, "source": "libretro-database " + SHA, "license": "CC-BY-SA-4.0", "games": games}
    data = json.dumps(pack, ensure_ascii=False, separators=(",", ":"), sort_keys=False).encode()
    write_gz(os.path.join(OUT, "gba_cheats.json.gz"), data)

    for row in audit:
        if "Pokemon" in row[1] or row[0] != "exact":
            print("%-10s %-70s %-12s %d" % row)
    print("games index: %d rows" % len(dat))
    print("cheat books: %d games, %d cheats, %d codes dropped (unsupported syntax), %d engine-rejected, %d files unmatched"
          % (len(games), sum(len(g["cheats"]) for g in games), dropped_total, len(engine_rejected), len(unmatched)))
    for u in unmatched:
        print("  unmatched:", u)


def engine_check(binary, games):
    """Asks the real engine to parse every code; returns the codes it doesn't fully accept."""
    if not os.path.exists(binary):
        print("engine check skipped (no %s)" % binary)
        return set()
    import subprocess
    rom = os.path.join(ROOT, "app", "src", "main", "assets", "games", "mochi_hop.gba")

    def accepted(codes):
        cmd = [binary, rom, "--frames", "1"]
        for code in codes:
            cmd += ["--cheat", code]
        r = subprocess.run(cmd, capture_output=True, text=True)
        m = re.search(r"cheats accepted: (\d+)", r.stderr + r.stdout)
        return int(m.group(1)) if m else -1

    codes = sorted({c["code"] for g in games for c in g["cheats"]})
    rejected = set()
    for i in range(0, len(codes), 100):
        chunk = codes[i:i + 100]
        if accepted(chunk) == sum(c.count("\n") + 1 for c in chunk):
            continue
        for code in chunk:  # find the offenders one by one
            if accepted([code]) != code.count("\n") + 1:
                rejected.add(code)
    return rejected


def write_gz(path, data):
    buf = io.BytesIO()
    with gzip.GzipFile(filename="", mode="wb", fileobj=buf, mtime=0, compresslevel=9) as gz:
        gz.write(data)
    with open(path, "wb") as fh:
        fh.write(buf.getvalue())
    print("wrote %s (%d bytes)" % (os.path.relpath(path, ROOT), len(buf.getvalue())))


if __name__ == "__main__":
    sys.exit(main())
