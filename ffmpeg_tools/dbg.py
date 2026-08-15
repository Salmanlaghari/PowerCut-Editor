import subprocess, os
FF="./ffmpeg"
def b(path,t,w=320,h=240):
    r=subprocess.run([FF,"-y","-ss",str(t),"-i",path,"-frames:v","1","-pix_fmt","gray","-f","rawvideo","frame.raw"],capture_output=True)
    if not os.path.exists("frame.raw"): return None
    d=open("frame.raw","rb").read()
    if len(d)<w*h: return None
    return sum(d)/ (w*h)
for name in ["op_no.mp4","op_yes.mp4"]:
    print(name, "exists", os.path.exists(name))
    for t in [0.1,1.0,2.0,2.5]:
        try:
            v=b(name,t)
            print(f"  t={t}: {v}")
        except Exception as e:
            print("ERR",e)
