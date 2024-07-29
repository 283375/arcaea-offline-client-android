package xyz.sevive.arcaeaoffline.helpers

import ai.onnxruntime.OrtSession
import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import org.apache.commons.io.IOUtils
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.ml.KNearest
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import xyz.sevive.arcaeaoffline.core.ArcaeaPartnerModifiers
import xyz.sevive.arcaeaoffline.core.database.entities.PlayResult
import xyz.sevive.arcaeaoffline.core.ocr.ImagePhashDatabase
import xyz.sevive.arcaeaoffline.core.ocr.device.CropBlackEdges
import xyz.sevive.arcaeaoffline.core.ocr.device.DeviceOcr
import xyz.sevive.arcaeaoffline.core.ocr.device.DeviceOcrResult
import xyz.sevive.arcaeaoffline.core.ocr.device.rois.DeviceRoisAutoSelector
import xyz.sevive.arcaeaoffline.core.ocr.device.rois.DeviceRoisAutoSelectorResult
import xyz.sevive.arcaeaoffline.core.ocr.device.rois.definition.DeviceRoisAutoT1
import xyz.sevive.arcaeaoffline.core.ocr.device.rois.definition.DeviceRoisAutoT2
import xyz.sevive.arcaeaoffline.core.ocr.device.rois.extractor.DeviceRoisExtractor
import xyz.sevive.arcaeaoffline.core.ocr.device.rois.masker.DeviceRoisMaskerAutoT1
import xyz.sevive.arcaeaoffline.core.ocr.device.rois.masker.DeviceRoisMaskerAutoT2
import xyz.sevive.arcaeaoffline.core.ocr.device.toScore
import xyz.sevive.arcaeaoffline.data.OcrDependencyPaths
import xyz.sevive.arcaeaoffline.helpers.context.getFilename
import java.io.FileNotFoundException

object DeviceOcrHelper {
    fun ocrImage(
        imageUri: Uri,
        context: Context,
        customKnnModel: KNearest? = null,
        customPhashDatabase: ImagePhashDatabase? = null,
        ortSession: OrtSession,
    ): DeviceOcrResult {
        val ocrDependencyPaths = OcrDependencyPaths(context)

        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw FileNotFoundException("Cannot open a input stream for $imageUri")

        val byteArray = inputStream.use { IOUtils.toByteArray(inputStream) }
        val img = Imgcodecs.imdecode(MatOfByte(*byteArray), Imgcodecs.IMREAD_COLOR)
        val imgCropped = CropBlackEdges.crop(img)

        val roisAutoType = DeviceRoisAutoSelector.select(img)
        val rois = when (roisAutoType) {
            DeviceRoisAutoSelectorResult.T1 -> DeviceRoisAutoT1(
                imgCropped.width(),
                imgCropped.height()
            )

            else -> DeviceRoisAutoT2(
                imgCropped.width(), imgCropped.height()
            )
        }
        val extractor = DeviceRoisExtractor(rois, imgCropped)
        val masker = when (roisAutoType) {
            DeviceRoisAutoSelectorResult.T1 -> DeviceRoisMaskerAutoT1()
            else -> DeviceRoisMaskerAutoT2()
        }
        val knnModel = customKnnModel ?: KNearest.load(ocrDependencyPaths.knnModelFile.path)
        val phashDatabase =
            customPhashDatabase ?: ImagePhashDatabase(ocrDependencyPaths.phashDatabaseFile.path)

        val ocr = DeviceOcr(extractor, masker, knnModel, phashDatabase, ortSession)
        return ocr.ocr()
    }

    fun ocrResultToScore(
        imageUri: Uri,
        context: Context,
        ocrResult: DeviceOcrResult,
        fallbackDate: Instant? = null,
        overrideDate: Instant? = null,
        customArcaeaPartnerModifiers: ArcaeaPartnerModifiers? = null,
    ): PlayResult {
        val arcaeaPartnerModifiers =
            customArcaeaPartnerModifiers ?: ArcaeaPartnerModifiers(context.assets)

        val byteArray = IOUtils.toByteArray(context.contentResolver.openInputStream(imageUri))

        val date = if (overrideDate != null) {
            overrideDate
        } else {
            val imgExif = ExifInterface(byteArray.inputStream())
            val imgExifDateTimeOriginal =
                imgExif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)

            if (imgExifDateTimeOriginal != null) {
                val localDateTime = LocalDateTime.parse(
                    imgExifDateTimeOriginal, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
                )
                var zonedDateTime = localDateTime.atZone(ZoneId.systemDefault())

                val imgExifOffsetTimeOriginal =
                    imgExif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)

                if (imgExifOffsetTimeOriginal != null) {
                    val zoneOffset = ZoneOffset.of(imgExifOffsetTimeOriginal)
                    zonedDateTime = zonedDateTime.withZoneSameLocal(zoneOffset)
                }

                zonedDateTime.toInstant()
            } else {
                fallbackDate
            }
        }

        val imgFilename = context.getFilename(imageUri)

        return ocrResult.toScore(
            arcaeaPartnerModifiers = arcaeaPartnerModifiers,
            date = date,
            comment = if (imgFilename != null) "OCR $imgFilename" else null,
        )
    }
}
