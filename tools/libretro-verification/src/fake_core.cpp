#include <cstddef>
#include <cstdint>
#include <cstring>
#include <vector>
#include <string>

extern "C" {
using retro_environment_t = bool (*)(unsigned, void*);
using retro_video_refresh_t = void (*)(const void*, unsigned, unsigned, std::size_t);
using retro_audio_sample_t = void (*)(std::int16_t, std::int16_t);
using retro_audio_sample_batch_t = std::size_t (*)(const std::int16_t*, std::size_t);
using retro_input_poll_t = void (*)();
using retro_input_state_t = std::int16_t (*)(unsigned, unsigned, unsigned, unsigned);

struct retro_game_info { const char* path; const void* data; std::size_t size; const char* meta; };
struct retro_system_info { const char* library_name; const char* library_version; const char* valid_extensions; bool need_fullpath; bool block_extract; };
struct retro_game_geometry { unsigned base_width; unsigned base_height; unsigned max_width; unsigned max_height; float aspect_ratio; };
struct retro_system_timing { double fps; double sample_rate; };
struct retro_system_av_info { retro_game_geometry geometry; retro_system_timing timing; };

static retro_environment_t environment_callback = nullptr;
static retro_video_refresh_t video_callback = nullptr;
static retro_audio_sample_t audio_callback = nullptr;
static retro_audio_sample_batch_t audio_batch_callback = nullptr;
static retro_input_poll_t input_poll_callback = nullptr;
static retro_input_state_t input_state_callback = nullptr;
static std::uint64_t frame_counter = 0;
static std::uint32_t last_input = 0;
static std::vector<std::uint32_t> frame(240U * 160U);
static std::uint8_t save_ram[32]{};
static bool loaded = false;
static std::vector<std::string> cheats;

unsigned retro_api_version() { return 1; }
void retro_set_environment(retro_environment_t callback) { environment_callback = callback; }
void retro_set_video_refresh(retro_video_refresh_t callback) { video_callback = callback; }
void retro_set_audio_sample(retro_audio_sample_t callback) { audio_callback = callback; }
void retro_set_audio_sample_batch(retro_audio_sample_batch_t callback) { audio_batch_callback = callback; }
void retro_set_input_poll(retro_input_poll_t callback) { input_poll_callback = callback; }
void retro_set_input_state(retro_input_state_t callback) { input_state_callback = callback; }

void retro_init() {
    int xrgb8888 = 1;
    if (environment_callback != nullptr) environment_callback(10, &xrgb8888);
}
void retro_deinit() {}
void retro_get_system_info(retro_system_info* info) {
    info->library_name = "Retra Fake Core";
    info->library_version = "1.0";
    info->valid_extensions = "gba";
    info->need_fullpath = false;
    info->block_extract = false;
}
void retro_get_system_av_info(retro_system_av_info* info) {
    info->geometry = {240, 160, 240, 160, 1.5F};
    info->timing = {60.0, 48000.0};
}
void retro_reset() { frame_counter = 0; last_input = 0; }
void retro_run() {
    if (!loaded) return;
    if (input_poll_callback != nullptr) input_poll_callback();
    const bool a = input_state_callback != nullptr && input_state_callback(0, 1, 0, 8) != 0;
    const bool b = input_state_callback != nullptr && input_state_callback(0, 1, 0, 0) != 0;
    last_input = (a ? 1U : 0U) | (b ? 2U : 0U);
    const std::uint32_t base = a ? 0x00FF0000U : (b ? 0x0000FF00U : 0x000000FFU);
    for (std::size_t index = 0; index < frame.size(); ++index) {
        frame[index] = base ^ static_cast<std::uint32_t>((index + frame_counter) & 0xFFU);
    }
    if (video_callback != nullptr) video_callback(frame.data(), 240, 160, 240U * sizeof(std::uint32_t));
    std::int16_t samples[32]{};
    for (std::size_t index = 0; index < 32; ++index) samples[index] = static_cast<std::int16_t>(frame_counter + index);
    if (audio_batch_callback != nullptr) audio_batch_callback(samples, 16);
    else if (audio_callback != nullptr) audio_callback(samples[0], samples[1]);
    ++frame_counter;
    save_ram[0] = static_cast<std::uint8_t>(frame_counter & 0xFFU);
}
std::size_t retro_serialize_size() { return sizeof(frame_counter) + sizeof(last_input); }
bool retro_serialize(void* data, std::size_t size) {
    if (data == nullptr || size != retro_serialize_size()) return false;
    std::memcpy(data, &frame_counter, sizeof(frame_counter));
    std::memcpy(static_cast<std::uint8_t*>(data) + sizeof(frame_counter), &last_input, sizeof(last_input));
    return true;
}
bool retro_unserialize(const void* data, std::size_t size) {
    if (data == nullptr || size != retro_serialize_size()) return false;
    std::memcpy(&frame_counter, data, sizeof(frame_counter));
    std::memcpy(&last_input, static_cast<const std::uint8_t*>(data) + sizeof(frame_counter), sizeof(last_input));
    return true;
}
bool retro_load_game(const retro_game_info* game) {
    loaded = game != nullptr && game->data != nullptr && game->size >= 192;
    return loaded;
}
void retro_unload_game() { loaded = false; }
void retro_cheat_reset() { cheats.clear(); }
void retro_cheat_set(unsigned index, bool enabled, const char* code) {
    if (!enabled || code == nullptr) return;
    if (cheats.size() <= index) cheats.resize(index + 1);
    cheats[index] = code;
}
void* retro_get_memory_data(unsigned id) { return id == 0 ? save_ram : nullptr; }
std::size_t retro_get_memory_size(unsigned id) { return id == 0 ? sizeof(save_ram) : 0; }
}
