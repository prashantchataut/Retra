#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace retra {

class LibretroMgbaEngine {
public:
    explicit LibretroMgbaEngine(const char* libraryPath = nullptr);
    ~LibretroMgbaEngine();

    LibretroMgbaEngine(const LibretroMgbaEngine&) = delete;
    LibretroMgbaEngine& operator=(const LibretroMgbaEngine&) = delete;

    bool available() const { return available_; }
    const std::string& error() const { return error_; }
    bool loadRom(const std::vector<std::uint8_t>& rom);
    std::vector<std::int32_t> step(std::uint32_t inputMask, float speedMultiplier);
    std::vector<std::int16_t> drainAudio();
    std::vector<std::uint8_t> serialize() const;
    bool deserialize(const std::vector<std::uint8_t>& bytes);
    std::vector<std::uint8_t> batterySave() const;
    bool restoreBattery(const std::vector<std::uint8_t>& bytes);
    bool setCheats(const std::vector<std::string>& codes);
    void clearCheats();
    void reset();
    int width() const { return width_; }
    int height() const { return height_; }
    int sampleRate() const { return sampleRate_; }

    static bool canOpen(const char* libraryPath = nullptr);

private:
    using EnvironmentCallback = bool (*)(unsigned, void*);
    using VideoCallback = void (*)(const void*, unsigned, unsigned, std::size_t);
    using AudioCallback = void (*)(std::int16_t, std::int16_t);
    using AudioBatchCallback = std::size_t (*)(const std::int16_t*, std::size_t);
    using InputPollCallback = void (*)();
    using InputStateCallback = std::int16_t (*)(unsigned, unsigned, unsigned, unsigned);

    struct RetroGameInfo {
        const char* path;
        const void* data;
        std::size_t size;
        const char* meta;
    };
    struct RetroSystemInfo {
        const char* libraryName;
        const char* libraryVersion;
        const char* validExtensions;
        bool needFullpath;
        bool blockExtract;
    };
    struct RetroGameGeometry {
        unsigned baseWidth;
        unsigned baseHeight;
        unsigned maxWidth;
        unsigned maxHeight;
        float aspectRatio;
    };
    struct RetroSystemTiming {
        double fps;
        double sampleRate;
    };
    struct RetroSystemAvInfo {
        RetroGameGeometry geometry;
        RetroSystemTiming timing;
    };
    struct RetroVariable {
        const char* key;
        const char* value;
    };

    template <typename Function>
    bool loadSymbol(Function& destination, const char* name);
    bool openLibrary(const char* path);
    void closeLibrary();
    bool claimActiveInstance();
    void releaseActiveInstance();
    void onVideo(const void* data, unsigned width, unsigned height, std::size_t pitch);
    std::size_t onAudioBatch(const std::int16_t* data, std::size_t frames);
    std::int16_t onInputState(unsigned port, unsigned device, unsigned index, unsigned id) const;
    bool onEnvironment(unsigned command, void* data);

    static bool environmentThunk(unsigned command, void* data);
    static void videoThunk(const void* data, unsigned width, unsigned height, std::size_t pitch);
    static void audioThunk(std::int16_t left, std::int16_t right);
    static std::size_t audioBatchThunk(const std::int16_t* data, std::size_t frames);
    static void inputPollThunk();
    static std::int16_t inputStateThunk(unsigned port, unsigned device, unsigned index, unsigned id);

    void* library_ = nullptr;
    bool available_ = false;
    bool initialized_ = false;
    bool loaded_ = false;
    std::string error_;
    std::vector<std::uint8_t> rom_;
    std::vector<std::int32_t> frame_;
    std::vector<std::int16_t> audio_;
    std::uint32_t inputMask_ = 0;
    int pixelFormat_ = 0;
    int width_ = 240;
    int height_ = 160;
    int sampleRate_ = 48000;
    double speedAccumulator_ = 0.0;

    unsigned (*retroApiVersion_)() = nullptr;
    void (*retroSetEnvironment_)(EnvironmentCallback) = nullptr;
    void (*retroSetVideoRefresh_)(VideoCallback) = nullptr;
    void (*retroSetAudioSample_)(AudioCallback) = nullptr;
    void (*retroSetAudioSampleBatch_)(AudioBatchCallback) = nullptr;
    void (*retroSetInputPoll_)(InputPollCallback) = nullptr;
    void (*retroSetInputState_)(InputStateCallback) = nullptr;
    void (*retroInit_)() = nullptr;
    void (*retroDeinit_)() = nullptr;
    void (*retroGetSystemInfo_)(RetroSystemInfo*) = nullptr;
    void (*retroGetSystemAvInfo_)(RetroSystemAvInfo*) = nullptr;
    void (*retroReset_)() = nullptr;
    void (*retroRun_)() = nullptr;
    std::size_t (*retroSerializeSize_)() = nullptr;
    bool (*retroSerialize_)(void*, std::size_t) = nullptr;
    bool (*retroUnserialize_)(const void*, std::size_t) = nullptr;
    bool (*retroLoadGame_)(const RetroGameInfo*) = nullptr;
    void (*retroUnloadGame_)() = nullptr;
    void* (*retroGetMemoryData_)(unsigned) = nullptr;
    std::size_t (*retroGetMemorySize_)(unsigned) = nullptr;
    void (*retroCheatReset_)() = nullptr;
    void (*retroCheatSet_)(unsigned, bool, const char*) = nullptr;

    static LibretroMgbaEngine* active_;
};

} // namespace retra
