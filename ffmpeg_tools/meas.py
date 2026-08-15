import subprocess, sys
import os
FF="./ffmpeg"
def brightness_at(path, tsec, w=320,h=240):
    r=subprocess.run([FF,"-y","-ss",str(tsec),"-i",path,"-frames:v","1","-pix_fmt","gray","-f","rawvideo","frame.raw"],capture_output=True)
    if not os.path.exists("frame.raw"): return None
    data=open("frame.raw","rb").read()
    if len(data)<w*h: return None
    return sum(data[i] for i in range(w*h))/ (w*h)
def brightness_over(path, w=320,h=240):
    # dump all frames
    r=subprocess.run([FF,"-i",path,"-pix_fmt","gray","-f","rawvideo","all.raw"],capture_output=True)
    if not os.path.exists("all.raw"):

        return None
    data=open("all.raw","rb").read()
    n=len(data)//(w*h)
    means=[]
    for i in range(n):
        chunk=data[i*w*h:(i+1)*w*h]
        means.append(sum(chunk)/len(chunk))
    return means
