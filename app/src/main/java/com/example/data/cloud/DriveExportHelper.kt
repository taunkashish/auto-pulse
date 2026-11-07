package com.example.data.cloud

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.TripData
import java.io.File
import java.io.FileOutputStream

object DriveExportHelper {

    /**
     * Writes JSON file to app cache and creates an Intent to save/share to Google Drive.
     */
    fun exportToJson(context: Context, trip: TripData): Intent {
        val fileName = "AutoPulse_Trip_${trip.id.take(8)}.json"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            out.write(trip.toJsonString().toByteArray(Charsets.UTF_8))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AutoPulse Telemetry Log: ${trip.title}")
            putExtra(Intent.EXTRA_TEXT, "Exported vehicle telemetry performance log (${trip.formattedDate})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(intent, "Save to Google Drive / Export JSON")
    }

    /**
     * Writes CSV file to app cache and creates an Intent to save/share to Google Drive / Google Sheets.
     */
    fun exportToCsv(context: Context, trip: TripData): Intent {
        val fileName = "AutoPulse_Trip_${trip.id.take(8)}.csv"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            out.write(trip.toCsvString().toByteArray(Charsets.UTF_8))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AutoPulse Telemetry CSV: ${trip.title}")
            putExtra(Intent.EXTRA_TEXT, "Exported vehicle telemetry dataset for Google Sheets (${trip.formattedDate})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(intent, "Save to Google Drive / Export CSV")
    }
}
