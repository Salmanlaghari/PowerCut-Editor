import subprocess, statistics, os
FF="./ffmpeg"
import meas
def run(cmd):
    r=subprocess.run(cmd, capture_output=True)
    return r.returncode, r.stderr.decode(errors='replace')
# ensure white.mp4 exists
if not os.path.exists("white.mp4"):
    subprocess.run([FF,"-y","-f","lavfi","-i","color=c=white:s=320x240:r=30:d=3","-pix_fmt","yuv420p","white.mp4"],capture_output=True)

print("=== OPACITY KEYFRAME 1.0->0.0 over 0..2s ===")
op_no = "colorchannelmixer=aa='if(between(t,0,2),(1.0+(-0.5)*(t-0)),0.0)'"
op_yes = "colorchannelmixer=aa='if(between(t,0,2),(1.0+(-0.5)*(t-0)),0.0)':eval=frame"
run([FF,"-y","-i","white.mp4","-vf",op_no,"-pix_fmt","yuv420p","op_no.mp4"])
run([FF,"-y","-i","white.mp4","-vf",op_yes,"-pix_fmt","yuv420p","op_yes.mp4"])
for name in ["op_no.mp4","op_yes.mp4"]:
    means=meas.brightness_over(name)
    print(f"  {name}: first={means[0]:.1f} mid={means[len(means)//2]:.1f} last={means[-1]:.1f} min={min(means):.1f} max={max(means):.1f}")
