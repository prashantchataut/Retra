// Retra GBA core - cheat engine.
//  Raw:          "AAAAAAAA:VV" / "AAAAAAAA:VVVV" / "AAAAAAAA:VVVVVVVV" (also "AAAAAAAA VV")
//  CodeBreaker:  "XXXXXXXX YYYY" (unencrypted)
//  GameShark v1/v2 and Pro Action Replay v3: "XXXXXXXX YYYYYYYY" (encrypted, auto-detected;
//                force with a leading "GS:" / "AR:" / "PAR:" / "ARV3:" tag).
#include "cheats.h"

#include <cctype>
#include <cstdlib>

#include "bus_inl.h"
#include "gba.h"

namespace retra {

const u32 Cheats::kGsSeeds[4] = {0x09F4FBBD, 0x9681884A, 0x352027E9, 0xF3DEE5A7};
const u32 Cheats::kAr3Seeds[4] = {0x7AA9648F, 0x7FAE6994, 0xC0EFAAD5, 0x42712C57};

void Cheats::gsDecrypt(u32& a, u32& v, const u32 s[4]) {
    u32 sum = 0xC6EF3720;
    for (int i = 0; i < 32; i++) {
        v -= ((a << 4) + s[2]) ^ (a + sum) ^ ((a >> 5) + s[3]);
        a -= ((v << 4) + s[0]) ^ (v + sum) ^ ((v >> 5) + s[1]);
        sum -= 0x9E3779B9;
    }
}

namespace {

enum Forced { F_AUTO = 0, F_GS = 1, F_AR3 = 2, F_CB = 3, F_RAW = 4 };

bool isHex(char c) { return std::isxdigit((unsigned char)c) != 0; }

bool validRamAddr(u32 a) {
    const u32 r = a >> 24;
    return r == 0x02 || r == 0x03 || r == 0x04 || r == 0x05 || r == 0x06 || r == 0x07;
}

// Plausibility of a decrypted GameShark v1/v2 code.
bool plausibleGs(u32 a, u32 v) {
    if (v == 0x001DC0DE) return true;
    if (a == 0xDEADFACE) return false;
    switch (a >> 28) {
        case 0x0: return validRamAddr(a & 0x0FFFFFFF) && v <= 0xFF;
        case 0x1: return validRamAddr(a & 0x0FFFFFFF) && v <= 0xFFFF;
        case 0x2: return validRamAddr(a & 0x0FFFFFFF);
        case 0x3: return (a & 0x0FFF0000) == 0;
        case 0x6: return (a & 0x0F000000) == 0;
        case 0xD: return validRamAddr(a & 0x0FFFFFFF) && v <= 0xFFFF;
        case 0xE: return validRamAddr(v & 0x0FFFFFFF);
        case 0xF: return ((a & 0x0FFFFFFF) >> 24) >= 8 && ((a & 0x0FFFFFFF) >> 24) <= 9;
        default: return false;
    }
}

u32 ar3Addr(u32 a) { return ((a & 0x00F00000) << 4) | (a & 0x000FFFFF); }

bool plausibleAr3(u32 a, u32 v) {
    if (v == 0x001DC0DE) return true;
    if (a == 0) return true;  // special codes
    const u32 t = a >> 24;
    switch (t) {
        case 0x00:
        case 0x02:
        case 0x04:
        case 0x40:
        case 0x42:
        case 0x44:
        case 0x80:
        case 0x82:
        case 0x84: return validRamAddr(ar3Addr(a));
        case 0xC4:
        case 0xC6:
        case 0xC7: return true;
        default:
            if ((a & 0x38000000) != 0) return validRamAddr(ar3Addr(a));
            return false;
    }
}

}  // namespace

bool Cheats::parseLine(const std::string& lineIn, int forced, std::vector<Code>& out, bool& cbEncrypted) {
    // Extract hex digit groups.
    std::string line;
    for (char ch : lineIn) line.push_back((char)std::toupper((unsigned char)ch));
    const bool hasColon = line.find(':') != std::string::npos;
    std::vector<std::string> groups;
    std::string cur;
    for (char ch : line) {
        if (isHex(ch)) {
            cur.push_back(ch);
        } else {
            if (!cur.empty()) groups.push_back(cur);
            cur.clear();
        }
    }
    if (!cur.empty()) groups.push_back(cur);
    if (groups.size() == 1 && groups[0].size() == 16) {
        groups = {groups[0].substr(0, 8), groups[0].substr(8)};
    }
    if (groups.size() != 2 || groups[0].size() != 8) return false;
    const std::string& vs = groups[1];
    if (vs.size() != 2 && vs.size() != 4 && vs.size() != 6 && vs.size() != 8) return false;
    const u32 a = (u32)std::strtoul(groups[0].c_str(), nullptr, 16);
    const u32 v = (u32)std::strtoul(vs.c_str(), nullptr, 16);

    if (forced == F_RAW || (hasColon && forced == F_AUTO) || (vs.size() == 2 && forced == F_AUTO)) {
        if (vs.size() == 6) return false;
        Code c{Kind::RAW, a, v};
        // encode width in the top bits of the kind-specific value: keep width via digit count
        c.a = a;
        c.v = v;
        out.push_back(c);
        out.back().kind = Kind::RAW;
        // width marker stored separately in high byte of a? Use digit count encoded in vector below.
        rawWidth.push_back((u8)(vs.size() / 2));
        return true;
    }
    if (vs.size() == 4 || vs.size() == 6 || forced == F_CB) {
        // CodeBreaker (6-digit values only appear in multi-line parameter rows)
        if (cbEncrypted) return false;
        if ((a >> 28) == 0x9) {
            cbEncrypted = true;  // encrypted CodeBreaker lists are not supported
            return false;
        }
        out.push_back(Code{Kind::CB, a, v});
        rawWidth.push_back(0);
        return true;
    }
    // 16 digit encrypted codes
    u32 ga = a, gv = v;
    gsDecrypt(ga, gv, kGsSeeds);
    u32 ra = a, rv = v;
    gsDecrypt(ra, rv, kAr3Seeds);
    if (forced == F_GS || (forced == F_AUTO && plausibleGs(ga, gv))) {
        out.push_back(Code{Kind::GS, ga, gv});
        rawWidth.push_back(0);
        return true;
    }
    if (forced == F_AR3 || (forced == F_AUTO && plausibleAr3(ra, rv))) {
        out.push_back(Code{Kind::AR3, ra, rv});
        rawWidth.push_back(0);
        return true;
    }
    // Possibly an already-decrypted (raw) GameShark code.
    if (forced == F_AUTO && plausibleGs(a, v)) {
        out.push_back(Code{Kind::GS, a, v});
        rawWidth.push_back(0);
        return true;
    }
    return false;
}

int Cheats::set(const std::vector<std::string>& codes) {
    restoreRomPatches();
    list.clear();
    rawWidth.clear();
    int accepted = 0;
    for (const std::string& entry : codes) {
        int forced = F_AUTO;
        bool cbEncrypted = false;
        size_t start = 0;
        std::string s = entry;
        // optional tag
        auto stripTag = [&](const char* tag, int f) {
            const size_t n = std::strlen(tag);
            if (s.size() >= n) {
                std::string head;
                for (size_t i = 0; i < n; i++) head.push_back((char)std::toupper((unsigned char)s[i]));
                if (head == tag) {
                    forced = f;
                    s = s.substr(n);
                    return true;
                }
            }
            return false;
        };
        while (!s.empty() && std::isspace((unsigned char)s[0])) s.erase(0, 1);
        stripTag("ARV3:", F_AR3) || stripTag("PAR:", F_AR3) || stripTag("AR:", F_AR3) || stripTag("GS:", F_GS) ||
            stripTag("CB:", F_CB) || stripTag("RAW:", F_RAW);
        // split into lines
        std::vector<std::string> lines;
        size_t pos = start;
        while (pos <= s.size()) {
            size_t e = s.find_first_of("\n\r;,", pos);
            if (e == std::string::npos) e = s.size();
            std::string line = s.substr(pos, e - pos);
            bool hasHex = false;
            for (char ch : line) hasHex |= isHex(ch);
            if (hasHex) lines.push_back(line);
            pos = e + 1;
        }
        int forced16 = forced;
        auto isEncryptedLine = [](const std::string& l) {
            int n = 0;
            for (char ch : l) n += isHex(ch) ? 1 : 0;
            return n == 16 && l.find(':') == std::string::npos;
        };
        if (forced == F_AUTO) {
            // Encrypted 16-digit codes: pick GameShark v1/v2 or Action Replay v3 for the whole
            // entry, whichever decrypts to more plausible codes (master/ID codes tip the scale).
            int gsScore = 0, arScore = 0, encrypted = 0;
            for (const std::string& l : lines) {
                std::string hex;
                for (char ch : l)
                    if (isHex(ch)) hex.push_back(ch);
                if (hex.size() != 16 || l.find(':') != std::string::npos) continue;
                encrypted++;
                u32 a = (u32)std::strtoul(hex.substr(0, 8).c_str(), nullptr, 16);
                u32 v = (u32)std::strtoul(hex.substr(8).c_str(), nullptr, 16);
                u32 ga = a, gv = v, ra = a, rv = v;
                gsDecrypt(ga, gv, kGsSeeds);
                gsDecrypt(ra, rv, kAr3Seeds);
                gsScore += plausibleGs(ga, gv) ? (gv == 0x001DC0DE ? 4 : 1) : 0;
                arScore += plausibleAr3(ra, rv) ? (rv == 0x001DC0DE ? 4 : 1) : 0;
            }
            if (encrypted > 1 && (gsScore || arScore)) forced16 = arScore > gsScore ? F_AR3 : F_GS;
        }
        for (const std::string& l : lines)
            if (parseLine(l, isEncryptedLine(l) ? forced16 : forced, list, cbEncrypted)) accepted++;
    }
    return accepted;
}

void Cheats::restoreRomPatches() {
    Bus& b = g->bus;
    for (auto it = romPatches.rbegin(); it != romPatches.rend(); ++it) {
        if (it->offset + 1 < b.rom.size()) storeLE<u16>(&b.rom[it->offset], it->original);
    }
    romPatches.clear();
}

void Cheats::romPatch(u32 offset, u16 value) {
    Bus& b = g->bus;
    offset &= 0x1FFFFFE;
    if (offset + 1 >= b.rom.size()) return;
    for (const auto& p : romPatches)
        if (p.offset == offset) {
            storeLE<u16>(&b.rom[offset], value);
            return;
        }
    romPatches.push_back(RomPatch{offset, loadLE<u16>(&b.rom[offset])});
    storeLE<u16>(&b.rom[offset], value);
}

void Cheats::apply() {
    size_t i = 0;
    while (i < list.size()) {
        switch (list[i].kind) {
            case Kind::RAW: {
                Bus& b = g->bus;
                const Code& c = list[i];
                const u8 w = rawWidth[i];
                if (w == 1) b.poke<u8>(c.a, (u8)c.v);
                else if (w == 2) b.poke<u16>(c.a, (u16)c.v);
                else b.poke<u32>(c.a, c.v);
                i++;
                break;
            }
            case Kind::CB: runCodeBreaker(i); break;
            case Kind::GS: runGameShark(i); break;
            case Kind::AR3: runAr3(i); break;
        }
        if (stopAll) {
            stopAll = false;
            return;
        }
    }
}

void Cheats::runCodeBreaker(size_t& i) {
    Bus& b = g->bus;
    const Code c = list[i];
    const u32 addr = c.a & 0x0FFFFFFF;
    const u16 v = (u16)c.v;
    i++;
    auto skipNext = [&](bool cond) {
        if (!cond && i < list.size()) i++;
    };
    switch (c.a >> 28) {
        case 0x0:
        case 0x1:
        case 0x9: break;  // game id / hook / encryption seed
        case 0x2: b.poke<u16>(addr, (u16)(b.peek<u16>(addr) | v)); break;
        case 0x3: b.poke<u8>(addr, (u8)v); break;
        case 0x4: {
            if (i >= list.size()) break;
            const Code p = list[i++];
            const u32 inc = p.a >> 16, count = p.a & 0xFFFF, step = p.v & 0xFFFF;
            for (u32 k = 0; k < count; k++) b.poke<u16>(addr + k * step, (u16)(v + k * inc));
            break;
        }
        case 0x5: {
            u32 count = v;
            u32 a = addr;
            while (count && i < list.size()) {
                const Code p = list[i++];
                const u8 bytes[6] = {(u8)(p.a >> 24), (u8)(p.a >> 16), (u8)(p.a >> 8),
                                     (u8)p.a,         (u8)(p.v >> 8),  (u8)p.v};
                for (int k = 0; k < 6 && count; k++, count--) b.poke<u8>(a++, bytes[k]);
            }
            break;
        }
        case 0x6: b.poke<u16>(addr, (u16)(b.peek<u16>(addr) & v)); break;
        case 0x7: skipNext(b.peek<u16>(addr) == v); break;
        case 0x8: b.poke<u16>(addr, v); break;
        case 0xA: skipNext(b.peek<u16>(addr) != v); break;
        case 0xB: skipNext((s16)b.peek<u16>(addr) > (s16)v); break;
        case 0xC: skipNext((s16)b.peek<u16>(addr) < (s16)v); break;
        case 0xD: skipNext((g->keyInput & v) == 0); break;
        case 0xE: b.poke<u16>(addr, (u16)(b.peek<u16>(addr) + v)); break;
        case 0xF: skipNext((b.peek<u16>(addr) & v) != 0); break;
    }
}

void Cheats::runGameShark(size_t& i) {
    Bus& b = g->bus;
    const Code c = list[i];
    i++;
    if (c.v == 0x001DC0DE) return;  // game id
    const u32 addr = c.a & 0x0FFFFFFF;
    switch (c.a >> 28) {
        case 0x0: b.poke<u8>(addr, (u8)c.v); break;
        case 0x1: b.poke<u16>(addr, (u16)c.v); break;
        case 0x2: b.poke<u32>(addr, c.v); break;
        case 0x3: {
            u32 count = c.a & 0xFFFF;
            const u32 value = c.v;
            while (count && i < list.size()) {
                const Code p = list[i++];
                b.poke<u32>(p.a & 0x0FFFFFFF, value);
                if (--count && p.v) {
                    b.poke<u32>(p.v & 0x0FFFFFFF, value);
                    --count;
                }
            }
            break;
        }
        case 0x6: romPatch((c.a & 0x00FFFFFF) * 2, (u16)c.v); break;
        case 0xD:
            if (b.peek<u16>(addr) != (u16)c.v && i < list.size()) i++;
            break;
        case 0xE: {
            const u32 n = (c.a >> 16) & 0xFF;
            if (b.peek<u16>(c.v & 0x0FFFFFFF) != (u16)(c.a & 0xFFFF)) i += n;
            break;
        }
        default: break;  // hook, button codes, seeds: ignored
    }
}

void Cheats::runAr3(size_t& i) {
    Bus& b = g->bus;
    const Code c = list[i];
    i++;
    if (c.v == 0x001DC0DE) return;
    if (c.a == 0) {
        // special codes
        const u32 t = c.v >> 24;
        if (c.v == 0) {
            stopAll = true;  // end of list
            return;
        }
        if (c.v == 0x60000000 || c.v == 0x40000000) return;  // ELSE / ENDIF markers
        if (i >= list.size()) return;
        const Code p = list[i++];
        const u32 addr = ar3Addr(c.v & 0x00FFFFFF);
        switch (t) {
            case 0x18:
            case 0x1A:
            case 0x1C:
            case 0x1E: romPatch((c.v & 0x00FFFFFF) * 2, (u16)p.a); break;
            case 0x80:
            case 0x82:
            case 0x84: {
                // slide: p.a = value, p.v = ssccssss
                u32 value = p.a;
                const u32 vinc = p.v >> 24, count = (p.v >> 16) & 0xFF, ainc = p.v & 0xFFFF;
                u32 a = addr;
                for (u32 k = 0; k < count; k++) {
                    if (t == 0x80) {
                        b.poke<u8>(a, (u8)value);
                        a += ainc;
                    } else if (t == 0x82) {
                        b.poke<u16>(a, (u16)value);
                        a += ainc * 2;
                    } else {
                        b.poke<u32>(a, value);
                        a += ainc * 4;
                    }
                    value += vinc;
                }
                break;
            }
            default: break;  // button codes ignored
        }
        return;
    }
    const u32 t = c.a >> 24;
    const u32 addr = ar3Addr(c.a);
    if ((c.a & 0x38000000) == 0 || t == 0xC4 || t == 0xC6 || t == 0xC7) {
        switch (t) {
            case 0x00: {
                const u32 n = (c.v >> 8) + 1;
                for (u32 k = 0; k < n && k < 0x10000; k++) b.poke<u8>(addr + k, (u8)c.v);
                break;
            }
            case 0x02: {
                const u32 n = (c.v >> 16) + 1;
                for (u32 k = 0; k < n; k++) b.poke<u16>(addr + k * 2, (u16)c.v);
                break;
            }
            case 0x04: b.poke<u32>(addr, c.v); break;
            case 0x40: b.poke<u8>(b.peek<u32>(addr) + (c.v >> 8), (u8)c.v); break;
            case 0x42: b.poke<u16>(b.peek<u32>(addr) + (c.v >> 16) * 2, (u16)c.v); break;
            case 0x44: b.poke<u32>(b.peek<u32>(addr), c.v); break;
            case 0x80: b.poke<u8>(addr, (u8)(b.peek<u8>(addr) + c.v)); break;
            case 0x82: b.poke<u16>(addr, (u16)(b.peek<u16>(addr) + c.v)); break;
            case 0x84: b.poke<u32>(addr, b.peek<u32>(addr) + c.v); break;
            case 0xC6: b.poke<u16>(0x04000000 | (c.a & 0x00FFFFFF), (u16)c.v); break;
            case 0xC7: b.poke<u32>(0x04000000 | (c.a & 0x00FFFFFF), c.v); break;
            default: break;
        }
        return;
    }
    // conditional
    const u32 cond = c.a & 0x38000000;
    const u32 size = (c.a >> 25) & 3;
    const u32 act = c.a & 0xC0000000;
    u32 cur, val;
    if (size == 0) {
        cur = b.peek<u8>(addr);
        val = c.v & 0xFF;
    } else if (size == 1) {
        cur = b.peek<u16>(addr);
        val = c.v & 0xFFFF;
    } else if (size == 2) {
        cur = b.peek<u32>(addr);
        val = c.v;
    } else {
        cur = 0;
        val = 0;
    }
    bool res = false;
    if (size != 3) {
        switch (cond >> 27) {
            case 1: res = cur == val; break;
            case 2: res = cur != val; break;
            case 3: res = (s32)cur < (s32)val; break;
            case 4: res = (s32)cur > (s32)val; break;
            case 5: res = cur < val; break;
            case 6: res = cur > val; break;
            case 7: res = (cur & val) != 0; break;
            default: res = false; break;
        }
    }
    if (res) return;
    switch (act) {
        case 0x00000000: i += 1; break;
        case 0x40000000: i += 2; break;
        case 0x80000000:
            while (i < list.size()) {
                const Code& n = list[i++];
                if (n.kind == Kind::AR3 && n.a == 0 && (n.v == 0x60000000 || n.v == 0x40000000)) break;
            }
            break;
        default: stopAll = true; break;
    }
}

}  // namespace retra
