package com.powercut.editor.domain.processing

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

/**
 * v7.2 — REAL image-overlay studio, canvas drawing and Studio FX helpers.
 *
 * Every filter chain here was validated against a real FFmpeg 4.4.2 binary:
 *  - drawbox expressions must use `iw/ih` (input frame size); `w/h` fails with
 *    "Error when evaluating the expression".
 *  - the `color` source ignores `@alpha` on 4.4, so transparency is created
 *    with `geq=lum=...:a='0'`.
 *  - `format=auto` is not a valid value for the `format` filter (only for the
 *    overlay filter's `format` option).
 *  - VP9 alpha support varies by ffmpeg-kit build, so the encoded pixel format
 *    is verified at runtime and position-based animations fall back to the
 *    static image when alpha is unavailable (never a black box).
 */
object OverlayDrawingStudio {

    private const val TAG = "OverlayDrawingStudio"

    /**
     * Studio FX ids shown in the Studio panel, each mapping to a REAL FFmpeg -vf
     * chain. Keys are `studio_`-prefixed so they can never collide with
     * EffectCatalog ids that VideoProcessor.effectChain() also matches by
     * substring (vhs / sparkle / fire / bokeh / light_leak / lens_flare /
     * dream / rgb_split / vignette / ...).
     */
    val STUDIO_FX_CHAINS: Map<String, String> = mapOf(
        "studio_cinematic_bars" to "drawbox=x=0:y=0:w=iw:h=ih*0.05:color=black@1:t=fill,drawbox=x=0:y=ih*0.95:w=iw:h=ih*0.05:color=black@1:t=fill",
        "studio_film_burn" to "eq=brightness='0.3*exp(-t*2)':saturation=1.4:eval=frame,colorbalance=rs=0.2:rm=0.15",
        "studio_tape" to "noise=alls=18:allf=t+u,curves=preset=vintage,boxblur=luma_radius=2:luma_power=1,eq=saturation=1.2:contrast=1.05",
        "studio_glitch_tv" to "noise=alls=30:allf=t+u,chromashift=cbh=-4:cbv=2:crh=4:crv=-2,eq=contrast=1.15",
        "studio_chroma_split" to "chromashift=cbh=-5:cbv=3:crh=5:crv=-3",
        "studio_prism" to "chromashift=cbh=-6:crv=6,hue=h=15,eq=saturation=1.4",
        "studio_lightning" to "eq=brightness='0.25*exp(-t*3)':eval=frame:enable='lt(mod(t,3),0.4)',noise=alls=10:allf=t",
        "studio_fireworks" to "eq=saturation=1.4:contrast=1.15,noise=alls=20:allf=t+u",
        "studio_frozen" to "colorbalance=bs=0.2:bm=0.15,eq=saturation=0.85:contrast=1.15:brightness=0.03,boxblur=luma_radius=1:luma_power=1",
        "studio_ember" to "colorbalance=rs=0.25:rm=0.18:gs=0.05,eq=brightness=0.06:saturation=1.4:contrast=1.2",
        "studio_ripple" to "boxblur=luma_radius=3:luma_power=1,noise=alls=6:allf=t,eq=saturation=1.15",
        "studio_starfield" to "noise=alls=30:allf=t+u,eq=brightness=0.05:contrast=1.1",
        "studio_orb" to "boxblur=luma_radius=15:luma_power=2,eq=brightness=0.05:saturation=1.2",
        "studio_light_spill" to "eq=brightness=0.08:saturation=1.15,colorbalance=rs=0.1:rm=0.08",
        "studio_scratch" to "noise=alls=10:allf=t+u,eq=contrast=1.05:saturation=0.95",
        "studio_soft_blur" to "gblur=sigma=4,eq=brightness=0.08:saturation=1.15",
        "studio_dark_edges" to "vignette=angle=PI/3,eq=contrast=1.1:saturation=1.05",
        "studio_shimmer" to "eq=brightness='0.08+0.08*sin(t*6)':eval=frame,eq=saturation=1.25:contrast=1.1",
        "studio_glare" to "eq=brightness=0.08:contrast=1.1,vignette=angle=PI/5,colorbalance=rs=0.06:rm=0.04",
        "studio_burst" to "eq=brightness='0.06+0.1*sin(t*5)':contrast=1.15:saturation=1.2:eval=frame"
    )

