// retra-headless: host runner for the Retra GBA core (tests, screenshots, benchmarks).
//
//   retra-headless rom.gba [--frames N] [--out shot.png] [--keys script] [--press SPEC]
//                  [--bench] [--save file.sav] [--bios bios.bin] [--wav out.wav]
//                  [--hash] [--state-test] [--cheat CODE]... [--color-correct]
//
// Key script: one entry per line "FROM[-TO] KEY[+KEY...]" (frames, inclusive; a single
// frame number presses for 6 frames). --press takes the same entries separated by ','.
// Key names: A B SELECT START RIGHT LEFT UP DOWN R L.
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <fstream>
#include <memory>
#include <sstream>
#include <string>
#include <vector>

#include "gba.h"

using namespace retra;

namespace {

std::vector<u8> readFile(const std::string& path) {
    std::ifstream f(path, std::ios::binary);
    if (!f) return {};
    return std::vector<u8>((std::istreambuf_iterator<char>(f)), std::istreambuf_iterator<char>());
}

bool writeFile(const std::string& path, const std::vector<u8>& d) {
    std::ofstream f(path, std::ios::binary);
    if (!f) return false;
    f.write((const char*)d.data(), (std::streamsize)d.size());
    return (bool)f;
}

u32 crc32(const u8* d, size_t n, u32 crc = 0) {
    static u32 table[256];
    static bool init = false;
    if (!init) {
        for (u32 i = 0; i < 256; i++) {
            u32 c = i;
            for (int k = 0; k < 8; k++) c = (c & 1) ? 0xEDB88320u ^ (c >> 1) : c >> 1;
            table[i] = c;
        }
        init = true;
    }
    crc = ~crc;
    for (size_t i = 0; i < n; i++) crc = table[(crc ^ d[i]) & 0xFF] ^ (crc >> 8);
    return ~crc;
}

void put32be(std::vector<u8>& v, u32 x) {
    v.push_back((u8)(x >> 24));
    v.push_back((u8)(x >> 16));
    v.push_back((u8)(x >> 8));
    v.push_back((u8)x);
}

void pngChunk(std::vector<u8>& out, const char* type, const std::vector<u8>& data) {
    put32be(out, (u32)data.size());
    std::vector<u8> td(type, type + 4);
    td.insert(td.end(), data.begin(), data.end());
    out.insert(out.end(), td.begin(), td.end());
    put32be(out, crc32(td.data(), td.size()));
}

// Minimal PNG encoder (zlib stored blocks, no compression).
std::vector<u8> encodePng(const u32* argb, int w, int h) {
    std::vector<u8> raw;
    raw.reserve((size_t)(w * 3 + 1) * h);
    for (int y = 0; y < h; y++) {
        raw.push_back(0);
        for (int x = 0; x < w; x++) {
            u32 p = argb[y * w + x];
            raw.push_back((u8)(p >> 16));
            raw.push_back((u8)(p >> 8));
            raw.push_back((u8)p);
        }
    }
    std::vector<u8> z = {0x78, 0x01};
    size_t pos = 0;
    while (pos < raw.size()) {
        size_t n = raw.size() - pos;
        if (n > 65535) n = 65535;
        const bool last = pos + n == raw.size();
        z.push_back(last ? 1 : 0);
        z.push_back((u8)n);
        z.push_back((u8)(n >> 8));
        z.push_back((u8)~n);
        z.push_back((u8)(~n >> 8));
        z.insert(z.end(), raw.begin() + (long)pos, raw.begin() + (long)(pos + n));
        pos += n;
    }
    u32 a = 1, b = 0;
    for (u8 c : raw) {
        a = (a + c) % 65521;
        b = (b + a) % 65521;
    }
    put32be(z, (b << 16) | a);
    std::vector<u8> out = {0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    std::vector<u8> ihdr;
    put32be(ihdr, (u32)w);
    put32be(ihdr, (u32)h);
    ihdr.insert(ihdr.end(), {8, 2, 0, 0, 0});
    pngChunk(out, "IHDR", ihdr);
    pngChunk(out, "IDAT", z);
    pngChunk(out, "IEND", {});
    return out;
}

void writeWav(const std::string& path, const std::vector<s16>& samples) {
    std::vector<u8> d;
    auto p32 = [&](u32 v) {
        for (int i = 0; i < 4; i++) d.push_back((u8)(v >> (8 * i)));
    };
    auto p16 = [&](u16 v) {
        d.push_back((u8)v);
        d.push_back((u8)(v >> 8));
    };
    const u32 bytes = (u32)samples.size() * 2;
    d.insert(d.end(), {'R', 'I', 'F', 'F'});
    p32(36 + bytes);
    d.insert(d.end(), {'W', 'A', 'V', 'E', 'f', 'm', 't', ' '});
    p32(16);
    p16(1);
    p16(2);
    p32(kSampleRate);
    p32(kSampleRate * 4);
    p16(4);
    p16(16);
    d.insert(d.end(), {'d', 'a', 't', 'a'});
    p32(bytes);
    for (s16 s : samples) p16((u16)s);
    writeFile(path, d);
}

struct KeyEvent {
    int from, to;
    u16 mask;
};

u16 keyMask(const std::string& name) {
    static const char* names[] = {"A", "B", "SELECT", "START", "RIGHT", "LEFT", "UP", "DOWN", "R", "L"};
    for (int i = 0; i < 10; i++)
        if (name == names[i]) return (u16)(1u << i);
    return 0;
}

bool parseKeyEntry(const std::string& entry, std::vector<KeyEvent>& out) {
    std::istringstream is(entry);
    std::string range, keys;
    if (!(is >> range >> keys)) return false;
    KeyEvent ev{};
    size_t dash = range.find('-');
    if (dash == std::string::npos) {
        ev.from = std::atoi(range.c_str());
        ev.to = ev.from + 5;
    } else {
        ev.from = std::atoi(range.substr(0, dash).c_str());
        ev.to = std::atoi(range.substr(dash + 1).c_str());
    }
    std::string k;
    std::istringstream ks(keys);
    while (std::getline(ks, k, '+')) ev.mask |= keyMask(k);
    out.push_back(ev);
    return true;
}

u64 fnv1a(const u32* p, size_t n) {
    u64 h = 1469598103934665603ull;
    for (size_t i = 0; i < n; i++) {
        h ^= p[i];
        h *= 1099511628211ull;
    }
    return h;
}

void usage() {
    std::fprintf(stderr,
                 "usage: retra-headless rom.gba [--frames N] [--out shot.png] [--keys script] [--press SPEC]\n"
                 "       [--bench] [--save file.sav] [--bios bios.bin] [--wav out.wav] [--hash]\n"
                 "       [--state-test] [--cheat CODE] [--color-correct] [--shots DIR:EVERY] [--debug-log]\n");
}

}  // namespace

int main(int argc, char** argv) {
    if (argc < 2) {
        usage();
        return 2;
    }
    std::string romPath = argv[1], outPath, keysPath, savePath, biosPath, wavPath, shotsSpec;
    int frames = 60;
    bool bench = false, hash = false, stateTest = false, colorCorrect = false, debugLog = false;
    std::vector<KeyEvent> keys;
    std::vector<std::string> cheats;
    for (int i = 2; i < argc; i++) {
        std::string a = argv[i];
        auto next = [&]() -> std::string {
            if (i + 1 >= argc) {
                usage();
                std::exit(2);
            }
            return argv[++i];
        };
        if (a == "--frames") frames = std::atoi(next().c_str());
        else if (a == "--out") outPath = next();
        else if (a == "--keys") keysPath = next();
        else if (a == "--press") {
            std::string spec = next(), e;
            std::istringstream ss(spec);
            while (std::getline(ss, e, ',')) {
                for (char& c : e)
                    if (c == ':') c = ' ';
                parseKeyEntry(e, keys);
            }
        } else if (a == "--bench") bench = true;
        else if (a == "--save") savePath = next();
        else if (a == "--bios") biosPath = next();
        else if (a == "--wav") wavPath = next();
        else if (a == "--hash") hash = true;
        else if (a == "--state-test") stateTest = true;
        else if (a == "--cheat") cheats.push_back(next());
        else if (a == "--color-correct") colorCorrect = true;
        else if (a == "--debug-log") debugLog = true;
        else if (a == "--shots") shotsSpec = next();
        else {
            usage();
            return 2;
        }
    }
    if (!keysPath.empty()) {
        std::ifstream f(keysPath);
        std::string line;
        while (std::getline(f, line)) {
            if (line.empty() || line[0] == '#') continue;
            parseKeyEntry(line, keys);
        }
    }

    std::vector<u8> rom = readFile(romPath);
    if (rom.empty()) {
        std::fprintf(stderr, "cannot read %s\n", romPath.c_str());
        return 1;
    }
    std::vector<u8> bios;
    if (!biosPath.empty()) bios = readFile(biosPath);

    auto gba = std::make_unique<Gba>();
    std::string err;
    if (!gba->loadRom(rom.data(), rom.size(), bios.empty() ? nullptr : bios.data(), bios.size(), err)) {
        std::fprintf(stderr, "load failed: %s\n", err.c_str());
        return 1;
    }
    const RomHeaderInfo& info = gba->info();
    std::fprintf(stderr, "ROM: '%s' code=%s maker=%s v%d save=%d rtc=%d\n", info.title.c_str(),
                 info.gameCode.c_str(), info.makerCode.c_str(), info.version, (int)info.saveKind, info.hasRtc);
    if (!savePath.empty()) {
        std::vector<u8> sav = readFile(savePath);
        if (!sav.empty()) gba->loadBatterySave(sav.data(), sav.size());
    }
    if (!cheats.empty()) std::fprintf(stderr, "cheats accepted: %d\n", gba->setCheats(cheats));
    gba->setColorCorrection(colorCorrect);
    if (debugLog) {
        gba->bus.debugCallback = [](void*, int level, const char* msg) { std::printf("[dbg%d] %s\n", level, msg); };
    }

    std::string shotsDir;
    int shotsEvery = 0;
    if (!shotsSpec.empty()) {
        size_t c = shotsSpec.rfind(':');
        shotsDir = shotsSpec.substr(0, c);
        shotsEvery = c == std::string::npos ? 60 : std::atoi(shotsSpec.substr(c + 1).c_str());
    }

    std::vector<u32> video(240 * 160);
    std::vector<s16> audio(4096 * 2);
    std::vector<s16> wav;
    u64 totalAudio = 0;
    std::vector<u8> midState;
    std::vector<u32> midFrame;
    const auto t0 = std::chrono::steady_clock::now();
    for (int f = 0; f < frames; f++) {
        u16 mask = 0;
        for (const auto& k : keys)
            if (f >= k.from && f <= k.to) mask |= k.mask;
        gba->setKeys(mask);
        int n = gba->runFrame(video.data(), audio.data(), 4096);
        totalAudio += (u64)n;
        if (!wavPath.empty()) wav.insert(wav.end(), audio.begin(), audio.begin() + n * 2);
        if (shotsEvery > 0 && (f + 1) % shotsEvery == 0) {
            char name[512];
            std::snprintf(name, sizeof(name), "%s/frame_%05d.png", shotsDir.c_str(), f + 1);
            writeFile(name, encodePng(video.data(), 240, 160));
        }
        if (stateTest && f == frames / 2) midState = gba->saveState();
    }
    const auto t1 = std::chrono::steady_clock::now();
    const double secs = std::chrono::duration<double>(t1 - t0).count();
    if (bench) {
        std::printf("frames=%d time=%.3fs fps=%.1f (%.1fx realtime) audio_frames=%llu (%.1f/frame)\n", frames, secs,
                    frames / secs, frames / secs / 59.7275, (unsigned long long)totalAudio,
                    frames ? (double)totalAudio / frames : 0.0);
    }
    if (stateTest && !midState.empty()) {
        // Re-run the second half from the saved state and compare final frames.
        std::vector<u32> endFrame = video;
        if (!gba->loadState(midState.data(), midState.size())) {
            std::printf("state-test: FAIL (loadState rejected)\n");
            return 1;
        }
        for (int f = frames / 2 + 1; f < frames; f++) {
            u16 mask = 0;
            for (const auto& k : keys)
                if (f >= k.from && f <= k.to) mask |= k.mask;
            gba->setKeys(mask);
            gba->runFrame(video.data(), audio.data(), 4096);
        }
        const bool same = endFrame == video;
        std::printf("state-test: %s (state %zu bytes)\n", same ? "PASS" : "FAIL", midState.size());
        if (!same) return 1;
    }
    if (hash) std::printf("hash=%016llx\n", (unsigned long long)fnv1a(video.data(), video.size()));
    if (!outPath.empty()) {
        if (!writeFile(outPath, encodePng(video.data(), 240, 160))) {
            std::fprintf(stderr, "cannot write %s\n", outPath.c_str());
            return 1;
        }
    }
    if (!wavPath.empty()) writeWav(wavPath, wav);
    if (!savePath.empty() && gba->hasBattery() && gba->consumeSaveDirty()) {
        writeFile(savePath, gba->batterySave());
    }
    return 0;
}
