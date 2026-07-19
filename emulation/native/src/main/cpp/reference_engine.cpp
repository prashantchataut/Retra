#include "reference_engine.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstring>

namespace retra {
namespace {
constexpr std::array<std::uint8_t, 8> kStateMagic{'R', 'E', 'T', 'R', 'A', 'R', 'E', 'F'};

std::int32_t argb(std::uint8_t red, std::uint8_t green, std::uint8_t blue) {
    return static_cast<std::int32_t>(0xFF000000u |
        (static_cast<std::uint32_t>(red) << 16u) |
        (static_cast<std::uint32_t>(green) << 8u) |
        static_cast<std::uint32_t>(blue));
}

void appendU32(std::vector<std::uint8_t>& output, std::uint32_t value) {
    for (int shift = 24; shift >= 0; shift -= 8) output.push_back(static_cast<std::uint8_t>((value >> shift) & 0xFFu));
}

void appendU64(std::vector<std::uint8_t>& output, std::uint64_t value) {
    for (int shift = 56; shift >= 0; shift -= 8) output.push_back(static_cast<std::uint8_t>((value >> shift) & 0xFFu));
}

std::uint32_t readU32(const std::vector<std::uint8_t>& input, std::size_t offset) {
    std::uint32_t value = 0;
    for (std::size_t index = 0; index < 4; ++index) value = (value << 8u) | input[offset + index];
    return value;
}

std::uint64_t readU64(const std::vector<std::uint8_t>& input, std::size_t offset) {
    std::uint64_t value = 0;
    for (std::size_t index = 0; index < 8; ++index) value = (value << 8u) | input[offset + index];
    return value;
}
} // namespace

bool ReferenceEngine::loadRom(const std::vector<std::uint8_t>& rom, const std::string& sha256) {
    if (rom.size() < 192 || sha256.size() != 64) return false;
    romFingerprint_ = 2166136261u;
    const auto sampleSize = std::min<std::size_t>(rom.size(), 4096);
    for (std::size_t index = 0; index < sampleSize; ++index) {
        romFingerprint_ ^= rom[index];
        romFingerprint_ *= 16777619u;
    }
    loaded_ = true;
    frameCounter_ = 0;
    lastInputMask_ = 0;
    return true;
}

std::vector<std::int32_t> ReferenceEngine::step(std::uint32_t inputMask, float speedMultiplier) {
    const float safeSpeed = std::clamp(speedMultiplier, 0.25f, 16.0f);
    frameCounter_ += static_cast<std::uint64_t>(std::max(1.0f, std::round(safeSpeed)));
    lastInputMask_ = inputMask;

    std::vector<std::int32_t> pixels(static_cast<std::size_t>(kWidth * kHeight));
    const auto phase = static_cast<int>((frameCounter_ + romFingerprint_) % 360u);
    for (int y = 0; y < kHeight; ++y) {
        for (int x = 0; x < kWidth; ++x) {
            const int wave = static_cast<int>(32.0 * std::sin((x + phase) * 0.045));
            const int red = std::clamp(44 + (x * 120 / kWidth) + wave, 0, 255);
            const int green = std::clamp(36 + (y * 110 / kHeight) - wave / 2, 0, 255);
            const int blue = std::clamp(96 + ((x + y + phase) % 112), 0, 255);
            pixels[static_cast<std::size_t>(y * kWidth + x)] = argb(
                static_cast<std::uint8_t>(red),
                static_cast<std::uint8_t>(green),
                static_cast<std::uint8_t>(blue)
            );
        }
    }

    for (int button = 0; button < 10; ++button) {
        if ((inputMask & (1u << button)) == 0u) continue;
        const int left = 10 + (button % 5) * 45;
        const int top = button < 5 ? 10 : 130;
        for (int y = top; y < std::min(top + 20, kHeight); ++y) {
            for (int x = left; x < std::min(left + 32, kWidth); ++x) {
                pixels[static_cast<std::size_t>(y * kWidth + x)] = argb(255, 214, 102);
            }
        }
    }
    return pixels;
}

std::vector<std::uint8_t> ReferenceEngine::serialize() const {
    std::vector<std::uint8_t> output;
    output.insert(output.end(), kStateMagic.begin(), kStateMagic.end());
    output.push_back(1);
    output.push_back(loaded_ ? 1 : 0);
    appendU64(output, frameCounter_);
    appendU32(output, lastInputMask_);
    appendU32(output, romFingerprint_);
    return output;
}

bool ReferenceEngine::deserialize(const std::vector<std::uint8_t>& bytes) {
    constexpr std::size_t kExpectedSize = 8 + 1 + 1 + 8 + 4 + 4;
    if (bytes.size() != kExpectedSize || !std::equal(kStateMagic.begin(), kStateMagic.end(), bytes.begin()) || bytes[8] != 1) {
        return false;
    }
    loaded_ = bytes[9] == 1;
    frameCounter_ = readU64(bytes, 10);
    lastInputMask_ = readU32(bytes, 18);
    romFingerprint_ = readU32(bytes, 22);
    return true;
}

void ReferenceEngine::reset() {
    frameCounter_ = 0;
    lastInputMask_ = 0;
}

} // namespace retra
