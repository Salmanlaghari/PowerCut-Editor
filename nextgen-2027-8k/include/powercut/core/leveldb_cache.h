// =============================================================================
// PowerCut Pro 2027 8K — LevelDB render cache (kept working, off render thread)
// File: include/powercut/core/leveldb_cache.h
// =============================================================================
#pragma once
#include <cstdint>
#include <string>
#include <vector>
#include <memory>

namespace powercut::core {

// Frame cache backed by LevelDB. The original backend wrote from the render
// thread, which caused ANR + crash (P1 fix #5). The cache now exposes a
// background-write queue: put_async() enqueues onto a low-priority worker
// thread and never blocks the render loop.
class LevelDBCache {
public:
    static LevelDBCache& instance(); // process-wide singleton

    bool open(const std::string& dir);
    void close();

    // Synchronous read (cheap, OS page cache) — safe on render thread.
    bool get_frame(int64_t pts_us, std::vector<uint8_t>* out);

    // Asynchronous write — handed to a background worker. Non-blocking.
    void put_frame_async(int64_t pts_us, const uint8_t* data, size_t len);

    // Flush + wait for the background queue to drain (called at export end).
    void flush_sync();

    size_t pending_writes() const;

private:
    LevelDBCache();
    ~LevelDBCache();
    LevelDBCache(const LevelDBCache&) = delete;
    LevelDBCache& operator=(const LevelDBCache&) = delete;
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

} // namespace powercut::core
