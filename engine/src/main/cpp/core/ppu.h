// Retra GBA core - picture processing unit (scanline renderer).
#pragma once

#include "common.h"

namespace retra {

class Gba;
class StateWriter;
class StateReader;

class Ppu {
public:
    explicit Ppu(Gba* gba);

    void reset();
    void onHBlank();   // end of H-draw for current VCOUNT
    void onLineEnd();  // VCOUNT advances
    void onAffineWrite(u32 ioAddr);  // BGxX/BGxY written -> latch internal reference
    void onDispstatWrite();
    void setColorCorrection(bool on);

    void serialize(StateWriter& w) const;
    void deserialize(StateReader& rd);

    // Output frame (ARGB8888), complete after VBlank start.
    alignas(16) u32 frame[240 * 160];
    bool frameDone = false;
    u32 vcount = 0;

    Gba* g;

private:
    void renderLine(u32 y);
    void checkVcount();

    // Per-line layer buffers. Bit layout: 0..14 colour (BGR555), bit 15 = transparent.
    // For OBJ: separate attribute buffer.
    alignas(16) u16 bgLine[4][240];
    alignas(16) u16 objLine[240];
    alignas(16) u8 objAttr[240];  // bits0-1 priority, bit2 semi-transparent, bit3 opaque, bit4 objwin
    alignas(16) u8 winMask[240];  // layer enable mask per pixel (bit0-3 BG, bit4 OBJ, bit5 effects)

    void renderTextBg(int bg, u32 y);
    void renderAffineBg(int bg, u32 y);
    void renderBitmap(u32 mode, u32 y);
    void renderObjs(u32 y);
    void buildWindows(u32 y);
    void compose(u32 y);

    s32 refX[2] = {}, refY[2] = {};  // internal affine reference points (BG2, BG3)
    u32 lut[32768];
    bool colorCorrection = false;
    void buildLut();
};

}  // namespace retra
