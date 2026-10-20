/* Turbo Drift - PSG sequencer (Mochi Hop format), sound effects and a
   speed-following engine tone on square channel 1. */
#include "game.h"

static const u8 *ch_ptr[4];
static const u8 *ch_start[4];
static u8 ch_wait[4];
static u8 ch_env[4];
static u8 ch_duty[4];
static u8 tempo, tick;
static u8 playing, done;
static u8 cur_song = 0xFF;
u8 sound_on = 1;

static int sfx_id = -1, sfx_t;
static u8 sq1_busy, noise_busy;
static u8 eng_on, eng_trig, skid_on, skid_trig;
static int eng_rpm, eng_load, eng_vol;

static void sq1_note(int midi, int env, int duty, int sweep) {
    REG_SOUND1CNT_L = sweep ? sweep : 0x0008;
    REG_SOUND1CNT_H = (env << 8) | (duty << 6);
    REG_SOUND1CNT_X = sq_rate[midi] | 0x8000;
}

static void sq1_rate(int rate, int env, int duty, int sweep) {
    REG_SOUND1CNT_L = sweep ? sweep : 0x0008;
    REG_SOUND1CNT_H = (env << 8) | (duty << 6);
    REG_SOUND1CNT_X = (rate & 2047) | 0x8000;
}

static void noise_hit(int env, int freq) {
    REG_SOUND4CNT_L = env << 8;
    REG_SOUND4CNT_H = freq | 0x8000;
}

void audio_init(void) {
    REG_SOUNDCNT_X = 0x0080;
    REG_SOUNDCNT_L = 0xFF77;
    REG_SOUNDCNT_H = 0x0002;
    REG_SOUND3CNT_L = 0x0000;
    /* punchy saw-ish bass wave */
    static const u8 wave[16] = { 0xFF, 0xEE, 0xDD, 0xCC, 0xBB, 0xAA, 0x99, 0x88,
                                 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11, 0x00 };
    for (int i = 0; i < 4; i++)
        WAVE_RAM[i] = wave[i * 4] | (wave[i * 4 + 1] << 8) | (wave[i * 4 + 2] << 16) | (wave[i * 4 + 3] << 24);
    REG_SOUND3CNT_L = 0x00C0;
    REG_SOUND3CNT_H = 0;
}

void music_stop(void) {
    playing = 0;
    cur_song = 0xFF;
    REG_SOUND3CNT_H = 0;
    REG_SOUND2CNT_L = 0;
    REG_SOUND2CNT_H = 0x8000;
    if (!sq1_busy && !eng_on) { REG_SOUND1CNT_H = 0; REG_SOUND1CNT_X = 0x8000; }
}

void music_play(int s) {
    if (s == cur_song && playing) return;
    const SongDef *sd = &song_defs[s];
    for (int c = 0; c < 4; c++) {
        ch_ptr[c] = ch_start[c] = sd->ch[c];
        ch_wait[c] = 1;
        ch_env[c] = 0xA2;
        ch_duty[c] = 2;
    }
    tempo = sd->tempo;
    tick = 0;
    playing = 1;
    done = 0;
    cur_song = (u8)s;
}

int music_done(void) { return done; }

static void trigger(int c, int note) {
    if (!sound_on && c != 3) note = note ? 0 : 0;
    switch (c) {
    case 0:
        if (sq1_busy || eng_on) return;
        if (note && sound_on) sq1_note(note, ch_env[0], ch_duty[0], 0);
        break;
    case 1:
        if (note && sound_on) {
            REG_SOUND2CNT_L = (ch_env[1] << 8) | (ch_duty[1] << 6);
            REG_SOUND2CNT_H = sq_rate[note] | 0x8000;
        }
        break;
    case 2:
        if (note && sound_on) {
            REG_SOUND3CNT_H = 0x2000;
            REG_SOUND3CNT_X = wave_rate[note] | 0x8000;
        } else {
            REG_SOUND3CNT_H = 0;
        }
        break;
    case 3:
        if (noise_busy || skid_on || !sound_on) return;
        if (note == 1) noise_hit(0xB1, 0x0062);
        else if (note == 2) noise_hit(0x92, 0x0032);
        else if (note == 3) noise_hit(0x51, 0x0011);
        else if (note == 4) noise_hit(0x63, 0x0012);
        break;
    }
}

static void seq_tick(void) {
    for (int c = 0; c < 4; c++) {
        if (!ch_ptr[c]) continue;
        if (--ch_wait[c]) continue;
        for (;;) {
            u8 b = *ch_ptr[c]++;
            if (b == 0xF0) { ch_env[c] = *ch_ptr[c]++; continue; }
            if (b == 0xF1) { ch_duty[c] = *ch_ptr[c]++; continue; }
            if (b == 0xFF) { ch_ptr[c] = ch_start[c]; continue; }
            if (b == 0xFE) { ch_ptr[c] = 0; if (c == 1) done = 1; if (c == 2) REG_SOUND3CNT_H = 0; break; }
            u8 d = *ch_ptr[c]++;
            trigger(c, b);
            ch_wait[c] = d;
            break;
        }
    }
}

void sfx_play(int id) {
    sfx_id = id;
    sfx_t = 0;
}

