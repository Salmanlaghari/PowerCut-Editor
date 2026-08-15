import subprocess, os
FF="./ffmpeg"
import meas
if not os.path.exists("white.mp4"):
    subprocess.run([FF,"-y","-f","lavfi","-i","color=c=white:s=320x240:r=30:d=3","-pix_fmt","yuv420p","white.mp4"],capture_output=True)

print("=== OPACITY KEYFRAME 1.0->0.0 over 0..2s ===")
for name,filt in [("no","colorchannelmixer=aa='if(between(t,0,2),(1.0+(-0.5)*(t-0)),0.0)'"),
                  ("yes","colorchannelmixer=aa='if(between(t,0,2),(1.0+(-0.5)*(t-0)),0.0)':eval=frame")]:
    subprocess.run([FF,"-y","-i","white.mp4","-vf",filt,"-pix_fmt","yuv420p",f"op_{name}.mp4"],capture_output=True)
    vals=[meas.brightness_at(f"op_{name}.mp4",t) for t in [0.1,1.0,2.0,2.5]]
    print(f"  {name}: t0.1={vals[0]:.1f} t1.0={vals[1]:.1f} t2.0={vals[2]:.1f} t2.5={vals[3]:.1f}")
