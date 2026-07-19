#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace retra {

class ReferenceEngine {
public:
    static constexpr int kWidth = 240;
    static constexpr int kHeight = 160;

    bool loadRom(const std::vector<std::uint8_t>& rom, const std::string& sha256);
    std::vector<std::int32_t> step(std::uint32_t inputMask, float speedMultiplier);
    std::vector<std::uint8_t> serialize() const;
    bool deserialize(const std::vector<std::uint8_t>& bytes);
    void reset();

    std::uint64_t frameCounter() const { return frameCounter_; }
    bool loaded() const { return loaded_; }

private:
    bool loaded_ = false;
    std::uint64_t frameCounter_ = 0;
    std::uint32_t lastInputMask_ = 0;
    std::uint32_t romFingerprint_ = 0;
};

} // namespace retra