void engine_set(int on, int rpm, int load) {
    if (on && !eng_on) eng_trig = 1;
    if (!on && eng_on && !sq1_busy) { REG_SOUND1CNT_H = 0; REG_SOUND1CNT_X = 0x8000; }
    eng_on = (u8)on;
    eng_rpm = rpm;
    eng_load = load;
}

void skid_set(int on) {
    if (on && !skid_on) skid_trig = 1;
    if (!on && skid_on && !noise_busy) { REG_SOUND4CNT_L = 0; REG_SOUND4CNT_H = 0x8000; }
    skid_on = (u8)on;
}

static void sfx_update(void) {
    if (sfx_id < 0) return;
    int t = sfx_t++;
    int end = 0;
    sq1_busy = 1;
    switch (sfx_id) {
    case SFX_MOVE:
        if (t == 0) sq1_note(84, 0x71, 2, 0);
        end = t >= 4;
        break;
    case SFX_SELECT:
        if (t == 0) sq1_note(88, 0x91, 2, 0);
        if (t == 4) sq1_note(95, 0x92, 2, 0);
        end = t >= 14;
        break;
    case SFX_BACK:
        if (t == 0) sq1_note(81, 0x91, 2, 0);
        if (t == 4) sq1_note(74, 0x92, 2, 0);
        end = t >= 14;
        break;
    case SFX_BUY:
        if (t % 3 == 0 && t < 15) {
            static const u8 arp[5] = { 84, 88, 91, 96, 100 };
            sq1_note(arp[t / 3], 0xA2, 2, 0);
        }
        end = t >= 26;
        break;
    case SFX_ERROR:
        if (t == 0) sq1_note(50, 0xA2, 0, 0);
        if (t == 6) sq1_note(46, 0xA2, 0, 0);
        end = t >= 18;
        break;
    case SFX_COUNT:
        if (t == 0) sq1_note(69, 0xC3, 2, 0);
        end = t >= 20;
        break;
    case SFX_GO:
        if (t == 0) sq1_note(81, 0xF4, 2, 0);
        end = t >= 34;
        break;
    case SFX_LAP:
        if (t % 4 == 0 && t < 16) {
            static const u8 arp[4] = { 79, 84, 88, 91 };
            sq1_note(arp[t / 4], 0xB2, 2, 0);
        }
        end = t >= 26;
        break;
    case SFX_CRASH:
        if (t == 0) {
            sq1_note(43, 0xF4, 0, 0x007F);
            noise_busy = 1;
            noise_hit(0xF5, 0x0071);
        }
        end = t >= 40;
        break;
    case SFX_BUMP:
        if (t == 0) { noise_busy = 1; noise_hit(0xC2, 0x0052); sq1_note(48, 0xB1, 0, 0x001F); }
        end = t >= 12;
        break;
    case SFX_NITRO:
        if (t == 0) {
            sq1_note(55, 0xD5, 1, 0x0016);
            noise_busy = 1;
            noise_hit(0xD6, 0x0043);
        }
        end = t >= 44;
        break;
    case SFX_NEAR:
        if (t == 0) sq1_note(91, 0xA1, 2, 0);
        if (t == 3) sq1_note(98, 0xA2, 2, 0);
        end = t >= 14;
        break;
    case SFX_BOOST:
        if (t == 0) sq1_note(72, 0xC3, 2, 0x0015);
        end = t >= 20;
        break;
    case SFX_FINISH:
        if (t % 5 == 0 && t < 30) {
            static const u8 arp[6] = { 72, 76, 79, 84, 88, 91 };
            sq1_note(arp[t / 5], 0xC2, 2, 0);
        }
        end = t >= 50;
        break;
    default:
        end = 1;
    }
    if (end) {
        sfx_id = -1;
        sq1_busy = 0;
        if (noise_busy) { noise_busy = 0; if (skid_on) skid_trig = 1; }
        if (eng_on) eng_trig = 1;
        else { REG_SOUND1CNT_H = 0; REG_SOUND1CNT_X = 0x8000; }
    }
}

static void engine_update(void) {
    if (!eng_on || sq1_busy) return;
    /* rpm 0..1023 -> 72..280 Hz; slight alternating detune for growl */
    int hz = 72 + ((eng_rpm * 208) >> 10);
    int rate = 2048 - (int)udiv32(131072, (u32)hz);
    rate += (vbl_count & 1) ? 5 : -5;
    if (rate < 0) rate = 0;
    int vol = eng_load ? 7 : 5;
    if (eng_trig || vol != eng_vol) {
        sq1_rate(rate, vol << 4, 0, 0);
        eng_vol = vol;
        eng_trig = 0;
    } else {
        REG_SOUND1CNT_X = (u16)rate;
    }
}

static void skid_update(void) {
    if (!skid_on || noise_busy) return;
    if (skid_trig) {
        REG_SOUND4CNT_L = 0x6000;
        REG_SOUND4CNT_H = 0x0009 | 0x8000;
        skid_trig = 0;
    }
}

void audio_update(void) {
    sfx_update();
    engine_update();
    skid_update();
    if (!playing) return;
    if (tick == 0) seq_tick();
    if (++tick >= tempo) tick = 0;
}
