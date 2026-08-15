import subprocess, os, math, statistics

FF="./ffmpeg"
def run(cmd):
    r=subprocess.run(cmd, capture_output=True, text=True)
    return r.returncode, r.stderr

# Build base: 3s solid bright video so we can measure opacity/scale changes
subprocess.run([FF,"-y","-f","lavfi","-i","color=c=white:s=320x240:r=30:d=3","-pix_fmt","yuv420p","white.mp4"],capture_output=True)
subprocess.run([FF,"-y","-f","lavfi","-i","testsrc=size=320x240:rate=30:duration=3","-pix_fmt","yuv420p","base.mp4"],capture_output=True)

def measure_brightness(path, tsec):
    # extract one frame at tsec, compute average luma via signalstats
    out="frame_%s.png"%(int(tsec*1000))
    r=subprocess.run([FF,"-y","-ss",str(tsec),"-i",path,"-frames:v","1","-pix_fmt","gray","-vf","signalstats","-f","rawvideo","/dev/null"],capture_output=True,text=True)
    # better: use signalstats + metadata
    r=subprocess.run([FF,"-y","-ss",str(tsec),"-i",path,"-frames:v","1","-vf","signalstats,metadata=print:key=lavfi.signalstats.YAVG","-f","null","-"],capture_output=True,text=True)
    for line in r.stderr.splitlines():
        if "lavfi.signalstats.YAVG" in line:
            return float(line.split("=")[1])
    return None

def avg_brightness(path):
    # measure YAVG for whole video via signalstats
    r=subprocess.run([FF,"-i",path,"-vf","signalstats,metadata=print:key=lavfi.signalstats.YAVG","-f","null","-"],capture_output=True,text=True)
    vals=[]
    for line in r.stderr.splitlines():
        if "lavfi.signalstats.YAVG" in line:
            try: vals.append(float(line.split("=")[1]))
            except: pass
    return statistics.mean(vals) if vals else None, min(vals) if vals else None, max(vals) if vals else None

print("=== OPACITY KEYFRAME TEST: 1.0 -> 0.0 over 0..2s ===")
# replicate: colorchannelmixer=aa='if(between(t,0,2),(1.0+(-0.5)*(t-0)),0.0)'
# WITHOUT eval=frame (current code)
op_no = "colorchannelmixer=aa='if(between(t,0,2),(1.0+(-0.5)*(t-0)),0.0)'"
run([FF,"-y","-i","white.mp4","-vf",op_no,"-pix_fmt","yuv420p","op_no.mp4"])
print("  without eval=frame:", avg_brightness("op_no.mp4"))
op_yes = "colorchannelmixer=aa='if(between(t,0,2),(1.0+(-0.5)*(t-0)),0.0)':eval=frame"
run([FF,"-y","-i","white.mp4","-vf",op_yes,"-pix_fmt","yuv420p","op_yes.mp4"])
print("  with eval=frame:", avg_brightness("op_yes.mp4"))
