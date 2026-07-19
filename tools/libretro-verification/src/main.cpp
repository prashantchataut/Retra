#include "libretro_mgba_engine.h"

#include <cstdint>
#include <iostream>
#include <vector>

int main(int argc, char** argv) {
    if (argc != 2) {
        std::cerr << "expected fake core path\n";
        return 2;
    }
    if (!retra::LibretroMgbaEngine::canOpen(argv[1])) {
        std::cerr << "adapter could not inspect fake core\n";
        return 1;
    }
    retra::LibretroMgbaEngine engine(argv[1]);
    if (!engine.available()) {
        std::cerr << engine.error() << '\n';
        return 1;
    }
    std::vector<std::uint8_t> rom(1024, 0);
    if (!engine.loadRom(rom)) return 1;
    const auto frame = engine.step(1U, 1.0F);
    if (frame.size() != 240U * 160U || engine.width() != 240 || engine.height() != 160) return 1;
    if (static_cast<std::uint32_t>(frame.front()) != 0xFFFF0000U) return 1;
    const auto audio = engine.drainAudio();
    if (audio.size() != 32 || engine.sampleRate() != 48000) return 1;
    const auto state = engine.serialize();
    if (state.empty()) return 1;
    engine.step(2U, 2.0F);
    if (!engine.deserialize(state)) return 1;
    auto battery = engine.batterySave();
    if (battery.size() != 32) return 1;
    battery[1] = 0x5A;
    if (!engine.restoreBattery(battery)) return 1;
    if (engine.batterySave()[1] != 0x5A) return 1;
    if (!engine.setCheats({"12345678 00000001", "ABCDEF12 00000002"})) return 1;
    engine.clearCheats();
    engine.reset();
    std::cout << "PASS libretro gameplay adapter: ABI load, in-memory ROM, frame, input, audio, state, battery save, cheats\n";
    return 0;
}
