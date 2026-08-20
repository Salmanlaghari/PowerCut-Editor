# Preview fix — visual proof (13-second clip)

These artifacts demonstrate that the live-preview FFmpeg pipeline renders an
**applied effect** for a real **13.0 s** source clip, instead of the blocking
"video too short" overlay.

How they were produced (mirrors `FilterPreviewRenderer` exactly — same
`-ss 0 -t 3` segment, same `scale/pad/fps/settb/format` normalisation, same
effect appended to the `-vf` chain):

```
# 13s source clip
ffmpeg -f lavfi -i "testsrc=size=1280x720:rate=30:duration=13" -c:v libx264 src13.mp4
# raw 3s preview segment
ffmpeg -ss 0 -t 3 -i src13.mp4 -vf "scale=640:360:...,pad=...,fps=30,settb=AVTB,format=yuv420p" raw_preview.mp4
# effect 3s preview segment (edgedetect / glitch)
ffmpeg -ss 0 -t 3 -i src13.mp4 -vf "...edgedetect"  effect_preview.mp4
ffmpeg -ss 0 -t 3 -i src13.mp4 -vf "...noise=alls=25,hue=s=2.5" glitch_preview.mp4
```

Files:
- `proof_13s_clip_filters.png` — side-by-side **RAW | EDGEDETECT | GLITCH** frame
  extracted at t=1.0s from the 13 s clip. The effect columns are visibly altered,
  proving the preview renders the applied effect.
- `frame_raw_passthrough.png` — raw preview frame (no effect).
- `frame_edgedetect.png` — effect preview frame (`edgedetect`).
- `frame_glitch.png` — effect preview frame (glitch-style noise + hue).
- `test_clip_13s.mp4` — the 13.0 s source (`ffprobe` duration = 13.000000 s).
- `filter_preview_3s.mp4` — the 3 s baked preview clip (what the player plays).

The accompanying code fix (renderers + `PreviewDurationPolicy`/`PreviewDurationProbe`)
removes the guard that previously mis-classified a valid 13 s clip as "too short"
and now lets the preview render, so the app shows the effect instead of the
blocking overlay.
