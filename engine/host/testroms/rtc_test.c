/* RTC (S-3511 over cartridge GPIO) test, using the same 3-wire protocol as Pokemon's siirtc. */
#include "testlib.h"

const char rtc_tag[] __attribute__((used, aligned(4))) = "SIIRTC_V001";

#define GPIO_DATA REG16(0x080000C4)
#define GPIO_DIR REG16(0x080000C6)
#define GPIO_CTRL REG16(0x080000C8)

static void begin(void) {
    GPIO_DIR = 7;
    GPIO_DATA = 1;
    GPIO_DATA = 5;
}
static void end(void) {
    GPIO_DATA = 1;
    GPIO_DATA = 1;
}
static void put_cmd(u8 v) {
    for (int i = 0; i < 8; i++) {
        u16 b = (v >> (7 - i)) & 1;
        GPIO_DATA = (u16)((b << 1) | 4);
        GPIO_DATA = (u16)((b << 1) | 4 | 1);
    }
}
static void put_byte(u8 v) {
    for (int i = 0; i < 8; i++) {
        u16 b = (v >> i) & 1;
        GPIO_DATA = (u16)((b << 1) | 4);
        GPIO_DATA = (u16)((b << 1) | 4 | 1);
    }
}
static u8 get_byte(void) {
    u8 v = 0;
    for (int i = 0; i < 8; i++) {
        GPIO_DATA = 4;
        GPIO_DATA = 5;
        u8 bit = (GPIO_DATA >> 1) & 1;
        v = (u8)((v >> 1) | (bit << 7));
    }
    return v;
}
static void read_regs(u8 cmd, u8* out, int n) {
    begin();
    put_cmd(cmd);
    GPIO_DIR = 5;
    for (int i = 0; i < n; i++) out[i] = get_byte();
    end();
}
static void write_regs(u8 cmd, const u8* in, int n) {
    begin();
    put_cmd(cmd);
    for (int i = 0; i < n; i++) put_byte(in[i]);
    end();
}
static int bcd_ok(u8 v, int max) {
    if ((v & 15) > 9 || (v >> 4) > 9) return 0;
    return ((v >> 4) * 10 + (v & 15)) <= max;
}

int main(void) {
    char buf[128], *p;
    int fail = 0;
    GPIO_CTRL = 1;
    u8 status;
    read_regs(0x63, &status, 1);
    u8 dt[7];
    read_regs(0x65, dt, 7);
    p = fmt_str(buf, "RTC status=");
    p = fmt_hex(p, status, 2);
    p = fmt_str(p, " date=");
    for (int i = 0; i < 7; i++) {
        p = fmt_hex(p, dt[i], 2);
        p = fmt_str(p, " ");
    }
    dbg_print(buf);
    if (!(status & 0x40)) fail |= 1;
    if (!bcd_ok(dt[0], 99) || !bcd_ok(dt[1] & 0x1F, 12) || dt[1] == 0 || !bcd_ok(dt[2] & 0x3F, 31) ||
        !bcd_ok(dt[3] & 7, 6) || !bcd_ok(dt[4] & 0x3F, 23) || !bcd_ok(dt[5], 59) || !bcd_ok(dt[6], 59))
        fail |= 2;
    /* Set 2001-02-03 (Sat=6) 04:05:06 and read back */
    const u8 set[7] = {0x01, 0x02, 0x03, 0x06, 0x04, 0x05, 0x06};
    write_regs(0x64, set, 7);
    read_regs(0x65, dt, 7);
    p = fmt_str(buf, "RTC after set=");
    for (int i = 0; i < 7; i++) {
        p = fmt_hex(p, dt[i], 2);
        p = fmt_str(p, " ");
    }
    dbg_print(buf);
    if (dt[0] != 0x01 || dt[1] != 0x02 || dt[2] != 0x03 || (dt[4] & 0x3F) != 0x04 || dt[5] != 0x05 || dt[6] > 0x08)
        fail |= 4;
    /* time-only command */
    u8 tm[3];
    read_regs(0x67, tm, 3);
    if ((tm[0] & 0x3F) != 0x04 || tm[1] != 0x05) fail |= 8;
    /* GPIO read-back disabled -> ROM contents visible */
    GPIO_CTRL = 0;
    if (GPIO_DIR == 5) fail |= 16;
    p = fmt_str(buf, fail ? "RESULT FAIL " : "RESULT PASS ");
    fmt_hex(p, (u32)fail, 2);
    dbg_print(buf);
    for (;;) wait_vblank();
}
