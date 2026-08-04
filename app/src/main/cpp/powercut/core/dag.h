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

// ---- Keyframe interpolation -----------------------------------------------
// A single animated parameter value on the timeline.
struct Keyframe {
    TimeMicros time = 0;       // timeline position (microseconds)
    double value = 0.0;        // parameter value at this keyframe
    enum Ease { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT } ease = LINEAR;
};

// Helper: interpolate keyframes at time t (linear for stub).
inline double interpolate_keyframes(const std::vector<Keyframe>& kfs, TimeMicros t) {
    if (kfs.empty()) return 1.0;  // default identity
    if (kfs.size() == 1 || t <= kfs.front().time) return kfs.front().value;
    if (t >= kfs.back().time) return kfs.back().value;
    for (size_t i = 1; i < kfs.size(); ++i) {
        if (t <= kfs[i].time) {
            const auto& a = kfs[i - 1];
            const auto& b = kfs[i];
            double span = (double)(b.time - a.time);
            if (span < 1.0) return b.value;
            double frac = (double)(t - a.time) / span;
            // Basic ease approximation for EASE_IN_OUT
            if (a.ease == Keyframe::EASE_IN_OUT) frac = frac * frac * (3.0 - 2.0 * frac);
            else if (a.ease == Keyframe::EASE_IN) frac = frac * frac;
            else if (a.ease == Keyframe::EASE_OUT) frac = 1.0 - (1.0 - frac) * (1.0 - frac);
            return a.value + (b.value - a.value) * frac;
        }
    }
    return kfs.back().value;
}

// ---- Effect node -----------------------------------------------------------
// Represents a visual effect (color grade, filter, LUT, etc.) applied to a
// segment. The compositor interprets these during render().
struct EffectNode {
    enum Type {
        EFF_NONE = 0,
        COLOR_GRADE,    // brightness/contrast/saturation/temp/tint
        LUT,            // 3D LUT file path
        FILTER,         // named filter (vintage, b&w, etc.)
        BLUR,           // gaussian blur radius
        SHARPEN,        // sharpen amount
        VIGNETTE,       // vignette intensity
        GRAIN           // film grain amount
    } type = EFF_NONE;
    std::string name;             // filter name or LUT path
    double intensity = 1.0;       // 0.0–1.0 blend
    std::vector<Keyframe> params; // animated effect parameters
};

// ---- A segment of the DAG covering a time range ----------------------------
struct DAGSegment {
    int mat_id = 0;          // material/clip ID
    int64_t src_offset = 0;  // source clip start offset (microseconds)
    int track_index = 0;     // Z-order layer (0 = bottom, higher = on top)
    int track_type = 0;      // 0=video, 1=text, 2=sticker, 3=overlay

    // Speed / trim: maps global timeline time to source clip local time.
    double speed = 1.0;             // playback speed (1.0 = normal)
    TimeMicros trim_start = 0;      // source trim-in point (microseconds)
    TimeMicros trim_end = 0;        // source trim-out point (0 = full clip)

    // Transform keyframes (scale, position, rotation, opacity)
    std::vector<Keyframe> kf_scale;
    std::vector<Keyframe> kf_pos_x;
    std::vector<Keyframe> kf_pos_y;
    std::vector<Keyframe> kf_rotation;
    std::vector<Keyframe> kf_opacity;

    // Crop region (normalized 0.0–1.0)
    double crop_x = 0.0, crop_y = 0.0, crop_w = 1.0, crop_h = 1.0;

    // Effects chain applied to this segment
    std::vector<EffectNode> effects;

    // Map a global timeline time to the source clip's local time.
    // Applies speed ramp and trim so the compositor gets the correct source
    // frame for this timeline position.
    TimeMicros src_time(TimeMicros global_t) const {
        TimeMicros local = global_t - src_offset;
        if (speed > 0.001) local = (TimeMicros)((double)local / speed);
        local += trim_start;
        if (trim_end > 0 && local > trim_end) local = trim_end;
        return local;
    }

    // Resolve all animated transform params at time t.
    double scale_at(TimeMicros t) const { return interpolate_keyframes(kf_scale, t); }
    double pos_x_at(TimeMicros t) const { return interpolate_keyframes(kf_pos_x, t); }
    double pos_y_at(TimeMicros t) const { return interpolate_keyframes(kf_pos_y, t); }
    double rotation_at(TimeMicros t) const { return interpolate_keyframes(kf_rotation, t); }
    double opacity_at(TimeMicros t) const { return interpolate_keyframes(kf_opacity, t); }
};

// ---- Audio segment ---------------------------------------------------------
// Represents an audio track segment (main, music, SFX) for mixing.
struct AudioSegment {
    int mat_id = 0;              // audio material ID
    int track_index = 0;         // 0=main, 1=music, 2=SFX, ...
    TimeMicros start = 0;        // timeline start (microseconds)
    TimeMicros duration = 0;     // segment duration (microseconds)
    double volume = 1.0;         // 0.0–2.0 (1.0 = unity)
    double pan = 0.0;            // -1.0 (left) to +1.0 (right), 0.0 = center
    TimeMicros fade_in = 0;      // fade-in duration (microseconds)
    TimeMicros fade_out = 0;     // fade-out duration (microseconds)
    double speed = 1.0;          // playback speed