    /**
     * Per-overlay image FX (real single-input filters applied to the overlay
     * image). Validated on FFmpeg 4.4: geq needs a luma expression alongside
     * `a=`; `tint` does not exist in 4.4; tblend drops single frames.
     */
    fun overlayFxChain(effect: String): String {
        return when (effect.lowercase().replace(" ", "_")) {
            "none" -> ""
            "grayscale" -> "hue=s=0"
            "sepia" -> "colorchannelmixer=.393:.769:.189:.349:.686:.168:.272:.534:.131"
            "invert" -> "negate"
            "vignette" -> "vignette=angle=PI/4"
            "blur_edge" -> "boxblur=luma_radius=3:luma_power=1"
            "round_corners" -> "format=rgba,geq=lum='lum(X,Y)':a='if(lt(hypot(max(abs(X-(W/2))-(W/2-0.08*W),0),max(abs(Y-(H/2))-(H/2-0.08*H),0)),0.08*W),255,0)'"
            "circle_mask" -> "format=rgba,geq=lum='lum(X,Y)':a='if(lt(hypot(X-(W/2),Y-(H/2)),min(W,H)/2),255,0)'"
            "shadow" -> "colorchannelmixer=aa=0.85,drawbox=x=0:y=0:w=iw:h=ih:color=black@0.35:t=10"
            "glow" -> "gblur=sigma=6,eq=brightness=0.08:saturation=1.15"
            "border" -> "drawbox=x=0:y=0:w=iw:h=ih:color=white@0.8:t=4"
            "gradient_bg" -> "drawbox=x=0:y=0:w=iw:h=ih:color=0x1a1a2e@0.45:t=fill"
            "neon_edge" -> "drawbox=x=0:y=0:w=iw:h=ih:color=0x00ffff@0.8:t=6"
            "3d_pop" -> "eq=contrast=1.25:saturation=1.25,unsharp=5:5:0.8:5:5:0"
            "glass" -> "boxblur=luma_radius=2:luma_power=1,eq=brightness=0.05"
            "frosted" -> "boxblur=luma_radius=8:luma_power=1,eq=brightness=0.06:saturation=0.9"
            else -> ""
        }
    }

