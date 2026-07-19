#include "libretro_mgba_engine.h"

#include <algorithm>
#include <cmath>
#include <cstdlib>
#include <cstring>
#include <dlfcn.h>
#include <limits>

namespace retra {
namespace {
constexpr unsigned kRetroApiVersion = 1;
constexpr unsigned kEnvironmentGetCanDupe = 3;
constexpr unsigned kEnvironmentGetSystemDirectory = 9;
constexpr unsigned kEnvironmentSetPixelFormat = 10;
constexpr unsigned kEnvironmentSetInputDescriptors = 11;
constexpr unsigned kEnvironmentGetVariable = 15;
constexpr unsigned kEnvironmentSetVariables = 16;
constexpr unsigned kEnvironmentSetSupportNoGame = 18;
constexpr unsigned kEnvironmentGetSaveDirectory = 31;
constexpr unsigned kEnvironmentGetLanguage = 39;
constexpr unsigned kEnvironmentGetCoreOptionsVersion = 52;
constexpr unsigned kEnvironmentGetMessageInterfaceVersion = 59;
constexpr unsigned kDeviceJoypad = 1;
constexpr unsigned kMemorySaveRam = 0;
constexpr int kPixel0Rgb1555 = 0;
constexpr int kPixelXrgb8888 = 1;
constexpr int kPixelRgb565 = 2;
constexpr std::size_t kMaximumStateBytes = 64U * 1024U * 1024U;
constexpr std::size_t kMaximumAudioSamples = 48000U * 2U * 4U;
constexpr const char* kDefaultLibraryName = "libmgba_libretro.so";

std::uint8_t expand5(unsigned value) {
    return static_cast<std::uint8_t>((value << 3U) | (value >> 2U));
}

std::uint8_t expand6(unsigned value) {
    return static_cast<std::uint8_t>((value << 2U) | (value >> 4U));
}
} // namespace

LibretroMgbaEngine* LibretroMgbaEngine::active_ = nullptr;

LibretroMgbaEngine::LibretroMgbaEngine(const char* libraryPath) {
    const char* path = libraryPath;
    if (path == nullptr || path[0] == '\0') {
        const char* overridePath = std::getenv("RETRA_MGBA_LIBRETRO_PATH");
        path = overridePath != nullptr && overridePath[0] != '\0' ? overridePath : kDefaultLibraryName;
    }
    if (!claimActiveInstance()) {
        error_ = "Only one libretro gameplay core may be active at a time.";
        return;
    }
    if (!openLibrary(path)) {
        releaseActiveInstance();
        return;
    }
    retroSetEnvironment_(environmentThunk);
    retroSetVideoRefresh_(videoThunk);
    retroSetAudioSample_(audioThunk);
    retroSetAudioSampleBatch_(audioBatchThunk);
    retroSetInputPoll_(inputPollThunk);
    retroSetInputState_(inputStateThunk);
    retroInit_();
    initialized_ = true;
    if (retroApiVersion_() != kRetroApiVersion) {
        error_ = "The bundled libretro core uses an unsupported API version.";
        closeLibrary();
        releaseActiveInstance();
        return;
    }
    RetroSystemInfo info{};
    retroGetSystemInfo_(&info);
    if (info.needFullpath) {
        error_ = "The bundled core requires unrestricted file paths; Retra only permits in-memory ROM loading.";
        closeLibrary();
        releaseActiveInstance();
        return;
    }
    available_ = true;
}

LibretroMgbaEngine::~LibretroMgbaEngine() {
    closeLibrary();
    releaseActiveInstance();
}

bool LibretroMgbaEngine::canOpen(const char* libraryPath) {
    void* handle = dlopen(libraryPath != nullptr ? libraryPath : kDefaultLibraryName, RTLD_NOW | RTLD_LOCAL);
    if (handle == nullptr) return false;
    const bool hasApi = dlsym(handle, "retro_api_version") != nullptr;
    dlclose(handle);
    return hasApi;
}

template <typename Function>
bool LibretroMgbaEngine::loadSymbol(Function& destination, const char* name) {
    dlerror();
    void* symbol = dlsym(library_, name);
    const char* symbolError = dlerror();
    if (symbolError != nullptr || symbol == nullptr) {
        error_ = std::string("Missing libretro symbol: ") + name;
        return false;
    }
    static_assert(sizeof(Function) == sizeof(symbol));
    std::memcpy(&destination, &symbol, sizeof(destination));
    return true;
}

bool LibretroMgbaEngine::openLibrary(const char* path) {
    library_ = dlopen(path, RTLD_NOW | RTLD_LOCAL);
    if (library_ == nullptr) {
        const char* message = dlerror();
        error_ = message != nullptr ? message : "Unable to load the bundled mGBA libretro core.";
        return false;
    }
#define LOAD_REQUIRED(member, symbol) if (!loadSymbol(member, symbol)) { closeLibrary(); return false; }
    LOAD_REQUIRED(retroApiVersion_, "retro_api_version")
    LOAD_REQUIRED(retroSetEnvironment_, "retro_set_environment")
    LOAD_REQUIRED(retroSetVideoRefresh_, "retro_set_video_refresh")
    LOAD_REQUIRED(retroSetAudioSample_, "retro_set_audio_sample")
    LOAD_REQUIRED(retroSetAudioSampleBatch_, "retro_set_audio_sample_batch")
    LOAD_REQUIRED(retroSetInputPoll_, "retro_set_input_poll")
    LOAD_REQUIRED(retroSetInputState_, "retro_set_input_state")
    LOAD_REQUIRED(retroInit_, "retro_init")
    LOAD_REQUIRED(retroDeinit_, "retro_deinit")
    LOAD_REQUIRED(retroGetSystemInfo_, "retro_get_system_info")
    LOAD_REQUIRED(retroGetSystemAvInfo_, "retro_get_system_av_info")
    LOAD_REQUIRED(retroReset_, "retro_reset")
    LOAD_REQUIRED(retroRun_, "retro_run")
    LOAD_REQUIRED(retroSerializeSize_, "retro_serialize_size")
    LOAD_REQUIRED(retroSerialize_, "retro_serialize")
    LOAD_REQUIRED(retroUnserialize_, "retro_unserialize")
    LOAD_REQUIRED(retroLoadGame_, "retro_load_game")
    LOAD_REQUIRED(retroUnloadGame_, "retro_unload_game")
    LOAD_REQUIRED(retroGetMemoryData_, "retro_get_memory_data")
    LOAD_REQUIRED(retroGetMemorySize_, "retro_get_memory_size")
    LOAD_REQUIRED(retroCheatReset_, "retro_cheat_reset")
    LOAD_REQUIRED(retroCheatSet_, "retro_cheat_set")
#undef LOAD_REQUIRED
    return true;
}

void LibretroMgbaEngine::closeLibrary() {
    available_ = false;
    if (loaded_ && retroUnloadGame_ != nullptr) retroUnloadGame_();
    loaded_ = false;
    if (initialized_ && retroDeinit_ != nullptr) retroDeinit_();
    initialized_ = false;
    if (library_ != nullptr) dlclose(library_);
    library_ = nullptr;
}

bool LibretroMgbaEngine::claimActiveInstance() {
    if (active_ != nullptr) return false;
    active_ = this;
    return true;
}

void LibretroMgbaEngine::releaseActiveInstance() {
    if (active_ == this) active_ = nullptr;
}

bool LibretroMgbaEngine::loadRom(const std::vector<std::uint8_t>& rom) {
    if (!available_ || rom.empty() || rom.size() > 64U * 1024U * 1024U) return false;
    if (loaded_) {
        retroUnloadGame_();
        loaded_ = false;
    }
    rom_ = rom;
    const RetroGameInfo game{nullptr, rom_.data(), rom_.size(), nullptr};
    if (!retroLoadGame_(&game)) {
        rom_.clear();
        error_ = "mGBA rejected the selected ROM.";
        return false;
    }
    RetroSystemAvInfo av{};
    retroGetSystemAvInfo_(&av);
    width_ = static_cast<int>(av.geometry.baseWidth > 0 ? av.geometry.baseWidth : 240U);
    height_ = static_cast<int>(av.geometry.baseHeight > 0 ? av.geometry.baseHeight : 160U);
    if (av.timing.sampleRate >= 8000.0 && av.timing.sampleRate <= 192000.0) {
        sampleRate_ = static_cast<int>(std::lround(av.timing.sampleRate));
    }
    frame_.assign(static_cast<std::size_t>(width_) * static_cast<std::size_t>(height_), static_cast<std::int32_t>(0xFF000000U));
    audio_.clear();
    speedAccumulator_ = 0.0;
    loaded_ = true;
    return true;
}

std::vector<std::int32_t> LibretroMgbaEngine::step(std::uint32_t inputMask, float speedMultiplier) {
    if (!available_ || !loaded_) return {};
    inputMask_ = inputMask;
    const double speed = std::clamp(static_cast<double>(speedMultiplier), 0.25, 16.0);
    speedAccumulator_ += speed;
    int runs = 0;
    while (speedAccumulator_ >= 1.0 && runs < 16) {
        retroRun_();
        speedAccumulator_ -= 1.0;
        ++runs;
    }
    if (frame_.empty()) {
        retroRun_();
        speedAccumulator_ = 0.0;
    }
    return frame_;
}

std::vector<std::int16_t> LibretroMgbaEngine::drainAudio() {
    std::vector<std::int16_t> drained;
    drained.swap(audio_);
    return drained;
}

std::vector<std::uint8_t> LibretroMgbaEngine::serialize() const {
    if (!available_ || !loaded_) return {};
    const std::size_t size = retroSerializeSize_();
    if (size == 0 || size > kMaximumStateBytes) return {};
    std::vector<std::uint8_t> state(size);
    if (!retroSerialize_(state.data(), state.size())) return {};
    return state;
}

bool LibretroMgbaEngine::deserialize(const std::vector<std::uint8_t>& bytes) {
    if (!available_ || !loaded_ || bytes.empty() || bytes.size() > kMaximumStateBytes) return false;
    return retroUnserialize_(bytes.data(), bytes.size());
}

std::vector<std::uint8_t> LibretroMgbaEngine::batterySave() const {
    if (!available_ || !loaded_) return {};
    const std::size_t size = retroGetMemorySize_(kMemorySaveRam);
    const void* data = retroGetMemoryData_(kMemorySaveRam);
    if (size == 0 || size > 16U * 1024U * 1024U || data == nullptr) return {};
    const auto* begin = static_cast<const std::uint8_t*>(data);
    return std::vector<std::uint8_t>(begin, begin + size);
}

bool LibretroMgbaEngine::restoreBattery(const std::vector<std::uint8_t>& bytes) {
    if (!available_ || !loaded_ || bytes.empty()) return false;
    const std::size_t size = retroGetMemorySize_(kMemorySaveRam);
    void* data = retroGetMemoryData_(kMemorySaveRam);
    if (data == nullptr || size != bytes.size()) return false;
    std::memcpy(data, bytes.data(), size);
    return true;
}

bool LibretroMgbaEngine::setCheats(const std::vector<std::string>& codes) {
    if (!available_ || !loaded_ || retroCheatSet_ == nullptr || retroCheatReset_ == nullptr) return false;
    if (codes.size() > 512U) return false;
    retroCheatReset_();
    for (std::size_t index = 0; index < codes.size(); ++index) {
        const auto& code = codes[index];
        if (code.empty() || code.size() > 8192U || code.find('\0') != std::string::npos) {
            retroCheatReset_();
            return false;
        }
        retroCheatSet_(static_cast<unsigned>(index), true, code.c_str());
    }
    return true;
}

void LibretroMgbaEngine::clearCheats() {
    if (available_ && loaded_ && retroCheatReset_ != nullptr) retroCheatReset_();
}

void LibretroMgbaEngine::reset() {
    if (available_ && loaded_) retroReset_();
}

void LibretroMgbaEngine::onVideo(const void* data, unsigned width, unsigned height, std::size_t pitch) {
    if (data == nullptr) return;
    if (width == 0 || height == 0 || width > 4096 || height > 4096) return;
    const std::size_t pixels = static_cast<std::size_t>(width) * static_cast<std::size_t>(height);
    if (pixels > std::numeric_limits<std::size_t>::max() / sizeof(std::int32_t)) return;
    std::vector<std::int32_t> converted(pixels);
    for (unsigned y = 0; y < height; ++y) {
        const auto* row = static_cast<const std::uint8_t*>(data) + static_cast<std::size_t>(y) * pitch;
        for (unsigned x = 0; x < width; ++x) {
            std::uint32_t argb = 0xFF000000U;
            if (pixelFormat_ == kPixelXrgb8888) {
                std::uint32_t pixel = 0;
                std::memcpy(&pixel, row + static_cast<std::size_t>(x) * 4U, sizeof(pixel));
                argb |= pixel & 0x00FFFFFFU;
            } else {
                std::uint16_t pixel = 0;
                std::memcpy(&pixel, row + static_cast<std::size_t>(x) * 2U, sizeof(pixel));
                if (pixelFormat_ == kPixelRgb565) {
                    argb |= static_cast<std::uint32_t>(expand5((pixel >> 11U) & 0x1FU)) << 16U;
                    argb |= static_cast<std::uint32_t>(expand6((pixel >> 5U) & 0x3FU)) << 8U;
                    argb |= static_cast<std::uint32_t>(expand5(pixel & 0x1FU));
                } else {
                    argb |= static_cast<std::uint32_t>(expand5((pixel >> 10U) & 0x1FU)) << 16U;
                    argb |= static_cast<std::uint32_t>(expand5((pixel >> 5U) & 0x1FU)) << 8U;
                    argb |= static_cast<std::uint32_t>(expand5(pixel & 0x1FU));
                }
            }
            converted[static_cast<std::size_t>(y) * width + x] = static_cast<std::int32_t>(argb);
        }
    }
    width_ = static_cast<int>(width);
    height_ = static_cast<int>(height);
    frame_.swap(converted);
}

std::size_t LibretroMgbaEngine::onAudioBatch(const std::int16_t* data, std::size_t frames) {
    if (data == nullptr || frames == 0) return 0;
    const std::size_t samples = frames * 2U;
    if (samples > kMaximumAudioSamples) return frames;
    if (audio_.size() + samples > kMaximumAudioSamples) {
        const std::size_t excess = audio_.size() + samples - kMaximumAudioSamples;
        audio_.erase(audio_.begin(), audio_.begin() + static_cast<std::ptrdiff_t>(std::min(excess, audio_.size())));
    }
    audio_.insert(audio_.end(), data, data + samples);
    return frames;
}

std::int16_t LibretroMgbaEngine::onInputState(unsigned port, unsigned device, unsigned index, unsigned id) const {
    if (port != 0 || device != kDeviceJoypad || index != 0) return 0;
    unsigned bit = 32;
    switch (id) {
        case 8: bit = 0; break;  // A
        case 0: bit = 1; break;  // B
        case 2: bit = 2; break;  // Select
        case 3: bit = 3; break;  // Start
        case 7: bit = 4; break;  // Right
        case 6: bit = 5; break;  // Left
        case 4: bit = 6; break;  // Up
        case 5: bit = 7; break;  // Down
        case 11: bit = 8; break; // R
        case 10: bit = 9; break; // L
        default: return 0;
    }
    return (inputMask_ & (1U << bit)) != 0U ? 1 : 0;
}

bool LibretroMgbaEngine::onEnvironment(unsigned command, void* data) {
    switch (command) {
        case kEnvironmentGetCanDupe:
            if (data != nullptr) *static_cast<bool*>(data) = true;
            return data != nullptr;
        case kEnvironmentSetPixelFormat:
            if (data == nullptr) return false;
            pixelFormat_ = *static_cast<int*>(data);
            return pixelFormat_ == kPixel0Rgb1555 || pixelFormat_ == kPixelXrgb8888 || pixelFormat_ == kPixelRgb565;
        case kEnvironmentGetVariable:
            if (data != nullptr) static_cast<RetroVariable*>(data)->value = nullptr;
            return false;
        case kEnvironmentGetSystemDirectory:
        case kEnvironmentGetSaveDirectory:
            if (data != nullptr) *static_cast<const char**>(data) = nullptr;
            return true;
        case kEnvironmentGetLanguage:
        case kEnvironmentGetCoreOptionsVersion:
        case kEnvironmentGetMessageInterfaceVersion:
            if (data != nullptr) *static_cast<unsigned*>(data) = 0;
            return data != nullptr;
        case kEnvironmentSetVariables:
        case kEnvironmentSetSupportNoGame:
        case kEnvironmentSetInputDescriptors:
            return true;
        default:
            return false;
    }
}

bool LibretroMgbaEngine::environmentThunk(unsigned command, void* data) {
    return active_ != nullptr && active_->onEnvironment(command, data);
}

void LibretroMgbaEngine::videoThunk(const void* data, unsigned width, unsigned height, std::size_t pitch) {
    if (active_ != nullptr) active_->onVideo(data, width, height, pitch);
}

void LibretroMgbaEngine::audioThunk(std::int16_t left, std::int16_t right) {
    if (active_ != nullptr) {
        const std::int16_t pair[2] = {left, right};
        active_->onAudioBatch(pair, 1);
    }
}

std::size_t LibretroMgbaEngine::audioBatchThunk(const std::int16_t* data, std::size_t frames) {
    return active_ != nullptr ? active_->onAudioBatch(data, frames) : 0;
}

void LibretroMgbaEngine::inputPollThunk() {}

std::int16_t LibretroMgbaEngine::inputStateThunk(unsigned port, unsigned device, unsigned index, unsigned id) {
    return active_ != nullptr ? active_->onInputState(port, device, index, id) : 0;
}

} // namespace retra
