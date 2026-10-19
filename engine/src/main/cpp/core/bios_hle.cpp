// Retra GBA core - high level emulation of BIOS SWI functions.
// Reached through trap opcodes placed in the Retra replacement BIOS (bios/retra_bios.S).
#include <cmath>
#include <vector>

#include "bus_inl.h"
#include "gba.h"

namespace retra {

namespace {

struct SinTable {
    s16 v[256];
    SinTable() {
        for (int i = 0; i < 256; i++) v[i] = (s16)std::lround(std::sin(i * 3.14159265358979323846 * 2.0 / 256.0) * 16384.0);
    }
};
const SinTable& sinTable() {
    static const SinTable t;
    return t;
}

// Polynomial approximation used by the BIOS; all arithmetic wraps at 32 bits.
// Also reports the intermediate values the BIOS leaves in r1 and r3.
s32 biosArcTan(s32 i, s32* outR1 = nullptr, s32* outR3 = nullptr) {
    auto mul = [](s32 x, s32 y) { return (s32)((u32)x * (u32)y); };
    s32 a = -(mul(i, i) >> 14);
    s32 b = (mul(0xA9, a) >> 14) + 0x390;
    b = (mul(b, a) >> 14) + 0x91C;
    b = (mul(b, a) >> 14) + 0xFB6;
    b = (mul(b, a) >> 14) + 0x16AA;
    b = (mul(b, a) >> 14) + 0x2081;
    b = (mul(b, a) >> 14) + 0x3651;
    b = (mul(b, a) >> 14) + 0xA2F9;
    if (outR1) *outR1 = a;
    if (outR3) *outR3 = b;
    return mul(i, b) >> 16;
}

s32 biosArcTan2(s32 x, s32 y, s32* outR1) {
    *outR1 = y;
    auto div = [](s32 n, s32 d) -> s32 {
        if (d == 0) return 0;
        if (n == INT32_MIN && d == -1) return INT32_MIN;
        return n / d;
    };
    auto shl14 = [](s32 v) { return (s32)((u32)v << 14); };
    if (y == 0) return x >= 0 ? 0 : 0x8000;
    if (x == 0) return y >= 0 ? 0x4000 : 0xC000;
    if (y >= 0) {
        if (x >= 0) {
            if (x >= y) return biosArcTan(div(shl14(y), x), outR1);
        } else if (-x >= y) {
            return biosArcTan(div(shl14(y), x), outR1) + 0x8000;
        }
        return 0x4000 - biosArcTan(div(shl14(x), y), outR1);
    }
    if (x <= 0) {
        if (-x > -y) return biosArcTan(div(shl14(y), x), outR1) + 0x8000;
    } else if (x >= -y) {
        return biosArcTan(div(shl14(y), x), outR1) + 0x10000;
    }
    return 0xC000 - biosArcTan(div(shl14(x), y), outR1);
}

}  // namespace

void Gba::hleCall(u32 n) {
    Cpu& c = cpu;
    Bus& b = bus;
    u32* r = c.r;
    switch (n) {
        case 0x100:  // boot (reset vector)
        case 0x26: {  // HardReset
            bootDirect();
            c.r[15] = 0;  // will be replaced by branchTo below
            c.branchTo(0x08000000);
            return;
        }
        case 0x00: {  // SoftReset
            const u8 flag = b.peek<u8>(0x03007FFA);
            for (u32 a = 0x7E00; a < 0x8000; a++) b.iwram[a] = 0;
            c.setCpsr(0xD3);
            r[13] = 0x03007FE0;
            r[14] = 0;
            c.spsr = 0;
            c.setCpsr(0xD2);
            r[13] = 0x03007FA0;
            r[14] = 0;
            c.spsr = 0;
            c.setCpsr(0x1F);
            r[13] = 0x03007F00;
            for (int i = 0; i < 13; i++) r[i] = 0;
            r[14] = 0;
            c.branchTo(flag ? 0x02000000 : 0x08000000);
            return;
        }
        case 0x01: {  // RegisterRamReset
            const u32 f = r[0];
            b.ioWrite16(0x000, 0x0080);
            if (f & 0x01) std::memset(b.ewram, 0, sizeof(b.ewram));
            if (f & 0x02) std::memset(b.iwram, 0, 0x7E00);
            if (f & 0x04) std::memset(b.pram, 0, sizeof(b.pram));
            if (f & 0x08) std::memset(b.vram, 0, sizeof(b.vram));
            if (f & 0x10) std::memset(b.oam, 0, sizeof(b.oam));
            if (f & 0x20) {
                for (u32 a = 0x120; a < 0x160; a += 2) b.setIo16(a, 0);
                b.setIo16(0x134, 0x8000);
            }
            if (f & 0x40) {
                for (u32 a = 0x060; a < 0x0A8; a += 2) b.ioWrite16(a, 0);
                b.ioWrite16(0x084, 0);
                b.setIo16(0x088, 0x0200);
            }
            if (f & 0x80) {
                for (u32 a = 0x004; a < 0x060; a += 2) {
                    if (a == 0x006) continue;
                    b.ioWrite16(a, 0);
                }
                b.ioWrite16(0x020, 0x100);
                b.ioWrite16(0x026, 0x100);
                b.ioWrite16(0x030, 0x100);
                b.ioWrite16(0x036, 0x100);
                for (u32 a = 0x0B0; a < 0x0E0; a += 2) b.ioWrite16(a, 0);
                for (u32 a = 0x100; a < 0x110; a += 2) b.ioWrite16(a, 0);
                b.ioWrite16(0x132, 0);
                b.ioWrite16(0x200, 0);
                b.ioWrite16(0x202, 0xFFFF);
                b.ioWrite16(0x204, 0);
                b.ioWrite16(0x208, 0);
            }
            sched.now += 100;
            return;
        }
        case 0x06:    // Div
        case 0x07: {  // DivArm
            s32 num = (s32)r[0], den = (s32)r[1];
            if (n == 0x07) std::swap(num, den);
            if (den == 0) {
                r[0] = num < 0 ? (u32)-1 : 1;
                r[1] = (u32)num;
                r[3] = 1;
            } else if (num == INT32_MIN && den == -1) {
                r[0] = (u32)INT32_MIN;
                r[1] = 0;
                r[3] = 0x80000000u;
            } else {
                const s32 q = num / den;
                r[0] = (u32)q;
                r[1] = (u32)(num % den);
                r[3] = (u32)(q < 0 ? -q : q);
            }
            sched.now += 60;
            return;
        }
        case 0x08: {  // Sqrt
            u32 v = r[0], res = 0, bit = 1u << 30;
            while (bit > v) bit >>= 2;
            while (bit) {
                if (v >= res + bit) {
                    v -= res + bit;
                    res = (res >> 1) + bit;
                } else {
                    res >>= 1;
                }
                bit >>= 2;
            }
            r[0] = res;
            sched.now += 80;
            return;
        }
        case 0x09: {  // ArcTan
            s32 r1v = 0, r3v = 0;
            r[0] = (u32)biosArcTan((s32)r[0], &r1v, &r3v);
            r[1] = (u32)r1v;
            r[3] = (u32)r3v;
            sched.now += 50;
            return;
        }
        case 0x0A: {  // ArcTan2
            s32 r1v = 0;
            r[0] = (u32)(biosArcTan2((s32)r[0], (s32)r[1], &r1v) & 0xFFFF);
            r[1] = (u32)r1v;
            r[3] = 0x170;
            sched.now += 60;
            return;
        }
        case 0x0B: {  // CpuSet
            u32 src = r[0], dst = r[1];
            const u32 cnt = r[2] & 0x1FFFFF;
            const bool fill = (r[2] >> 24) & 1;
            const bool word = (r[2] >> 26) & 1;
            if ((src & 0x0E000000) == 0) return;  // BIOS protection
            if (word) {
                // The 8-bit SRAM bus sees the unaligned address; elsewhere words are aligned.
                if ((src >> 25) != 7) src &= ~3u;
                if ((dst >> 25) != 7) dst &= ~3u;
                const u32 v0 = fill ? b.read<u32>(src) : 0;
                for (u32 i = 0; i < cnt; i++) {
                    u32 v = fill ? v0 : b.read<u32>(src + i * 4, i != 0);
                    b.write<u32>(dst + i * 4, v, i != 0);
                }
            } else {
                // The BIOS uses LDRH/STRH without aligning: misaligned loads rotate.
                auto ld = [&](u32 a, bool seq) { return (u16)ror32(b.read<u16>(a, seq), (a & 1) * 8); };
                const u16 v0 = fill ? ld(src, false) : 0;
                for (u32 i = 0; i < cnt; i++) {
                    u16 v = fill ? v0 : ld(src + i * 2, i != 0);
                    b.write<u16>(dst + i * 2, v, i != 0);
                }
            }
            return;
        }
        case 0x0C: {  // CpuFastSet
            u32 src = r[0], dst = r[1];
            if ((src >> 25) != 7) src &= ~3u;
            if ((dst >> 25) != 7) dst &= ~3u;
            const u32 cnt = ((r[2] & 0x1FFFFF) + 7) & ~7u;
            const bool fill = (r[2] >> 24) & 1;
            if ((src & 0x0E000000) == 0) return;
            const u32 v0 = fill ? b.read<u32>(src) : 0;
            for (u32 i = 0; i < cnt; i++) {
                u32 v = fill ? v0 : b.read<u32>(src + i * 4, (i & 7) != 0);
                b.write<u32>(dst + i * 4, v, (i & 7) != 0);
            }
            return;
        }
        case 0x0D:  // GetBiosChecksum
            r[0] = 0xBAAE187F;
            sched.now += 100;
            return;
        case 0x0E: {  // BgAffineSet
            u32 src = r[0], dst = r[1];
            const auto& st = sinTable();
            for (u32 i = 0; i < r[2]; i++) {
                const s32 ox = (s32)b.read<u32>(src);
                const s32 oy = (s32)b.read<u32>(src + 4);
                const s32 dx = (s16)b.read<u16>(src + 8);
                const s32 dy = (s16)b.read<u16>(src + 10);
                const s32 sx = (s16)b.read<u16>(src + 12);
                const s32 sy = (s16)b.read<u16>(src + 14);
                const u32 theta = (b.read<u16>(src + 16) >> 8) & 0xFF;
                const s32 sn = st.v[theta], cs = st.v[(theta + 64) & 0xFF];
                const s32 pa = (sx * cs) >> 14;
                const s32 pb = -((sx * sn) >> 14);
                const s32 pc = (sy * sn) >> 14;
                const s32 pd = (sy * cs) >> 14;
                const s32 x = ox - (pa * dx + pb * dy);
                const s32 y = oy - (pc * dx + pd * dy);
                b.write<u16>(dst, (u16)pa);
                b.write<u16>(dst + 2, (u16)pb);
                b.write<u16>(dst + 4, (u16)pc);
                b.write<u16>(dst + 6, (u16)pd);
                b.write<u32>(dst + 8, (u32)x);
                b.write<u32>(dst + 12, (u32)y);
                src += 20;
                dst += 16;
            }
            return;
        }
        case 0x0F: {  // ObjAffineSet
            u32 src = r[0], dst = r[1];
            const u32 off = r[3];
            const auto& st = sinTable();
            for (u32 i = 0; i < r[2]; i++) {
                const s32 sx = (s16)b.read<u16>(src);
                const s32 sy = (s16)b.read<u16>(src + 2);
                const u32 theta = (b.read<u16>(src + 4) >> 8) & 0xFF;
                const s32 sn = st.v[theta], cs = st.v[(theta + 64) & 0xFF];
                b.write<u16>(dst, (u16)((sx * cs) >> 14));
                b.write<u16>(dst + off, (u16)(-((sx * sn) >> 14)));
                b.write<u16>(dst + off * 2, (u16)((sy * sn) >> 14));
                b.write<u16>(dst + off * 3, (u16)((sy * cs) >> 14));
                src += 8;
                dst += off * 4;
            }
            return;
        }
        case 0x10: {  // BitUnPack
            u32 src = r[0], dst = r[1];
            const u32 info = r[2];
            const u32 len = b.read<u16>(info);
            const u32 sw = b.read<u8>(info + 2);
            const u32 dw = b.read<u8>(info + 3);
            const u32 offRaw = b.read<u32>(info + 4);
            const u32 offset = offRaw & 0x7FFFFFFF;
            const bool zeroFlag = offRaw >> 31;
            if (!(sw == 1 || sw == 2 || sw == 4 || sw == 8)) return;
            if (!(dw == 1 || dw == 2 || dw == 4 || dw == 8 || dw == 16 || dw == 32)) return;
            u32 out = 0, bits = 0;
            const u32 smask = (1u << sw) - 1;
            for (u32 i = 0; i < len; i++) {
                const u8 byte = b.read<u8>(src + i);
                for (u32 s = 0; s < 8; s += sw) {
                    u32 v = (byte >> s) & smask;
                    if (v || zeroFlag) v += offset;
                    if (dw < 32) v &= (1u << dw) - 1;
                    out |= v << bits;
                    bits += dw;
                    if (bits >= 32) {
                        b.write<u32>(dst, out);
                        dst += 4;
                        out = 0;
                        bits = 0;
                    }
                }
            }
            return;
        }
        case 0x11:    // LZ77UnCompWram
        case 0x12: {  // LZ77UnCompVram
            u32 src = r[0], dst = r[1];
            const u32 hdr = b.read<u32>(src);
            const u32 size = hdr >> 8;
            if ((src & 0x0E000000) == 0 || size == 0) return;
            src += 4;
            std::vector<u8> out;
            out.reserve(size);
            while (out.size() < size) {
                u8 flags = b.read<u8>(src++);
                for (int k = 0; k < 8 && out.size() < size; k++, flags <<= 1) {
                    if (flags & 0x80) {
                        const u8 b0 = b.read<u8>(src++), b1 = b.read<u8>(src++);
                        const u32 len = (b0 >> 4) + 3;
                        const u32 disp = (((b0 & 0xF) << 8) | b1) + 1;
                        for (u32 j = 0; j < len && out.size() < size; j++) {
                            const size_t from = out.size() >= disp ? out.size() - disp : 0;
                            out.push_back(out.empty() ? 0 : out[from]);
                        }
                    } else {
                        out.push_back(b.read<u8>(src++));
                    }
                }
            }
            if (n == 0x11) {
                for (u32 i = 0; i < size; i++) b.write<u8>(dst + i, out[i], i != 0);
            } else {
                for (u32 i = 0; i < size; i += 2) {
                    u16 v = out[i] | (u16)((i + 1 < size ? out[i + 1] : 0) << 8);
                    b.write<u16>(dst + i, v, i != 0);
                }
            }
            return;
        }
        case 0x13: {  // HuffUnComp
            u32 src = r[0] & ~3u, dst = r[1];
            const u32 hdr = b.read<u32>(src);
            const u32 size = hdr >> 8;
            u32 bitsPer = hdr & 0xF;
            if (bitsPer != 4 && bitsPer != 8) bitsPer = 8;
            const u32 treeBase = src + 4;
            const u32 treeSize = (b.read<u8>(treeBase) + 1) * 2;
            u32 stream = treeBase + treeSize;
            const u32 root = treeBase + 1;
            u32 node = root;
            u32 out = 0, outBits = 0, written = 0;
            while (written < size) {
                const u32 word = b.read<u32>(stream);
                stream += 4;
                for (int bit = 31; bit >= 0 && written < size; bit--) {
                    const u8 nv = b.read<u8>(node);
                    const u32 dir = (word >> bit) & 1;
                    const u32 child = (node & ~1u) + (nv & 0x3F) * 2 + 2 + dir;
                    const bool leaf = dir ? (nv & 0x40) : (nv & 0x80);
                    if (leaf) {
                        const u32 val = b.read<u8>(child) & ((1u << bitsPer) - 1);
                        out |= val << outBits;
                        outBits += bitsPer;
                        if (outBits == 32) {
                            b.write<u32>(dst, out);
                            dst += 4;
                            written += 4;
                            out = 0;
                            outBits = 0;
                        }
                        node = root;
                    } else {
                        node = child;
                    }
                }
            }
            return;
        }
        case 0x14:    // RLUnCompWram
        case 0x15: {  // RLUnCompVram
            u32 src = r[0], dst = r[1];
            const u32 hdr = b.read<u32>(src);
            const u32 size = hdr >> 8;
            if ((src & 0x0E000000) == 0) return;
            src += 4;
            std::vector<u8> out;
            out.reserve(size);
            while (out.size() < size) {
                const u8 flag = b.read<u8>(src++);
                if (flag & 0x80) {
                    const u32 len = (flag & 0x7F) + 3;
                    const u8 v = b.read<u8>(src++);
                    for (u32 j = 0; j < len && out.size() < size; j++) out.push_back(v);
                } else {
                    const u32 len = (flag & 0x7F) + 1;
                    for (u32 j = 0; j < len && out.size() < size; j++) out.push_back(b.read<u8>(src++));
                }
            }
            if (n == 0x14) {
                for (u32 i = 0; i < size; i++) b.write<u8>(dst + i, out[i], i != 0);
            } else {
                for (u32 i = 0; i < size; i += 2) {
                    u16 v = out[i] | (u16)((i + 1 < size ? out[i + 1] : 0) << 8);
                    b.write<u16>(dst + i, v, i != 0);
                }
            }
            return;
        }
        case 0x16:    // Diff8bitUnFilterWram
        case 0x17: {  // Diff8bitUnFilterVram
            u32 src = r[0], dst = r[1];
            const u32 size = b.read<u32>(src) >> 8;
            src += 4;
            u8 acc = 0;
            std::vector<u8> out(size);
            for (u32 i = 0; i < size; i++) {
                acc = (u8)(acc + b.read<u8>(src + i));
                out[i] = acc;
            }
            if (n == 0x16) {
                for (u32 i = 0; i < size; i++) b.write<u8>(dst + i, out[i], i != 0);
            } else {
                for (u32 i = 0; i < size; i += 2) {
                    u16 v = out[i] | (u16)((i + 1 < size ? out[i + 1] : 0) << 8);
                    b.write<u16>(dst + i, v, i != 0);
                }
            }
            return;
        }
        case 0x18: {  // Diff16bitUnFilter
            u32 src = r[0], dst = r[1];
            const u32 size = b.read<u32>(src) >> 8;
            src += 4;
            u16 acc = 0;
            for (u32 i = 0; i + 1 < size + 1 && i < size; i += 2) {
                acc = (u16)(acc + b.read<u16>(src + i));
                b.write<u16>(dst + i, acc, i != 0);
            }
            return;
        }
        case 0x19: {  // SoundBias
            u16 bias = b.io16(0x088);
            bias = (u16)((bias & ~0x3FEu) | (r[0] ? 0x200 : 0));
            b.ioWrite16(0x088, bias);
            return;
        }
        case 0x1F: {  // MidiKey2Freq
            const u32 freq = b.read<u32>(r[0] + 4);
            const double e = (180.0 - (double)r[1] - (double)(r[2] & 0xFF) / 256.0) / 12.0;
            r[0] = (u32)((double)freq / std::pow(2.0, e));
            sched.now += 100;
            return;
        }
        case 0x25:  // MultiBoot: no link partner
            r[0] = 1;
            return;
        default:
            // Sound driver functions (BIOS m4a) and other rarely used calls: no-op.
            return;
    }
}

}  // namespace retra
