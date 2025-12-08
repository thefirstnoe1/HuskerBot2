package org.j3y.HuskerBot2.service

import org.bytedeco.opencv.global.opencv_core
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.RectVector
import org.bytedeco.opencv.opencv_core.Size
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.color.ColorSpace
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import java.awt.image.ConvolveOp
import java.awt.image.DataBufferByte
import java.awt.image.Kernel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.stream.MemoryCacheImageOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Service
class DeepFryProcessor {
    private val log = LoggerFactory.getLogger(DeepFryProcessor::class.java)

    private val restTemplate: RestTemplate = RestTemplate()

    companion object {
        private val overlayImages: List<BufferedImage> by lazy {
            try {
                val resolver = PathMatchingResourcePatternResolver(DeepFryProcessor::class.java.classLoader)
                val resources: Array<Resource> =
                    resolver.getResources("classpath*:/deepfry/chars/*.png") +
                            resolver.getResources("classpath*:/deepfry/emotes/*.png")
                resources.mapNotNull { res ->
                    try {
                        res.inputStream.use { ImageIO.read(it) }
                    } catch (_: Exception) { null }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private val flareImages: List<BufferedImage> by lazy {
            try {
                val resolver = PathMatchingResourcePatternResolver(DeepFryProcessor::class.java.classLoader)
                val resources: Array<Resource> = resolver.getResources("classpath*:/deepfry/flares/*.png")
                resources.mapNotNull { res ->
                    try {
                        res.inputStream.use { ImageIO.read(it) }
                    } catch (_: Exception) { null }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun isValidHttpUrl(url: String): Boolean = try {
        val scheme = java.net.URI(url).scheme?.lowercase()
        scheme == "http" || scheme == "https"
    } catch (_: Exception) { false }

    fun downloadImage(url: String): BufferedImage? {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.USER_AGENT, "HuskerBot2-DeepFry/1.0")
        headers.accept = listOf(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.IMAGE_GIF, MediaType.ALL)
        val entity = HttpEntity<Void>(headers)
        val response = restTemplate.exchange(url, HttpMethod.GET, entity, ByteArray::class.java)
        if (!response.statusCode.is2xxSuccessful) return null
        val contentType = response.headers.contentType
        if (contentType == null || contentType.type.lowercase() != "image") return null
        val data = response.body ?: return null
        return try { ImageIO.read(ByteArrayInputStream(data)) } catch (_: Exception) { null }
    }

    fun resizeIfTooLarge(img: BufferedImage, maxW: Int, maxH: Int): BufferedImage {
        val w = img.width
        val h = img.height
        if (w <= maxW && h <= maxH) return img
        val scale = min(maxW.toDouble() / w, maxH.toDouble() / h)
        val nw = (w * scale).toInt().coerceAtLeast(1)
        val nh = (h * scale).toInt().coerceAtLeast(1)
        val out = BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.drawImage(img, 0, 0, nw, nh, null)
        g.dispose()
        return out
    }

    fun fryToJpeg(img: BufferedImage, qualityRange: ClosedFloatingPointRange<Float> = 0.05f..0.25f): ByteArray {
        val resized = resizeIfTooLarge(img, 2048, 2048)
        val withEyes = coverEyesWithFlares(resized)
        val withOverlays = addRandomOverlays(withEyes)
        val fried = deepFryRandom(withOverlays)
        val q = Random.Default.nextDouble(qualityRange.start.toDouble(), qualityRange.endInclusive.toDouble()).toFloat()
        return toLowQualityJpegBytes(fried, q)
    }

    private fun coverEyesWithFlares(img: BufferedImage): BufferedImage {
        val eyes = detectEyes(img)
        if (eyes.isEmpty()) return img
        if (flareImages.isEmpty()) return img
        val rng = Random(System.nanoTime())
        val flare = flareImages[rng.nextInt(flareImages.size)]
        val out = toRgb(img)
        val g = out.createGraphics() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            for (r in eyes) {
                try {
                    if (flare.width <= 0 || flare.height <= 0) continue
                    val cx = r.x + r.width / 2.0
                    val cy = r.y + r.height / 2.0
                    val baseSize = max(r.width, r.height).toDouble()
                    val target = (baseSize * rng.nextDouble(1.2, 2.2)).toInt().coerceAtLeast(8)
                    val baseScale = target / max(flare.width, flare.height).toDouble()
                    val extra = rng.nextDouble(0.9, 1.3)
                    val scale = baseScale * extra
                    val ow = max(1, (flare.width * scale).toInt())
                    val oh = max(1, (flare.height * scale).toInt())
                    val x = (cx - ow / 2.0 + rng.nextDouble(-0.15, 0.15) * ow).toInt()
                    val y = (cy - oh / 2.0 + rng.nextDouble(-0.15, 0.15) * oh).toInt()
                    val tx = AffineTransform()
                    tx.translate(x.toDouble(), y.toDouble())
                    tx.scale(ow / flare.width.toDouble(), oh / flare.height.toDouble())
                    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, rng.nextDouble(0.75, 1.0).toFloat())
                    g.drawImage(flare, tx, null)
                } catch (_: Exception) {
                }
            }
        } finally {
            g.dispose()
        }
        return out
    }

    private fun detectEyes(img: BufferedImage): List<Rectangle> {
        try {
            val gray = toGrayscaleMat(img)
            val classifier = getEyeCascade() ?: return emptyList()
            val rects = RectVector()
            classifier.detectMultiScale(gray, rects, 1.1, 3, 0, Size(20, 20), Size())
            val out = ArrayList<Rectangle>(rects.size().toInt())
            for (i in 0 until rects.size().toInt()) {
                val r = rects.get(i.toLong())
                out.add(Rectangle(r.x(), r.y(), r.width(), r.height()))
            }
            return out
        } catch (_: Exception) {
            return emptyList()
        }
    }

    private fun getEyeCascade(): CascadeClassifier? {
        val names = listOf(
            "haarcascade_eye.xml",
            "haarcascade_eye_tree_eyeglasses.xml"
        )
        for (n in names) {
            val cc = loadCascadeFromResources(n)
            if (cc != null && !cc.empty()) return cc
        }
        return null
    }

    private fun loadCascadeFromResources(resourceName: String): CascadeClassifier? {
        return try {
            val resolver = PathMatchingResourcePatternResolver(DeepFryProcessor::class.java.classLoader)
            val resources = resolver.getResources("classpath*:/$resourceName")
            val tempFile = kotlin.io.path.createTempFile(suffix = resourceName)
            resources.firstOrNull()?.inputStream.use { input ->
                if (input != null) {
                    java.nio.file.Files.copy(input, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                    CascadeClassifier(tempFile.toString())
                } else null
            }
        } catch (_: Exception) { null }
    }

    private fun toGrayscaleMat(img: BufferedImage): Mat {
        // Ensure 3-byte BGR backing for reliable data extraction
        val bgr: BufferedImage = if (img.type != BufferedImage.TYPE_3BYTE_BGR) {
            val tmp = BufferedImage(img.width, img.height, BufferedImage.TYPE_3BYTE_BGR)
            val g = tmp.createGraphics()
            g.drawImage(img, 0, 0, null)
            g.dispose()
            tmp
        } else img

        val data = (bgr.raster.dataBuffer as DataBufferByte).data
        val src = Mat(bgr.height, bgr.width, opencv_core.CV_8UC3)
        // Fill mat data
        val dp = src.data()
        dp.put(*data)
        val gray = Mat()
        opencv_imgproc.cvtColor(src, gray, opencv_imgproc.COLOR_BGR2GRAY)
        return gray
    }

    private fun addRandomOverlays(img: BufferedImage): BufferedImage {
        if (overlayImages.isEmpty()) return img
        val rng = Random(System.nanoTime())
        val out = toRgb(img)
        val g = out.createGraphics() as Graphics2D
        try {
            val count = rng.nextInt(5, 10)
            repeat(count) {
                val overlay = overlayImages[rng.nextInt(overlayImages.size)]
                if (overlay.width <= 0 || overlay.height <= 0) return@repeat
                val scale = rng.nextDouble(0.2, 0.7)
                val ow = (overlay.width * scale).toInt().coerceAtLeast(1)
                val oh = (overlay.height * scale).toInt().coerceAtLeast(1)
                val x = rng.nextInt(0, max(1, out.width - ow))
                val y = rng.nextInt(0, max(1, out.height - oh))
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, rng.nextDouble(0.6, 1.0).toFloat())
                g.drawImage(overlay, x, y, ow, oh, null)
            }
        } finally {
            g.dispose()
        }
        return out
    }

    private fun deepFryRandom(img: BufferedImage): BufferedImage {
        var out = toRgb(img)
        val rng = Random(System.nanoTime())
        // Random oversaturation
        out = oversaturate(out, rng.nextDouble(1.5, 2.5).toFloat())
        // Random brightness/contrast
        out = adjustBrightnessContrast(out, rng.nextDouble(-0.1, 0.3).toFloat(), rng.nextDouble(1.0, 1.6).toFloat())
        // Sharpen
        out = sharpen(out, rng.nextDouble(0.5, 1.5).toFloat())
        // Noise
        out = addNoise(out, rng.nextDouble(0.02, 0.06).toFloat())
        // Vignette
        out = vignette(out, rng.nextDouble(0.15, 0.35).toFloat())
        // Scanlines
        out = scanlines(out, rng.nextDouble(0.04, 0.12).toFloat(), rng.nextInt(2, 6))
        // Chromatic aberration
        out = chromaticAberration(out, rng.nextInt(1, 4))
        // Posterize a bit
        out = posterize(out, rng.nextInt(4, 12))
        return out
    }

    private fun toRgb(img: BufferedImage): BufferedImage {
        if (img.type == BufferedImage.TYPE_INT_RGB) return img
        val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.drawImage(img, 0, 0, null)
        g.dispose()
        return out
    }

    private fun toLowQualityJpegBytes(img: BufferedImage, quality: Float = 0.15f): ByteArray {
        val writer: ImageWriter = ImageIO.getImageWritersByFormatName("jpg").next()
        val baos = ByteArrayOutputStream()
        MemoryCacheImageOutputStream(baos).use { ios ->
            writer.output = ios
            val param: ImageWriteParam = writer.defaultWriteParam
            param.compressionMode = ImageWriteParam.MODE_EXPLICIT
            param.compressionQuality = quality
            writer.write(null, IIOImage(img, null, null), param)
            writer.dispose()
        }
        return baos.toByteArray()
    }

    // Effects implementations below
    private fun oversaturate(img: BufferedImage, saturation: Float): BufferedImage {
        val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        val op = ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null)
        op.filter(img, out)
        g.dispose()
        // crude saturation by blending towards average
        val raster = out.raster
        val pixels = IntArray(3)
        for (y in 0 until out.height) {
            for (x in 0 until out.width) {
                raster.getPixel(x, y, pixels)
                val avg = (pixels[0] + pixels[1] + pixels[2]) / 3f
                pixels[0] = (avg + (pixels[0] - avg) * saturation).toInt().coerceIn(0, 255)
                pixels[1] = (avg + (pixels[1] - avg) * saturation).toInt().coerceIn(0, 255)
                pixels[2] = (avg + (pixels[2] - avg) * saturation).toInt().coerceIn(0, 255)
                raster.setPixel(x, y, pixels)
            }
        }
        return out
    }

    private fun adjustBrightnessContrast(img: BufferedImage, brightness: Float, contrast: Float): BufferedImage {
        val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
        val rasterIn = img.raster
        val rasterOut = out.raster
        fun adj(v: Int): Int {
            val b = (v / 255f - 0.5f) * contrast + 0.5f + brightness
            return (b * 255f).toInt().coerceIn(0, 255)
        }
        val px = IntArray(3)
        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                rasterIn.getPixel(x, y, px)
                px[0] = adj(px[0])
                px[1] = adj(px[1])
                px[2] = adj(px[2])
                rasterOut.setPixel(x, y, px)
            }
        }
        return out
    }

    private fun addNoise(img: BufferedImage, amount: Float): BufferedImage {
        val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
        val rng = Random(System.nanoTime())
        val inRaster = img.raster
        val outRaster = out.raster
        val px = IntArray(3)
        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                inRaster.getPixel(x, y, px)
                val n = ((rng.nextDouble(-1.0, 1.0) * 255) * amount).toInt()
                px[0] = (px[0] + n).coerceIn(0, 255)
                px[1] = (px[1] + n).coerceIn(0, 255)
                px[2] = (px[2] + n).coerceIn(0, 255)
                outRaster.setPixel(x, y, px)
            }
        }
        return out
    }

    private fun kernelConvolve(img: BufferedImage, kernel: Kernel): BufferedImage {
        val op = ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null)
        val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
        op.filter(img, out)
        return out
    }

    private fun sharpen(img: BufferedImage, intensity: Float): BufferedImage {
        val i = intensity.coerceIn(0.1f, 2.0f)
        val k = floatArrayOf(
            0f, -i, 0f,
            -i, 1f + 4 * i, -i,
            0f, -i, 0f
        )
        val kernel = Kernel(3, 3, k)
        return kernelConvolve(img, kernel)
    }

    private fun vignette(img: BufferedImage, strength: Float): BufferedImage {
        val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.drawImage(img, 0, 0, null)
        val cx = img.width / 2.0
        val cy = img.height / 2.0
        val maxD = kotlin.math.sqrt((cx * cx + cy * cy)).toFloat()
        val raster = out.raster
        val px = IntArray(3)
        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                val dx = (x - cx).toFloat()
                val dy = (y - cy).toFloat()
                val d = kotlin.math.sqrt((dx * dx + dy * dy)) / maxD
                val v = (1.0f - d * strength).coerceIn(0f, 1f)
                raster.getPixel(x, y, px)
                px[0] = (px[0] * v).toInt()
                px[1] = (px[1] * v).toInt()
                px[2] = (px[2] * v).toInt()
                raster.setPixel(x, y, px)
            }
        }
        g.dispose()
        return out
    }

    private fun scanlines(img: BufferedImage, opacity: Float, spacing: Int): BufferedImage {
        val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.drawImage(img, 0, 0, null)
        g.color = Color(0, 0, 0, (opacity * 255).toInt().coerceIn(0, 255))
        for (y in 0 until img.height step spacing) {
            g.drawLine(0, y, img.width, y)
        }
        g.dispose()
        return out
    }

    private fun chromaticAberration(img: BufferedImage, shift: Int): BufferedImage {
        val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        fun sample(ix: Int, iy: Int): Int {
            val x = ix.coerceIn(0, img.width - 1)
            val y = iy.coerceIn(0, img.height - 1)
            return img.getRGB(x, y)
        }
        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                val r = sample(x + shift, y)
                val gCh = sample(x, y)
                val b = sample(x - shift, y)
                val rr = (r ushr 16) and 0xFF
                val gg = (gCh ushr 8) and 0xFF
                val bb = b and 0xFF
                val rgb = (rr shl 16) or (gg shl 8) or bb
                out.setRGB(x, y, rgb)
            }
        }
        g.dispose()
        return out
    }

    private fun posterize(img: BufferedImage, levels: Int): BufferedImage {
        val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB)
        val inRaster = img.raster
        val outRaster = out.raster
        fun q(v: Int): Int {
            val step = 255.0 / (levels - 1)
            return (kotlin.math.round(v / step) * step).toInt().coerceIn(0, 255)
        }
        val px = IntArray(3)
        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                inRaster.getPixel(x, y, px)
                px[0] = q(px[0])
                px[1] = q(px[1])
                px[2] = q(px[2])
                outRaster.setPixel(x, y, px)
            }
        }
        return out
    }
}
