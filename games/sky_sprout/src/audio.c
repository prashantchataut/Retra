/* Sky Sprout - PSG sequencer + two-voice sound effects (original).
   Based on the Mochi Hop sequencer: square 1 doubles as the melodic SFX voice,
   noise doubles as the explosion voice; SFX override music on those channels. */
#include "game.h"

static const u8 *ch_ptr[4];
static const u8 *ch_start[4];
static u8 ch_wait[4];
static u8 ch_env[4];
static u8 ch_duty[4];
static u8 tempo, tick;
static u8 playing, paused, done;
static u8 cur_song = 0xFF;

/* voice 0 = square 1, voice 1 = noise */
static s8 v_id[2] = { -1, -1 };
static u8 v_t[2], v_prio[2];

static const u8 sfx_prio[SFX_COUNT] = {
    /* SHOT POP BOOM POWER 1UP BOMB DEATH WARN SHIELD HIT MOVE SELECT BACK TICK PAUSE CANCEL PHASE */
    1, 2, 4, 5, 6, 6, 7, 6, 5, 1, 3, 3, 3, 3, 3, 4, 5
};
/* bit0 = uses square voice, bit1 = uses noise voice */
static const u8 sfx_voices[SFX_COUNT] = { 1, 2, 2, 1, 1, 3, 3, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2 };

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
    REG_SOUNDCNT_X = 0x0080;
    REG_SOUNDCNT_L = 0xFF77;
    REG_SOUNDCNT_H = 0x0002;
    REG_SOUND3CNT_L = 0x0000;
    /* punchy bass wave: rounded saw */
    static const u8 wave[16] = { 0x02, 0x46, 0x8A, 0xCE, 0xFF, 0xFE, 0xEE, 0xDD,
                                 0xCB, 0xA9, 0x87, 0x65, 0x43, 0x21, 0x10, 0x00 };
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
    if (v_id[0] < 0) { REG_SOUND1CNT_H = 0; REG_SOUND1CNT_X = 0x8000; }
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
        if (v_id[0] >= 0) return;
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
        if (v_id[1] >= 0) return;
        if (note == 1) noise_hit(0xA1, 0x0062);
        else if (note == 2) noise_hit(0x92, 0x0031);
        else if (note == 3) noise_hit(0x41, 0x0010);
        else if (note == 4) noise_hit(0xB3, 0x0051);   /* crash */
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
    int vm = sfx_voices[id], pr = sfx_prio[id];
    for (int v = 0; v < 2; v++) {
        if (!(vm & (1 << v))) continue;
        if (v_id[v] >= 0 && v_prio[v] > pr) continue;
        v_id[v] = (s8)id;
        v_t[v] = 0;
        v_prio[v] = (u8)pr;
    }
}

static int sq_sfx(int id, int t) {
    switch (id) {
    case SFX_SHOT:
        if (t == 0) sq1_note(86, 0x41, 2, 0x001B);
        return t >= 3;
    case SFX_POWER:
        if (t % 3 == 0 && t < 15) { static const u8 a[5] = { 72, 76, 79, 84, 88 }; sq1_note(a[t / 3], 0xB2, 2, 0); }
        return t >= 22;
    case SFX_1UP:
        if (t % 5 == 0 && t < 30) { static const u8 a[6] = { 76, 79, 88, 84, 86, 91 }; sq1_note(a[t / 5], 0xC2, 1, 0); }
        return t >= 40;
    case SFX_BOMB:
        if (t == 0) sq1_note(48, 0xF4, 2, 0x0075);
        return t >= 40;
    case SFX_DEATH:
        if (t == 0) sq1_note(84, 0xF3, 0, 0x007D);
        if (t == 14) sq1_note(72, 0xC3, 0, 0x007D);
        return t >= 44;
    case SFX_WARN:
        if (t % 10 == 0 && t < 60) sq1_note((t / 10) & 1 ? 74 : 81, 0xD1, 1, 0);
        return t >= 62;
    case SFX_SHIELD:
        if (t == 0) sq1_note(91, 0xA2, 2, 0);
        if (t == 4) sq1_note(96, 0x92, 2, 0);
        return t >= 16;
    case SFX_MOVE:
        if (t == 0) sq1_note(84, 0x71, 2, 0);
        return t >= 4;
    case SFX_SELECT:
        if (t == 0) sq1_note(88, 0x91, 2, 0);
        if (t == 4) sq1_note(95, 0x92, 2, 0);
        return t >= 14;
    case SFX_BACK:
        if (t == 0) sq1_note(81, 0x91, 2, 0);
        if (t == 4) sq1_note(74, 0x92, 2, 0);
        return t >= 14;
    case SFX_TICK:
        if (t == 0) sq1_note(96, 0x61, 2, 0);
        return t >= 3;
    case SFX_PAUSE:
        if (t == 0) sq1_note(79, 0x91, 2, 0);
        if (t == 5) sq1_note(91, 0x91, 2, 0);
        return t >= 14;
    case SFX_CANCEL:
        if (t % 3 == 0 && t < 12) { static const u8 a[4] = { 93, 98, 100, 105 }; sq1_note(a[t / 3], 0x81, 2, 0); }
        return t >= 16;
    }
    return 1;
}

static int noise_sfx(int id, int t) {
    switch (id) {
    case SFX_POP:
        if (t == 0) noise_hit(0xB2, 0x0043);
        return t >= 10;
    case SFX_BOOM:
        if (t == 0) noise_hit(0xF6, 0x0072);
        if (t == 18) noise_hit(0xB5, 0x0073);
        return t >= 48;
    case SFX_BOMB:
        if (t == 0) noise_hit(0xF7, 0x0061);
        return t >= 50;
    case SFX_DEATH:
        if (t == 0) noise_hit(0xF5, 0x0073);
        return t >= 40;
    case SFX_HIT:
        if (t == 0) noise_hit(0x41, 0x0021);
        return t >= 2;
    case SFX_PHASE:
        if (t == 0) noise_hit(0xE5, 0x0052);
        return t >= 30;
    }
    return 1;
}

static void sfx_update(void) {
    for (int v = 0; v < 2; v++) {
        if (v_id[v] < 0) continue;
        int t = v_t[v]++;
        int end = v == 0 ? sq_sfx(v_id[v], t) : noise_sfx(v_id[v], t);
        if (end) {
            v_id[v] = -1;
            v_prio[v] = 0;
        }
    }
}

void audio_update(void) {
    sfx_update();
    if (!playing || paused) return;
    if (tick == 0) seq_tick();
    if (++tick >= tempo) tick = 0;
}
