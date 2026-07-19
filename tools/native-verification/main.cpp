#include <cassert>
#include <cstdint>
#include <iostream>
#include <vector>

#include "../../emulation/native/src/main/cpp/reference_engine.h"

int main() {
    retra::ReferenceEngine engine;
    std::vector<std::uint8_t> rom(1024, 0x42);
    assert(engine.loadRom(rom, std::string(64, 'a')));
    const auto first = engine.step(1u, 1.0f);
    assert(first.size() == static_cast<std::size_t>(retra::ReferenceEngine::kWidth * retra::ReferenceEngine::kHeight));
    assert(engine.frameCounter() == 1u);
    const auto state = engine.serialize();
    engine.step(0u, 4.0f);
    assert(engine.frameCounter() == 5u);
    assert(engine.deserialize(state));
    assert(engine.frameCounter() == 1u);
    engine.reset();
    assert(engine.frameCounter() == 0u);
    auto damaged = state;
    damaged[0] = 0;
    assert(!engine.deserialize(damaged));
    std::cout << "PASS native reference engine: load, frame, input marker, state, restore, reset, corruption rejection\n";
    return 0;
}
