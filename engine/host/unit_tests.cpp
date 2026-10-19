// retra-unit-tests: host unit tests for core features that test ROMs don't cover well
// (cheat formats incl. encrypted GameShark / Action Replay, battery import, state checks).
#include <cstdio>
#include <memory>
#include <string>
#include <vector>

#include "bus_inl.h"
#include "gba.h"

using namespace retra;

static int failures = 0;
#define CHECK(cond)                                                       \
    do {                                                                  \
        if (!(cond)) {                                                    \
            std::printf("  FAIL %s:%d: %s\n", __FILE__, __LINE__, #cond); \
            failures++;                                                   \
        }                                                                 \
    } while (0)

// TEA-style encryption used by GameShark / Action Replay (inverse of Cheats::gsDecrypt).
static void gsEncrypt(u32& a, u32& v, const u32 s[4]) {
    u32 sum = 0;
    for (int i = 0; i < 32; i++) {
        sum += 0x9E3779B9;
        a += ((v << 4) + s[0]) ^ (v + sum) ^ ((v >> 5) + s[1]);
        v += ((a << 4) + s[2]) ^ (a + sum) ^ ((a >> 5) + s[3]);
    }
}

static std::string hex8(u32 v) {
    char b[16];
    std::snprintf(b, sizeof(b), "%08X", v);
    return b;
}

static std::string encrypted(u32 a, u32 v, const u32 seeds[4]) {
    gsEncrypt(a, v, seeds);
    return hex8(a) + " " + hex8(v);
}

// Minimal cartridge: header + "b ." loop, optional save tag.
static std::vector<u8> makeRom(const char* tag, const char* code = "UNIT") {
    std::vector<u8> rom(0x400, 0);
    rom[0] = 0x2E;  // b 0x080000C0
    rom[3] = 0xEA;
    std::memcpy(&rom[0xA0], "UNITTEST", 8);
    std::memcpy(&rom[0xAC], code, 4);
    rom[0xC0] = 0xFE;  // b . (0xEAFFFFFE)
    rom[0xC1] = 0xFF;
    rom[0xC2] = 0xFF;
    rom[0xC3] = 0xEA;
    if (tag) std::memcpy(&rom[0x200], tag, std::strlen(tag));
    return rom;
}

static void testCheats() {
    std::printf("cheats\n");
    auto g = std::make_unique<Gba>();
    std::string err;
    auto rom = makeRom(nullptr);
    CHECK(g->loadRom(rom.data(), rom.size(), nullptr, 0, err));
    std::vector<std::string> codes = {
        "02000010:AB",
        "02000020:1234",
        "02000030:DEADBEEF",
        "82000040 BEEF\n32000043 0077",            // CodeBreaker 16-bit + 8-bit write
        "72000040 BEEF\n82000044 1111",            // CB conditional (true)
        "72000040 0000\n82000046 2222",            // CB conditional (false)
        encrypted(0x22000050, 0xCAFEBABE, Cheats::kGsSeeds),                  // GS 32-bit write
        encrypted(0x12345678, 0x001DC0DE, Cheats::kAr3Seeds) + "\n" +       // AR v3 ID code +
            encrypted(0x04200060, 0x12345678, Cheats::kAr3Seeds),           // 32-bit write
        "GS:" + encrypted(0x02000070, 0x00000042, Cheats::kGsSeeds),          // forced, 8-bit
        "this is not a code",
    };
    const int accepted = g->setCheats(codes);
    CHECK(accepted == 13);
    u32 video[240 * 160];
    s16 audio[4096 * 2];
    g->runFrame(video, audio, 4096);
    Bus& b = g->bus;
    CHECK(b.peek<u8>(0x02000010) == 0xAB);
    CHECK(b.peek<u16>(0x02000020) == 0x1234);
    CHECK(b.peek<u32>(0x02000030) == 0xDEADBEEF);
    CHECK(b.peek<u16>(0x02000040) == 0xBEEF);
    CHECK(b.peek<u8>(0x02000043) == 0x77);
    CHECK(b.peek<u16>(0x02000044) == 0x1111);
    CHECK(b.peek<u16>(0x02000046) == 0x0000);
    CHECK(b.peek<u32>(0x02000050) == 0xCAFEBABE);
    CHECK(b.peek<u32>(0x02000060) == 0x12345678);
    CHECK(b.peek<u8>(0x02000070) == 0x42);
    // Clearing the list stops further writes.
    g->setCheats({});
    b.poke<u8>(0x02000010, 0);
    g->runFrame(video, audio, 4096);
    CHECK(b.peek<u8>(0x02000010) == 0);
}

static void testSaves() {
    std::printf("battery saves\n");
    std::string err;
    {
        auto g = std::make_unique<Gba>();
        auto rom = makeRom("FLASH1M_V103");
        CHECK(g->loadRom(rom.data(), rom.size(), nullptr, 0, err));
        CHECK(g->info().saveKind == SaveKind::FLASH_128K);
        CHECK(g->batterySave().size() == 0x20000);
        std::vector<u8> sav(0x20000, 0x5A);
        CHECK(g->loadBatterySave(sav.data(), sav.size()));
        CHECK(g->batterySave()[0x1FFFF] == 0x5A);
    }
    {
        auto g = std::make_unique<Gba>();
        auto rom = makeRom("FLASH_V126");
        CHECK(g->loadRom(rom.data(), rom.size(), nullptr, 0, err));
        CHECK(g->info().saveKind == SaveKind::FLASH_64K);
        std::vector<u8> sav(0x20000, 1);  // a 128K file upgrades the chip
        CHECK(g->loadBatterySave(sav.data(), sav.size()));
        CHECK(g->batterySave().size() == 0x20000);
    }
    {
        auto g = std::make_unique<Gba>();
        auto rom = makeRom("EEPROM_V124");
        CHECK(g->loadRom(rom.data(), rom.size(), nullptr, 0, err));
        CHECK(g->eepromSizePending());
        std::vector<u8> sav(512, 7);
        CHECK(g->loadBatterySave(sav.data(), sav.size()));
        CHECK(g->batterySave().size() == 512);
        CHECK(!g->eepromSizePending());
    }
    {
        auto g = std::make_unique<Gba>();
        auto rom = makeRom(nullptr);
        CHECK(g->loadRom(rom.data(), rom.size(), nullptr, 0, err));
        CHECK(!g->hasBattery());
        g->bus.poke<u8>(0x0E000010, 0x99);  // untagged game writing SRAM becomes an SRAM cart
        CHECK(g->hasBattery());
        CHECK(g->consumeSaveDirty());
        CHECK(!g->consumeSaveDirty());
        CHECK(g->batterySave()[0x10] == 0x99);
    }
}

static void testStates() {
    std::printf("save states\n");
    std::string err;
    auto a = std::make_unique<Gba>();
    auto b = std::make_unique<Gba>();
    auto romA = makeRom(nullptr, "AAAA");
    auto romB = makeRom(nullptr, "BBBB");
    CHECK(a->loadRom(romA.data(), romA.size(), nullptr, 0, err));
    CHECK(b->loadRom(romB.data(), romB.size(), nullptr, 0, err));
    u32 video[240 * 160];
    s16 audio[4096 * 2];
    a->runFrame(video, audio, 4096);
    std::vector<u8> st = a->saveState();
    CHECK(!b->loadState(st.data(), st.size()));  // other game: rejected
    CHECK(a->loadState(st.data(), st.size()));
    std::vector<u8> trunc(st.begin(), st.begin() + (long)st.size() / 2);
    const u64 before = a->sched.now;
    CHECK(!a->loadState(trunc.data(), trunc.size()));  // truncated: rejected, state unchanged
    CHECK(a->sched.now == before);
    std::vector<u8> junk(100, 0xAB);
    CHECK(!a->loadState(junk.data(), junk.size()));
}

static void testRomInfo() {
    std::printf("rom info\n");
    auto rom = makeRom("SRAM_V113", "BPEE");
    RomHeaderInfo h = Gba::parseHeader(rom.data(), rom.size());
    CHECK(h.title == "UNITTEST");
    CHECK(h.gameCode == "BPEE");
    CHECK(h.hasRtc);
    CHECK(h.saveKind == SaveKind::SRAM);
    auto rom2 = makeRom(nullptr, "BPRE");
    CHECK(!Gba::parseHeader(rom2.data(), rom2.size()).hasRtc);
    std::string err;
    auto g = std::make_unique<Gba>();
    std::vector<u8> tiny(16, 0);
    CHECK(!g->loadRom(tiny.data(), tiny.size(), nullptr, 0, err));
    CHECK(!err.empty());
    std::vector<u8> badBios(100, 0);
    CHECK(!g->loadRom(rom.data(), rom.size(), badBios.data(), badBios.size(), err));
}

int main() {
    testCheats();
    testSaves();
    testStates();
    testRomInfo();
    std::printf(failures ? "UNIT TESTS FAILED (%d)\n" : "UNIT TESTS PASSED\n", failures);
    return failures ? 1 : 0;
}
