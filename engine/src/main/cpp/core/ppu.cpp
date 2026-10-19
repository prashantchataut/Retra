// Retra GBA core - PPU: scanline renderer for modes 0-5, sprites, windows, blending, mosaic.
#include "ppu.h"

#include <algorithm>
#include <cmath>

#include "gba.h"
#include "state.h"

namespace retra {

namespace {
constexpr u16 kTransparent = 0x8000;

// OBJ sizes [shape][size] -> width, height
constexpr u8 kObjW[4][4] = {{8, 16, 32, 64}, {16, 32, 32, 64}, {8, 8, 16, 32}, {8, 8, 8, 8}};
constexpr u8 kObjH[4][4] = {{8, 16, 32, 64}, {8, 8, 16, 32}, {16, 32, 32, 64}, {8, 8, 8, 8}};

// objAttr bits
constexpr u8 OA_PRIO = 0x03;
constexpr u8 OA_SEMI = 0x04;
constexpr u8 OA_OPAQUE = 0x08;
constexpr u8 OA_WIN = 0x10;
}  // namespace

Ppu::Ppu(Gba* gba) : g(gba) {
    buildLut();
    reset();
}

void Ppu::reset() {
    std::fill(std::begin(frame), std::end(frame), 0xFF000000u);
    frameDone = false;
    vcount = 0;
    refX[0] = refX[1] = refY[0] = refY[1] = 0;
}

void Ppu::setColorCorrection(bool on) {
    if (on == colorCorrection) return;
    colorCorrection = on;
    buildLut();
}

void Ppu::buildLut() {
    for (u32 c = 0; c < 32768; c++) {
        u32 r = c & 31, gr = (c >> 5) & 31, b = (c >> 10) & 31;
        u32 R, G, B;
        if (!colorCorrection) {
            R = (r << 3) | (r >> 2);
            G = (gr << 3) | (gr >> 2);
            B = (b << 3) | (b >> 2);
        } else {
            // Approximation of the GBA's reflective LCD: dark gamma, slight channel cross-talk.
            const double lr = std::pow(r / 31.0, 4.0 / 2.2 * 1.0);
            const double lg = std::pow(gr / 31.0, 4.0 / 2.2 * 1.0);
            const double lb = std::pow(b / 31.0, 4.0 / 2.2 * 1.0);
            double rr = 0.82 * lr + 0.24 * lg - 0.06 * lb;
            double gg = 0.10 * lr + 0.69 * lg + 0.21 * lb;
            double bb = 0.17 * lr + 0.12 * lg + 0.71 * lb;
            auto enc = [](double v) {
                v = v < 0 ? 0 : (v > 1 ? 1 : v);
                return (u32)std::lround(std::pow(v, 1.0 / 1.45) * 255.0);
            };
            R = enc(rr);
            G = enc(gg);
            B = enc(bb);
        }
        lut[c] = 0xFF000000u | (R << 16) | (G << 8) | B;
    }
}

static inline s32 signExtend28(u32 v) { return (s32)(v << 4) >> 4; }

void Ppu::onAffineWrite(u32 addr) {
    Bus& b = g->bus;
    const int i = addr >= 0x38 ? 1 : 0;
    const u32 base = i ? 0x38 : 0x28;
    if (addr == base || addr == base + 2)
        refX[i] = signExtend28(b.io16(base) | ((u32)b.io16(base + 2) << 16));
    else
        refY[i] = signExtend28(b.io16(base + 4) | ((u32)b.io16(base + 6) << 16));
}

void Ppu::checkVcount() {
    Bus& b = g->bus;
    u16 stat = b.io16(4);
    const bool match = vcount == (u32)(stat >> 8);
    if (match) {
        if (!(stat & 4) && (stat & 0x20)) g->raiseIrq(IRQ_VCOUNT);
        stat |= 4;
    } else {
        stat &= ~4;
    }
    b.setIo16(4, stat);
}

void Ppu::onDispstatWrite() {
    Bus& b = g->bus;
    u16 stat = b.io16(4);
    if (vcount == (u32)(stat >> 8))
        stat |= 4;
    else
        stat &= ~4;
    b.setIo16(4, stat);
}

void Ppu::onHBlank() {
    Bus& b = g->bus;
    u16 stat = b.io16(4) | 2;
    b.setIo16(4, stat);
    if (stat & 0x10) g->raiseIrq(IRQ_HBLANK);
    if (vcount < 160) {
        renderLine(vcount);
        // advance affine reference points by (PB, PD)
        refX[0] += (s16)b.io16(0x22);
        refY[0] += (s16)b.io16(0x26);
        refX[1] += (s16)b.io16(0x32);
        refY[1] += (s16)b.io16(0x36);
        g->dma.trigger(DMA_HBLANK);
    }
    if (vcount >= 2 && vcount < 162) g->dma.videoCapture(vcount);
}

void Ppu::onLineEnd() {
    Bus& b = g->bus;
    vcount = vcount + 1;
    if (vcount >= kLinesPerFrame) vcount = 0;
    b.setIo16(6, (u16)vcount);
    u16 stat = b.io16(4) & ~2;
    if (vcount == 160) {
        stat |= 1;
        b.setIo16(4, stat);
        if (stat & 8) g->raiseIrq(IRQ_VBLANK);
        // reload affine reference points
        refX[0] = signExtend28(b.io16(0x28) | ((u32)b.io16(0x2A) << 16));
        refY[0] = signExtend28(b.io16(0x2C) | ((u32)b.io16(0x2E) << 16));
        refX[1] = signExtend28(b.io16(0x38) | ((u32)b.io16(0x3A) << 16));
        refY[1] = signExtend28(b.io16(0x3C) | ((u32)b.io16(0x3E) << 16));
        g->dma.trigger(DMA_VBLANK);
        frameDone = true;
    } else {
        if (vcount == 227) stat &= ~1;
        b.setIo16(4, stat);
    }
    checkVcount();
}

// ------------------------------------------------------------------ rendering

void Ppu::renderLine(u32 y) {
    Bus& b = g->bus;
    const u16 dispcnt = b.io16(0);
    u32* out = &frame[y * 240];
    if (dispcnt & 0x80) {
        for (int x = 0; x < 240; x++) out[x] = lut[0x7FFF];
        return;
    }
    const u32 mode = dispcnt & 7;
    u32 bgEnable = (dispcnt >> 8) & 0xF;
    switch (mode) {
        case 0: break;
        case 1: bgEnable &= 0x7; break;
        case 2: bgEnable &= 0xC; break;
        case 3:
        case 4:
        case 5: bgEnable &= 0x4; break;
        default: bgEnable = 0; break;
    }

    for (int bg = 0; bg < 4; bg++) {
        if (!(bgEnable & (1u << bg))) continue;
        if (mode == 0 || (mode == 1 && bg < 2))
            renderTextBg(bg, y);
        else if (mode <= 2)
            renderAffineBg(bg, y);
        else
            renderBitmap(mode, y);
    }

    std::fill(std::begin(objLine), std::end(objLine), kTransparent);
    std::memset(objAttr, 0, sizeof(objAttr));
    if (dispcnt & 0x1000) renderObjs(y);

    buildWindows(y);
    lastBgEnable = bgEnable;
    compose(y);
}

void Ppu::applyMosaicH(u16* line, u32 mosH) {
    if (mosH <= 1) return;
    for (u32 x = 0; x < 240; x++) line[x] = line[x - (x % mosH)];
}

void Ppu::renderTextBg(int bg, u32 y) {
    Bus& b = g->bus;
    const u16 cnt = b.io16(0x08 + bg * 2);
    const u32 hofs = b.io16(0x10 + bg * 4) & 0x1FF;
    const u32 vofs = b.io16(0x12 + bg * 4) & 0x1FF;
    const u32 charBase = ((cnt >> 2) & 3) * 0x4000;
    const u32 screenBase = ((cnt >> 8) & 31) * 0x800;
    const bool c256 = cnt & 0x80;
    const u32 size = (cnt >> 14) & 3;
    const u32 wMask = (size & 1) ? 511 : 255;
    const u32 hMask = (size & 2) ? 511 : 255;
    const u16 mosaic = b.io16(0x4C);
    u32 yy = y;
    if (cnt & 0x40) {
        const u32 mv = ((mosaic >> 4) & 15) + 1;
        yy = y - (y % mv);
    }
    const u32 ty = (yy + vofs) & hMask;
    u16* line = bgLine[bg];
    const u8* vram = b.vram;
    const u8* pram = b.pram;

    u32 lastTileCol = 0xFFFFFFFF;
    u16 entry = 0;
    for (u32 x = 0; x < 240; x++) {
        const u32 tx = (x + hofs) & wMask;
        const u32 tileCol = tx >> 3;
        if (tileCol != lastTileCol) {
            lastTileCol = tileCol;
            u32 block = 0;
            if (tx >= 256) block += 1;
            if (ty >= 256) block += (size == 3) ? 2 : 1;
            const u32 addr = screenBase + block * 0x800 + ((ty >> 3) & 31) * 64 + (tileCol & 31) * 2;
            entry = loadLE<u16>(&vram[addr & 0xFFFF]);
        }
        u32 px = tx & 7, py = ty & 7;
        if (entry & 0x400) px = 7 - px;
        if (entry & 0x800) py = 7 - py;
        const u32 tile = entry & 0x3FF;
        u32 color;
        if (c256) {
            const u32 addr = charBase + tile * 64 + py * 8 + px;
            if (addr >= 0x10000) {
                line[x] = kTransparent;
                continue;
            }
            color = vram[addr];
            if (!color) {
                line[x] = kTransparent;
                continue;
            }
            line[x] = loadLE<u16>(&pram[color * 2]) & 0x7FFF;
        } else {
            const u32 addr = charBase + tile * 32 + py * 4 + (px >> 1);
            if (addr >= 0x10000) {
                line[x] = kTransparent;
                continue;
            }
            color = (vram[addr] >> ((px & 1) * 4)) & 15;
            if (!color) {
                line[x] = kTransparent;
                continue;
            }
            line[x] = loadLE<u16>(&pram[((entry >> 12) * 16 + color) * 2]) & 0x7FFF;
        }
    }
    if (cnt & 0x40) applyMosaicH(line, (mosaic & 15) + 1);
}

void Ppu::renderAffineBg(int bg, u32 y) {
    (void)y;
    Bus& b = g->bus;
    const u16 cnt = b.io16(0x08 + bg * 2);
    const int i = bg - 2;
    const u32 regBase = 0x20 + i * 0x10;
    const s32 pa = (s16)b.io16(regBase + 0);
    const s32 pc = (s16)b.io16(regBase + 4);
    s32 rx = refX[i], ry = refY[i];
    const u16 mosaic = b.io16(0x4C);
    if (cnt & 0x40) {
        const u32 mv = ((mosaic >> 4) & 15) + 1;
        const u32 back = y % mv;
        rx -= (s32)back * (s16)b.io16(regBase + 2);
        ry -= (s32)back * (s16)b.io16(regBase + 6);
    }
    const u32 charBase = ((cnt >> 2) & 3) * 0x4000;
    const u32 screenBase = ((cnt >> 8) & 31) * 0x800;
    const bool wrap = cnt & 0x2000;
    const u32 sizePx = 128u << ((cnt >> 14) & 3);
    const u32 tilesPerRow = sizePx >> 3;
    const u8* vram = b.vram;
    const u8* pram = b.pram;
    u16* line = bgLine[bg];
    for (u32 x = 0; x < 240; x++, rx += pa, ry += pc) {
        s32 tx = rx >> 8, ty = ry >> 8;
        if (wrap) {
            tx &= (s32)(sizePx - 1);
            ty &= (s32)(sizePx - 1);
        } else if (tx < 0 || ty < 0 || tx >= (s32)sizePx || ty >= (s32)sizePx) {
            line[x] = kTransparent;
            continue;
        }
        const u32 mapAddr = screenBase + (u32)(ty >> 3) * tilesPerRow + (u32)(tx >> 3);
        const u32 tile = vram[mapAddr & 0xFFFF];
        const u32 addr = charBase + tile * 64 + (u32)(ty & 7) * 8 + (u32)(tx & 7);
        const u32 color = addr < 0x10000 ? vram[addr] : 0;
        line[x] = color ? (loadLE<u16>(&pram[color * 2]) & 0x7FFF) : kTransparent;
    }
    if (cnt & 0x40) applyMosaicH(line, (mosaic & 15) + 1);
}

void Ppu::renderBitmap(u32 mode, u32 y) {
    Bus& b = g->bus;
    const u16 dispcnt = b.io16(0);
    const u16 cnt = b.io16(0x0C);
    const s32 pa = (s16)b.io16(0x20);
    const s32 pc = (s16)b.io16(0x24);
    s32 rx = refX[0], ry = refY[0];
    const u16 mosaic = b.io16(0x4C);
    if (cnt & 0x40) {
        const u32 mv = ((mosaic >> 4) & 15) + 1;
        const u32 back = y % mv;
        rx -= (s32)back * (s16)b.io16(0x22);
        ry -= (s32)back * (s16)b.io16(0x26);
    }
    const u8* vram = b.vram;
    const u8* pram = b.pram;
    u16* line = bgLine[2];
    const u32 page = (dispcnt & 0x10) ? 0xA000 : 0;
    const s32 w = mode == 5 ? 160 : 240;
    const s32 h = mode == 5 ? 128 : 160;
    for (u32 x = 0; x < 240; x++, rx += pa, ry += pc) {
        const s32 tx = rx >> 8, ty = ry >> 8;
        if (tx < 0 || ty < 0 || tx >= w || ty >= h) {
            line[x] = kTransparent;
            continue;
        }
        if (mode == 3) {
            line[x] = loadLE<u16>(&vram[(ty * 240 + tx) * 2]) & 0x7FFF;
        } else if (mode == 4) {
            const u32 idx = vram[page + ty * 240 + tx];
            line[x] = idx ? (loadLE<u16>(&pram[idx * 2]) & 0x7FFF) : kTransparent;
        } else {
            line[x] = loadLE<u16>(&vram[page + (ty * 160 + tx) * 2]) & 0x7FFF;
        }
    }
    if (cnt & 0x40) applyMosaicH(line, (mosaic & 15) + 1);
}

void Ppu::renderObjs(u32 y) {
    Bus& b = g->bus;
    const u16 dispcnt = b.io16(0);
    const bool map1d = dispcnt & 0x40;
    const bool bitmapMode = (dispcnt & 7) >= 3;
    const u16 mosaic = b.io16(0x4C);
    const u32 mosH = ((mosaic >> 8) & 15) + 1;
    const u32 mosV = ((mosaic >> 12) & 15) + 1;
    const u8* oam = b.oam;
    const u8* vram = b.vram;
    const u8* pram = b.pram;

    for (int i = 0; i < 128; i++) {
        const u16 a0 = loadLE<u16>(&oam[i * 8]);
        const u16 a1 = loadLE<u16>(&oam[i * 8 + 2]);
        const u16 a2 = loadLE<u16>(&oam[i * 8 + 4]);
        const bool affine = a0 & 0x100;
        if (!affine && (a0 & 0x200)) continue;  // disabled
        const u32 objMode = (a0 >> 10) & 3;
        if (objMode == 3) continue;
        const u32 shape = (a0 >> 14) & 3;
        const u32 sz = (a1 >> 14) & 3;
        const s32 w = kObjW[shape][sz], h = kObjH[shape][sz];
        const bool dbl = affine && (a0 & 0x200);
        const s32 bw = dbl ? w * 2 : w, bh = dbl ? h * 2 : h;
        s32 oy = a0 & 0xFF;
        if (oy + bh > 256) oy -= 256;
        s32 ly = (s32)y - oy;
        if (ly < 0 || ly >= bh) continue;
        s32 ox = a1 & 0x1FF;
        if (ox >= 240) ox -= 512;
        if (ox + bw <= 0) continue;

        const bool mos = a0 & 0x1000;
        if (mos) {
            // vertical mosaic relative to screen line
            const s32 my = (s32)y - ((s32)y % (s32)mosV);
            ly = my - oy;
            if (ly < 0) ly = 0;
        }
        const bool c256 = a0 & 0x2000;
        const u32 tileBase = a2 & 0x3FF;
        if (bitmapMode && tileBase < 512) continue;
        const u32 prio = (a2 >> 10) & 3;
        const u32 palBank = (a2 >> 12) & 15;

        s32 pa = 0x100, pb = 0, pc = 0, pd = 0x100;
        if (affine) {
            const u32 pi = (a1 >> 9) & 31;
            pa = (s16)loadLE<u16>(&oam[pi * 32 + 6]);
            pb = (s16)loadLE<u16>(&oam[pi * 32 + 14]);
            pc = (s16)loadLE<u16>(&oam[pi * 32 + 22]);
            pd = (s16)loadLE<u16>(&oam[pi * 32 + 30]);
        }
        const bool hflip = !affine && (a1 & 0x1000);
        const bool vflip = !affine && (a1 & 0x2000);
        const u32 rowTiles = map1d ? (u32)(w >> 3) * (c256 ? 2 : 1) : 32;

        const s32 x0 = ox < 0 ? 0 : ox;
        const s32 x1 = ox + bw > 240 ? 240 : ox + bw;
        const s32 cx = bw / 2, cy = bh / 2;
        const s32 dy = ly - cy;
        for (s32 sx = x0; sx < x1; sx++) {
            s32 lx = sx - ox;
            if (mos) {
                s32 mx = sx - (sx % (s32)mosH);
                lx = mx - ox;
                if (lx < 0) lx = 0;
            }
            s32 tx, ty;
            if (affine) {
                const s32 dx = lx - cx;
                tx = ((pa * dx + pb * dy) >> 8) + w / 2;
                ty = ((pc * dx + pd * dy) >> 8) + h / 2;
                if (tx < 0 || ty < 0 || tx >= w || ty >= h) continue;
            } else {
                tx = hflip ? w - 1 - lx : lx;
                ty = vflip ? h - 1 - ly : ly;
            }
            u32 tile;
            u32 color;
            if (c256) {
                tile = (tileBase & ~1u) + (u32)(ty >> 3) * rowTiles + (u32)(tx >> 3) * 2;
                const u32 addr = 0x10000 + ((tile * 32 + (u32)(ty & 7) * 8 + (u32)(tx & 7)) & 0x7FFF);
                const u32 idx = vram[addr];
                if (!idx) continue;
                color = loadLE<u16>(&pram[0x200 + idx * 2]) & 0x7FFF;
            } else {
                tile = tileBase + (u32)(ty >> 3) * rowTiles + (u32)(tx >> 3);
                const u32 addr = 0x10000 + ((tile * 32 + (u32)(ty & 7) * 4 + (u32)((tx & 7) >> 1)) & 0x7FFF);
                const u32 idx = (vram[addr] >> ((tx & 1) * 4)) & 15;
                if (!idx) continue;
                color = loadLE<u16>(&pram[0x200 + (palBank * 16 + idx) * 2]) & 0x7FFF;
            }
            if (objMode == 2) {
                objAttr[sx] |= OA_WIN;
                continue;
            }
            const u8 cur = objAttr[sx];
            if (!(cur & OA_OPAQUE) || prio < (u32)(cur & OA_PRIO)) {
                objLine[sx] = (u16)color;
                objAttr[sx] = (u8)((cur & OA_WIN) | OA_OPAQUE | prio | (objMode == 1 ? OA_SEMI : 0));
            }
        }
    }
}

void Ppu::buildWindows(u32 y) {
    Bus& b = g->bus;
    const u16 dispcnt = b.io16(0);
    if (!(dispcnt & 0xE000)) {
        std::memset(winMask, 0x3F, sizeof(winMask));
        return;
    }
    const u16 winin = b.io16(0x48), winout = b.io16(0x4A);
    std::memset(winMask, winout & 0x3F, sizeof(winMask));
    if ((dispcnt & 0x8000) && (dispcnt & 0x1000)) {
        const u8 m = (winout >> 8) & 0x3F;
        for (int x = 0; x < 240; x++)
            if (objAttr[x] & OA_WIN) winMask[x] = m;
    }
    for (int w = 1; w >= 0; w--) {
        if (!(dispcnt & (0x2000 << w))) continue;
        const u16 h = b.io16(0x40 + w * 2), v = b.io16(0x44 + w * 2);
        u32 x1 = h >> 8, x2 = h & 0xFF;
        const u32 y1 = v >> 8, y2 = v & 0xFF;
        bool inY;
        if (y1 <= y2)
            inY = y >= y1 && y < y2;
        else
            inY = y >= y1 || y < y2;
        if (!inY) continue;
        if (x2 > 240) x2 = 240;
        const u8 m = (winin >> (w * 8)) & 0x3F;
        if (x1 <= x2) {
            for (u32 x = x1; x < x2; x++) winMask[x] = m;
        } else {
            for (u32 x = 0; x < x2; x++) winMask[x] = m;
            for (u32 x = x1; x < 240; x++) winMask[x] = m;
        }
    }
}

void Ppu::compose(u32 y) {
    Bus& b = g->bus;
    u32* out = &frame[y * 240];
    const u16 backdrop = loadLE<u16>(&b.pram[0]) & 0x7FFF;
    const u16 bldcnt = b.io16(0x50);
    const u16 bldalpha = b.io16(0x52);
    const u32 eva = std::min<u32>(bldalpha & 31, 16);
    const u32 evb = std::min<u32>((bldalpha >> 8) & 31, 16);
    const u32 evy = std::min<u32>(b.io16(0x54) & 31, 16);
    const u32 blendMode = (bldcnt >> 6) & 3;
    const u32 target1 = bldcnt & 0x3F;
    const u32 target2 = (bldcnt >> 8) & 0x3F;

    // BG order by priority then index
    int order[4];
    int nbg = 0;
    u32 prio[4];
    for (int bg = 0; bg < 4; bg++) prio[bg] = b.io16(0x08 + bg * 2) & 3;
    for (u32 p = 0; p < 4; p++)
        for (int bg = 0; bg < 4; bg++)
            if ((lastBgEnable & (1u << bg)) && prio[bg] == p) order[nbg++] = bg;

    for (u32 x = 0; x < 240; x++) {
        const u32 mask = winMask[x];
        u16 col[2] = {backdrop, backdrop};
        u32 layer[2] = {5, 5};
        int n = 0;
        const u8 oa = objAttr[x];
        const bool objVisible = (oa & OA_OPAQUE) && (mask & 0x10);
        const u32 objPrio = objVisible ? (oa & OA_PRIO) : 4;
        bool objPlaced = !objVisible;
        for (int k = 0; k < nbg && n < 2; k++) {
            const int bg = order[k];
            if (!(mask & (1u << bg))) continue;
            const u16 c = bgLine[bg][x];
            if (c & kTransparent) continue;
            if (!objPlaced && objPrio <= prio[bg]) {
                col[n] = objLine[x];
                layer[n] = 4;
                n++;
                objPlaced = true;
                if (n >= 2) break;
            }
            col[n] = c;
            layer[n] = (u32)bg;
            n++;
        }
        if (!objPlaced && n < 2) {
            if (n == 0) {
                col[1] = col[0];
                layer[1] = layer[0];
            }
            col[n] = objLine[x];
            layer[n] = 4;
            n++;
            if (n == 1) {
                col[1] = backdrop;
                layer[1] = 5;
            }
        }

        u16 c = col[0];
        if (mask & 0x20) {
            const bool semi = layer[0] == 4 && (oa & OA_SEMI);
            const bool secondIsTarget = (target2 >> layer[1]) & 1;
            if (semi && secondIsTarget) {
                c = blendAlpha(col[0], col[1], eva, evb);
            } else if ((target1 >> layer[0]) & 1) {
                if (blendMode == 1) {
                    if (secondIsTarget) c = blendAlpha(col[0], col[1], eva, evb);
                } else if (blendMode == 2) {
                    c = brighten(col[0], evy);
                } else if (blendMode == 3) {
                    c = darken(col[0], evy);
                }
            }
        }
        out[x] = lut[c & 0x7FFF];
    }
}

u16 Ppu::blendAlpha(u16 a, u16 bcol, u32 eva, u32 evb) {
    u32 r = ((a & 31) * eva + (bcol & 31) * evb) >> 4;
    u32 gg = (((a >> 5) & 31) * eva + ((bcol >> 5) & 31) * evb) >> 4;
    u32 bb = (((a >> 10) & 31) * eva + ((bcol >> 10) & 31) * evb) >> 4;
    if (r > 31) r = 31;
    if (gg > 31) gg = 31;
    if (bb > 31) bb = 31;
    return (u16)(r | (gg << 5) | (bb << 10));
}

u16 Ppu::brighten(u16 a, u32 evy) {
    u32 r = a & 31, gg = (a >> 5) & 31, bb = (a >> 10) & 31;
    r += ((31 - r) * evy) >> 4;
    gg += ((31 - gg) * evy) >> 4;
    bb += ((31 - bb) * evy) >> 4;
    return (u16)(r | (gg << 5) | (bb << 10));
}

u16 Ppu::darken(u16 a, u32 evy) {
    u32 r = a & 31, gg = (a >> 5) & 31, bb = (a >> 10) & 31;
    r -= (r * evy) >> 4;
    gg -= (gg * evy) >> 4;
    bb -= (bb * evy) >> 4;
    return (u16)(r | (gg << 5) | (bb << 10));
}

void Ppu::serialize(StateWriter& w) const {
    w.tag("PPU0");
    w.pod(vcount);
    w.pod(refX);
    w.pod(refY);
}

void Ppu::deserialize(StateReader& rd) {
    rd.tag("PPU0");
    rd.pod(vcount);
    rd.pod(refX);
    rd.pod(refY);
    if (vcount >= kLinesPerFrame) vcount = 0;
}

}  // namespace retra
