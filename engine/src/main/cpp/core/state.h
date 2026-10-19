// Retra GBA core - save-state serialization helpers.
#pragma once

#include <string>
#include <type_traits>
#include <vector>

#include "common.h"

namespace retra {

constexpr u32 kStateMagic = 0x53415452;  // "RTAS"
constexpr u32 kStateVersion = 1;

class StateWriter {
public:
    std::vector<u8> buf;

    void bytes(const void* p, size_t n) {
        const u8* b = static_cast<const u8*>(p);
        buf.insert(buf.end(), b, b + n);
    }
    template <typename T>
    void pod(const T& v) {
        static_assert(std::is_trivially_copyable<T>::value, "pod");
        bytes(&v, sizeof(T));
    }
    void u8v(const std::vector<u8>& v) {
        pod<u32>((u32)v.size());
        bytes(v.data(), v.size());
    }
    void tag(const char* t) { bytes(t, 4); }
};

class StateReader {
public:
    StateReader(const u8* d, size_t n) : data(d), size(n) {}
    const u8* data;
    size_t size;
    size_t pos = 0;
    bool ok = true;

    void bytes(void* p, size_t n) {
        if (!ok || pos + n > size) {
            ok = false;
            std::memset(p, 0, n);
            return;
        }
        std::memcpy(p, data + pos, n);
        pos += n;
    }
    template <typename T>
    void pod(T& v) {
        static_assert(std::is_trivially_copyable<T>::value, "pod");
        bytes(&v, sizeof(T));
    }
    template <typename T>
    T get() {
        T v{};
        pod(v);
        return v;
    }
    void u8v(std::vector<u8>& v, size_t maxSize) {
        u32 n = get<u32>();
        if (!ok || n > maxSize || pos + n > size) {
            ok = false;
            return;
        }
        v.assign(data + pos, data + pos + n);
        pos += n;
    }
    void tag(const char* t) {
        char b[4];
        bytes(b, 4);
        if (std::memcmp(b, t, 4) != 0) ok = false;
    }
};

}  // namespace retra
