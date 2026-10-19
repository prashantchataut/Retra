// Retra GBA core - cheat engine (raw writes, CodeBreaker, GameShark v1/v2, Action Replay v3).
#pragma once

#include <string>
#include <vector>

#include "common.h"

namespace retra {

class Gba;

class Cheats {
public:
    explicit Cheats(Gba* gba) : g(gba) {}

    // Replaces the active list. Each string may contain several lines/codes.
    // Returns number of accepted codes (lines).
    int set(const std::vector<std::string>& codes);
    void clear() { list.clear(); }
    void apply();  // once per frame
    bool empty() const { return list.empty(); }

    enum class Kind : u8 { RAW, CB, GS, AR3 };
    struct Code {
        Kind kind;
        u32 a;  // address/opcode word
        u32 v;  // value
    };

    // Exposed for tests.
    static void gsDecrypt(u32& a, u32& v, const u32 seeds[4]);
    static const u32 kGsSeeds[4];
    static const u32 kAr3Seeds[4];

    Gba* g;

private:
    std::vector<Code> list;

    void runCodeBreaker(size_t& i);
    void runGameShark(size_t& i);
    void runAr3(size_t& i);
    bool parseLine(const std::string& line, int forced, std::vector<Code>& out, bool& cbEncrypted);
};

}  // namespace retra