    /**
     * Renders freehand drawing strokes (normalized JSON) as filled drawbox
     * circles along each stroke path. Stroke format:
     *   [ { "color": "#RRGGBB", "size": 0.012, "opacity": 1.0,
     *       "points": [[x,y], ...] }, ... ]   (x,y normalized 0..1)
     */
    fun drawingChain(drawingJson: String): List<String> {
        return try {
            val arr = org.json.JSONArray(drawingJson)
            val filters = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val stroke = arr.getJSONObject(i)
                val color = stroke.optString("color", "#FFFFFF").replace("#", "0x")
                val sizeFrac = stroke.optDouble("size", 0.012).toFloat().coerceIn(0.002f, 0.2f)
                val opacity = stroke.optDouble("opacity", 1.0).toFloat().coerceIn(0.05f, 1.0f)
                val pts = stroke.optJSONArray("points") ?: continue
                if (pts.length() < 1) continue
                var px = pts.getJSONArray(0).getDouble(0).coerceIn(0.0, 1.0)
                var py = pts.getJSONArray(0).getDouble(1).coerceIn(0.0, 1.0)
                // drawbox expressions must use iw/ih (input frame size) — w/h
                // fails on FFmpeg 4.4 with "Error when evaluating the expression".
                val s = sizeFrac
                fun box(cx: Double, cy: Double): String =
                    "drawbox=x='(iw*$cx)-(min(iw,ih)*$s)/2':y='(ih*$cy)-(min(iw,ih)*$s)/2':w='min(iw,ih)*$s':h='min(iw,ih)*$s':color=${color}@$opacity:t=fill"
                filters.add(box(px, py))
                for (j in 1 until pts.length()) {
                    val nx = pts.getJSONArray(j).getDouble(0).coerceIn(0.0, 1.0)
                    val ny = pts.getJSONArray(j).getDouble(1).coerceIn(0.0, 1.0)
                    filters.add(box((px + nx) / 2.0, (py + ny) / 2.0))
                    filters.add(box(nx, ny))
                    px = nx; py = ny
                }
            }
            filters
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Pre-renders the overlay image with the selected overlay FX and entrance
     * animation using a REAL FFmpeg pass BEFORE the main export:
     *  - FX only            → still PNG with the FX baked in.
     *  - FX + animation     → 2s WebM (VP9+alpha) at w×h with the entrance baked
     *                         in; overlay's eof_action=repeat holds the last
     *                         frame, so the entrance plays once then the image
     *                         stays at its final position.
     * Returns the path to the rendered asset (registered via [registerTemp])
     * or the original path when nothing was requested / the pre-pass failed
     * (export still works).
     */
    fun preprocessOverlayImage(
        context: Context,
        path: String,
        fx: String,
        anim: String,
        w: Int,
        h: Int,
        registerTemp: (File) -> Unit
    ): String {
        val fxChain = overlayFxChain(fx)
        val a = anim.lowercase().replace(" ", "_")
        if (fxChain.isEmpty() && a == "none") return path
        return try {
            if (a == "none") {
                val out = File(context.cacheDir, "ovl_${System.currentTimeMillis()}.png")
                val vf = "$fxChain,scale=$w:$h,format=rgba"
                val s = FFmpegKit.executeWithArguments(
                    arrayOf("-y", "-i", path, "-vf", vf, "-frames:v", "1", out.absolutePath)
                )
                if (ReturnCode.isSuccess(s.returnCode) && out.exists() && out.length() > 0L) {
                    registerTemp(out)
                    Log.d(TAG, "Overlay FX pre-pass done: $fx -> ${out.absolutePath}")
                    out.absolutePath
                } else {
                    Log.e(TAG, "Overlay FX pre-pass failed: ${s.failStackTrace}")
                    path
                }
            } else {
                val out = File(context.cacheDir, "ovl_${System.currentTimeMillis()}.webm")
                val baseFilter = if (fxChain.isEmpty()) "" else "$fxChain,"
                val imageLabel = when (a) {
                    "fade_in" -> "[1:v]scale=$w:$h,format=rgba,fade=t=in:st=0:d=0.6:alpha=1[i]"
                    "zoom_in" -> "[1:v]scale=$w:$h,format=rgba,zoompan=z='1.4-0.4*min(on/45,1)':d=1:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=${w}x${h}[i]"
                    "zoom_out" -> "[1:v]scale=$w:$h,format=rgba,zoompan=z='0.5+0.5*min(on/45,1)':d=1:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=${w}x${h}[i]"
                    "pop" -> "[1:v]scale=$w:$h,format=rgba,zoompan=z='0.5+0.5*min(on/15,1)':d=1:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=${w}x${h}[i]"
                    "rotate" -> "[1:v]scale=$w:$h,format=rgba,rotate=a='PI*min(t/1,1)/4':fillcolor=none[i]"
                    "flip" -> "[1:v]scale=$w:$h,format=rgba,hflip=enable='lt(t,0.3)'[i]"
                    else -> "[1:v]scale=$w:$h,format=rgba[i]" // slide/bounce/elastic: moved via overlay x/y below
                }
                val pos = when (a) {
                    "slide_left" -> "x='-W*(1-min(t/0.7,1))':y=0"
                    "slide_right" -> "x='W*(1-min(t/0.7,1))':y=0"
                    "slide_up" -> "y='-H*(1-min(t/0.7,1))':x=0"
                    "slide_down" -> "y='H*(1-min(t/0.7,1))':x=0"
                    "bounce" -> "y='-60*abs(sin(t*7))*(1-min(t/1.2,1)/1.2)':x=0"
                    "elastic" -> "y='-50*exp(-2.5*t)*abs(cos(6*t))':x=0"
                    else -> "x=0:y=0"
                }
                // geq makes the base fully transparent (the color source ignores
                // @alpha on FFmpeg 4.4); the image enters over it and stays.
                val fc = "[0:v]format=rgba,geq=lum='lum(X,Y)':a='0'[bg];" +
                        "$baseFilter$imageLabel;" +
                        "[bg][i]overlay=$pos:shortest=1[v]"
                val s = FFmpegKit.executeWithArguments(arrayOf(
                    "-y", "-f", "lavfi", "-i", "color=c=black:s=${w}x${h}:d=2:r=30",
                    "-loop", "1", "-t", "2", "-i", path,
                    "-filter_complex", fc,
                    "-map", "[v]", "-t", "2",
                    "-c:v", "libvpx-vp9", "-pix_fmt", "yuva420p", "-b:v", "2M",
                    out.absolutePath
                ))
                if (ReturnCode.isSuccess(s.returnCode) && out.exists() && out.length() > 0L) {
                    // Some ffmpeg-kit builds lack VP9 alpha: verify the encoded
                    // pixel format before trusting transparency.
                    val alphaOk = try {
                        val probe = FFprobeKit.getMediaInformation(out.absolutePath)
                        val stream = probe.mediaInformation?.streams?.firstOrNull()
                        // StreamInformation.getFormat() exposes the encoded pixel
                        // format ("yuva420p" for VP9 alpha) in this ffmpeg-kit
                        // fork — there is no `pixelFormat` accessor.
                        (stream?.format ?: "").contains("yuva")
                    } catch (e: Exception) {
                        Log.w(TAG, "Overlay alpha probe failed: ${e.message}")
                        false
                    }
                    if (alphaOk) {
                        registerTemp(out)
                        Log.d(TAG, "Overlay entrance pre-pass done (alpha): $a -> ${out.absolutePath}")
                        out.absolutePath
                    } else if (a == "zoom_in" || a == "zoom_out" || a == "pop") {
                        // Opaque-safe animations still work without alpha.
                        registerTemp(out)
                        Log.d(TAG, "Overlay entrance pre-pass done (opaque): $a -> ${out.absolutePath}")
                        out.absolutePath
                    } else {
                        Log.w(TAG, "VP9 alpha unavailable — falling back to static overlay for '$a'")
                        out.delete()
                        path
                    }
                } else {
                    Log.e(TAG, "Overlay entrance pre-pass failed ($a): ${s.failStackTrace}")
                    path
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "preprocessOverlayImage exception", e)
            path
        }
    }
}
