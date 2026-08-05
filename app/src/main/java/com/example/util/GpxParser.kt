package com.example.util

import com.example.data.model.GpxPoint
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object GpxParser {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parse(inputStream: InputStream): List<GpxPoint> {
        val points = mutableListOf<GpxPoint>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentLat: Double? = null
            var currentLng: Double? = null
            var currentEle: Double? = null
            var currentTime: Long? = null
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name.lowercase()
                        if (currentTag == "trkpt" || currentTag == "rtept") {
                            val latStr = parser.getAttributeValue(null, "lat")
                            val lonStr = parser.getAttributeValue(null, "lon")
                            currentLat = latStr?.toDoubleOrNull()
                            currentLng = lonStr?.toDoubleOrNull()
                            currentEle = null
                            currentTime = null
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty()) {
                            when (currentTag) {
                                "ele" -> currentEle = text.toDoubleOrNull()
                                "time" -> currentTime = parseIsoTime(text)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name.lowercase()
                        if (name == "trkpt" || name == "rtept") {
                            if (currentLat != null && currentLng != null) {
                                points.add(GpxPoint(currentLat, currentLng, currentEle, currentTime))
                            }
                            currentLat = null
                            currentLng = null
                            currentEle = null
                            currentTime = null
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return points
    }

    fun parse(xmlString: String): List<GpxPoint> {
        return parse(xmlString.byteInputStream(Charsets.UTF_8))
    }

    private fun parseIsoTime(text: String): Long? {
        return try {
            val cleanText = text.replace("Z", "").take(19)
            isoFormat.parse(cleanText)?.time
        } catch (e: Exception) {
            null
        }
    }

    fun exportGpx(points: List<GpxPoint>, trackName: String = "Vol Paramoteur"): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"Paramoteur App\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <trk>\n")
        sb.append("    <name>").append(trackName).append("</name>\n")
        sb.append("    <trkseg>\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        for (pt in points) {
            sb.append("      <trkpt lat=\"").append(pt.lat).append("\" lon=\"").append(pt.lng).append("\">\n")
            if (pt.ele != null) {
                sb.append("        <ele>").append(pt.ele).append("</ele>\n")
            }
            if (pt.time != null) {
                val formattedTime = dateFormat.format(java.util.Date(pt.time))
                sb.append("        <time>").append(formattedTime).append("</time>\n")
            }
            sb.append("      </trkpt>\n")
        }

        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>")
        return sb.toString()
    }
}
