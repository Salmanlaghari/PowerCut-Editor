#pragma once
// =============================================================================
// PowerCut Core — DAG (Directed Acyclic Graph) stub header.
//
// In the full native build this is backed by src/core/dag.cpp. This stub
// provides the type declarations so the export engine and JNI bridge compile
// cleanly. PowerCutDAG represents the render graph: clips, filters, transitions,
// text overlays — evaluated per-frame at a given microsecond timestamp.
// =============================================================================
#include <cstdint>
#include <vector>
#include <string>

namespace PowerCut {

using TimeMicros = int64_t;

// A segment of the DAG covering a time range.
struct DAGSegment {
    int mat_id = 0;          // material/clip ID
    int64_t src_offset = 0;  // source clip start offset (microseconds)

    // Map a global timeline time to the source clip's local time.
    TimeMicros src_time(TimeMicros global_t) const {
        return global_t - src_offset;
    }
};

// RGBA frame produced by the GPU compositor.
struct RGBAFrame {
    uint8_t* data = nullptr;
    int width = 0;
    int height = 0;
    int stride = 0;  // bytes per row (width * 4)

    void release() {
        // In the full build this returns the frame to the GPU frame pool.
    }
};

// PCM audio frame.
struct PCMFrame {
    float* data = nullptr;
    int samples = 0;
    int channels = 2;
    int sample_rate = 48000;
};

// The render DAG — the timeline graph.
class PowerCutDAG {
public:
    virtual ~PowerCutDAG() = default;

    // Total timeline duration in microseconds.
    TimeMicros duration() const { return dur_; }

    // Evaluate the DAG at time t, returning the active segments.
    std::vector<DAGSegment> evaluate(TimeMicros t) const {
        (void)t;
        return segments_;
    }

    // Detect scene cuts with the given threshold.
    std::vector<TimeMicros> detect_scene_cuts(int threshold) const {
        (void)threshold;
        return cuts_;
    }

    // Setters used by the editor to populate the DAG.
    void set_duration(TimeMicros d) { dur_ = d; }

private:
    TimeMicros dur_ = 0;
    std::vector<DAGSegment> segments_;
    std::vector<TimeMicros> cuts_;
};

}  // namespace PowerCut
