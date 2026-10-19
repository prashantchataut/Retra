/* Mochi Hop - tiny PSG sequencer + sound effects (original). */
#include "game.h"

static const u8 *ch_ptr[4];
static const u8 *ch_start[4];
static u8 ch_wait[4];
static u8 ch_env[4];
static u8 ch_duty[4];
static u8 tempo, tick;
static u8 playing, paused, done;
static u8 cur_song = 0xFF;

static int sfx_id = -1, sfx_t;
static u8 sq1_busy, noise_busy;

static void sq1_note(int midi, int env, int duty, int sweep) {
    REG_SOUND1CNT_L = sweep ? sweep : 0x0008;
    REG_SOUND1CNT_H = (env << 8) | (duty << 6);
    REG_SOUND1CNT_X = sq_rate[midi] | 0x8000;
}

static void noise_hit(int env, int freq) {
    REG_SOUND4CNT_L = env << 8;
    REG_SOUND4CNT_H = freq | 0x8000;
}

void audio_init(void) {
    REG_SOUNDCNT_X = 0x0080;          /* master enable */
    REG_SOUNDCNT_L = 0xFF77;          /* all PSG channels L+R, full volume */
    REG_SOUNDCNT_H = 0x0002;          /* PSG at 100% */
    /* soft "round" bass wave: triangle with a little body */
    REG_SOUND3CNT_L = 0x0000;
    static const u8 wave[16] = { 0x01, 0x34, 0x67, 0x9A, 0xBC, 0xDE, 0xEF, 0xFF,
                                 0xFF, 0xFE, 0xED, 0xCB, 0xA9, 0x76, 0x43, 0x10 };
    for (int i = 0; i < 4; i++)
        WAVE_RAM[i] = wave[i * 4] | (wave[i * 4 + 1] << 8) | (wave[i * 4 + 2] << 16) | (wave[i * 4 + 3] << 24);
    REG_SOUND3CNT_L = 0x00C0;         /* enable, play bank 1 (the one we just wrote) */
    REG_SOUND3CNT_H = 0;
}

void music_stop(void) {
    playing = 0;
    cur_song = 0xFF;
    REG_SOUND3CNT_H = 0;
    REG_SOUND2CNT_L = 0;
    REG_SOUND2CNT_H = 0x8000;
    if (!sq1_busy) { REG_SOUND1CNT_H = 0; REG_SOUND1CNT_X = 0x8000; }
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
    paused = 0;
    done = 0;
    cur_song = s;
}

int music_done(void) { return done; }

void music_pause(int p) {
    paused = p;
    REG_SOUNDCNT_L = p ? 0xFF33 : 0xFF77;
    if (p) REG_SOUND3CNT_H = 0;
}

static void trigger(int c, int note) {
    switch (c) {
    case 0:
        if (sq1_busy) return;
        if (note) sq1_note(note, ch_env[0], ch_duty[0], 0);
        break;
    case 1:
        if (note) {
            REG_SOUND2CNT_L = (ch_env[1] << 8) | (ch_duty[1] << 6);
            REG_SOUND2CNT_H = sq_rate[note] | 0x8000;
        }
        break;
    case 2:
        if (note) {
            REG_SOUND3CNT_H = 0x2000;
            REG_SOUND3CNT_X = wave_rate[note] | 0x8000;
        } else {
            REG_SOUND3CNT_H = 0;
        }
        break;
    case 3:
        if (noise_busy) return;
        if (note == 1) noise_hit(0xA1, 0x0062);        /* kick */
        else if (note == 2) noise_hit(0x82, 0x0031);   /* snare */
        else if (note == 3) noise_hit(0x41, 0x0010);   /* hat */
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

static void sfx_update(void) {
    if (sfx_id < 0) return;
    int t = sfx_t++;
    int end = 0;
    sq1_busy = 1;
    switch (sfx_id) {
    case SFX_JUMP:
        if (t == 0) sq1_note(62, 0x92, 2, 0x0015);
        end = t >= 9;
        break;
    case SFX_STAR:
        if (t == 0) sq1_note(88, 0xA1, 2, 0);
        if (t == 4) sq1_note(95, 0xA2, 2, 0);
        end = t >= 16;
        break;
    case SFX_SPROUT:
        if (t % 4 == 0 && t < 20) {
            static const u8 arp[5] = { 72, 76, 79, 84, 88 };
            sq1_note(arp[t / 4], 0xB2, 1, 0);
        }
        end = t >= 30;
        break;
    case SFX_STOMP:
        if (t == 0) { sq1_note(67, 0xB1, 2, 0x0023 | 0x08); noise_busy = 1; noise_hit(0xC2, 0x0052); }
        if (t == 10) noise_busy = 0;
        end = t >= 10;
        break;
    case SFX_HURT:
        if (t == 0) sq1_note(81, 0xC3, 0, 0x003A);
        end = t >= 26;
        break;
    case SFX_CHECK:
        if (t % 4 == 0 && t < 16) {
            static const u8 arp[4] = { 79, 84, 88, 91 };
            sq1_note(arp[t / 4], 0xA2, 2, 0);
        }
        end = t >= 26;
        break;
    case SFX_SPRING:
        if (t == 0) sq1_note(55, 0xC3, 2, 0x0032);
        end = t >= 22;
        break;
    case SFX_MOVE:
        if (t == 0) sq1_note(84, 0x71, 2, 0);
        end = t >= 4;
        break;
    case SFX_SELECT:
        if (t == 0) sq1_note(88, 0x91, 2, 0);
        if (t == 4) sq1_note(93, 0x92, 2, 0);
        end = t >= 14;
        break;
    case SFX_BACK:
        if (t == 0) sq1_note(81, 0x91, 2, 0);
        if (t == 4) sq1_note(76, 0x92, 2, 0);
        end = t >= 14;
        break;
    case SFX_TICK:
        if (t == 0) sq1_note(96, 0x61, 2, 0);
        end = t >= 3;
        break;
    case SFX_FALL:
        if (t == 0) sq1_note(76, 0xA4, 1, 0x007C);
        end = t >= 40;
        break;
    default:
        end = 1;
    }
    if (end) {
        sfx_id = -1;
        sq1_busy = 0;
        noise_busy = 0;
    }
}

void audio_update(void) {
    sfx_update();
    if (!playing || paused) return;
    if (tick == 0) seq_tick();
    if (++tick >= tempo) tick = 0;
}
