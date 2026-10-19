// Retra GBA core - common definitions.
#pragma once

#include <cstdint>
#include <cstddef>
#include <cstring>

namespace retra {

using u8 = uint8_t;
using u16 = uint16_t;
using u32 = uint32_t;
using u64 = uint64_t;
using s8 = int8_t;
using s16 = int16_t;
using s32 = int32_t;
using s64 = int64_t;

#if defined(__GNUC__)
#define RETRA_INLINE inline __attribute__((always_inline))
#define RETRA_NOINLINE __attribute__((noinline))
#define RETRA_LIKELY(x) __builtin_expect(!!(x), 1)
#define RETRA_UNLIKELY(x) __builtin_expect(!!(x), 0)
#else
#define RETRA_INLINE inline
#define RETRA_NOINLINE
#define RETRA_LIKELY(x) (x)
#define RETRA_UNLIKELY(x) (x)
#endif

// Little-endian host assumed (x86, arm64 Android). Unaligned-safe loads.
template <typename T>
RETRA_INLINE T loadLE(const u8* p) {
    T v;
    std::memcpy(&v, p, sizeof(T));
    return v;
}
template <typename T>
RETRA_INLINE void storeLE(u8* p, T v) {
    std::memcpy(p, &v, sizeof(T));
}

RETRA_INLINE u32 ror32(u32 v, u32 s) {
    s &= 31;
    return s ? (v >> s) | (v << (32 - s)) : v;
}

constexpr u32 kCyclesPerLine = 1232;
constexpr u32 kLinesPerFrame = 228;
constexpr u32 kCyclesPerFrame = kCyclesPerLine * kLinesPerFrame;  // 280896
constexpr u32 kCpuClock = 16777216;
constexpr u32 kSampleRate = 32768;
constexpr u32 kCyclesPerSample = kCpuClock / kSampleRate;  // 512

}  // namespace retra