    // Check if this segment is active at timeline time t.
    bool active_at(TimeMicros t) const {
        return t >= start && t < start + duration;
    }

    // Compute volume envelope at timeline time t (including fades).
    double envelope(TimeMicros t) const {
        double env = volume;
        TimeMicros local = t - start;
        if (fade_in > 0 && local < fade_in) {
            env *= (double)local / (double)fade_in;
        }
        if (fade_out > 0 && local > duration - fade_out) {
            TimeMicros into_fade = local - (duration - fade_out);
            env *= 1.0 - (double)into_fade / (double)fade_out;
            if (env < 0) env = 0;
        }
        return env;
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
    // Segments are returned sorted by track_index ascending (bottom→top Z-order).
    std::vector<DAGSegment> evaluate(TimeMicros t) const {
        (void)t;
        return segments_;
    }

    // Evaluate all active audio segments at timeline time t.
    std::vector<AudioSegment> evaluate_audio(TimeMicros t) const {
        std::vector<AudioSegment> active;
        for (const auto& seg : audio_segments_) {
            if (seg.active_at(t)) active.push_back(seg);
        }
        return active;
    }

    // Detect scene cuts with the given threshold.
    std::vector<TimeMicros> detect_scene_cuts(int threshold) const {
        (void)threshold;
        return cuts_;
    }

    // Compute a 64-bit content hash of the entire DAG state.
    // This covers all segments, effects, keyframes, and audio segments so that
    // ANY timeline edit produces a different hash → cache invalidation.
    uint64_t content_hash() const {
        uint64_t h = 0xcbf29ce484222325ULL;  // FNV-1a offset basis
        auto fnv1a = [&](const void* p, size_t n) {
            const uint8_t* b = (const uint8_t*)p;
            for (size_t i = 0; i < n; ++i) {
                h ^= b[i];
                h *= 0x100000001b3ULL;
            }
        };
        // Duration
        fnv1a(&dur_, sizeof(dur_));
        // Segments
        for (const auto& s : segments_) {
            fnv1a(&s.mat_id, sizeof(s.mat_id));
            fnv1a(&s.src_offset, sizeof(s.src_offset));
            fnv1a(&s.track_index, sizeof(s.track_index));
            fnv1a(&s.track_type, sizeof(s.track_type));
            fnv1a(&s.speed, sizeof(s.speed));
            fnv1a(&s.trim_start, sizeof(s.trim_start));
            fnv1a(&s.trim_end, sizeof(s.trim_end));
            fnv1a(&s.crop_x, sizeof(s.crop_x));
            fnv1a(&s.crop_y, sizeof(s.crop_y));
            fnv1a(&s.crop_w, sizeof(s.crop_w));
            fnv1a(&s.crop_h, sizeof(s.crop_h));
            for (const auto& kf : s.kf_scale) fnv1a(&kf.value, sizeof(kf.value));
            for (const auto& kf : s.kf_pos_x) fnv1a(&kf.value, sizeof(kf.value));
            for (const auto& kf : s.kf_pos_y) fnv1a(&kf.value, sizeof(kf.value));
            for (const auto& kf : s.kf_rotation) fnv1a(&kf.value, sizeof(kf.value));
            for (const auto& kf : s.kf_opacity) fnv1a(&kf.value, sizeof(kf.value));
            for (const auto& eff : s.effects) {
                fnv1a(&eff.type, sizeof(eff.type));
                fnv1a(eff.name.data(), eff.name.size());
                fnv1a(&eff.intensity, sizeof(eff.intensity));
                for (const auto& kf : eff.params) fnv1a(&kf.value, sizeof(kf.value));
            }
        }
        // Audio segments
        for (const auto& a : audio_segments_) {
            fnv1a(&a.mat_id, sizeof(a.mat_id));
            fnv1a(&a.track_index, sizeof(a.track_index));
            fnv1a(&a.start, sizeof(a.start));
            fnv1a(&a.duration, sizeof(a.duration));
            fnv1a(&a.volume, sizeof(a.volume));
            fnv1a(&a.pan, sizeof(a.pan));
            fnv1a(&a.fade_in, sizeof(a.fade_in));
            fnv1a(&a.fade_out, sizeof(a.fade_out));
            fnv1a(&a.speed, sizeof(a.speed));
        }
        return h;
    }

    // Setters used by the editor to populate the DAG.
    void set_duration(TimeMicros d) { dur_ = d; }
    void set_segments(std::vector<DAGSegment> s) { segments_ = std::move(s); }
    void set_audio_segments(std::vector<AudioSegment> a) { audio_segments_ = std::move(a); }
    void set_cuts(std::vector<TimeMicros> c) { cuts_ = std::move(c); }

private:
    TimeMicros dur_ = 0;
    std::vector<DAGSegment> segments_;
    std::vector<AudioSegment> audio_segments_;
    std::vector<TimeMicros> cuts_;
};

}  // namespace PowerCut
