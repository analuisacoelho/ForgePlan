package com.example.forgeplan.projects.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.forgeplan.core.model.Project
import com.example.forgeplan.core.model.Task
import java.io.File
import java.io.FileOutputStream

fun exportProjectFile(
    context: Context,
    project: Project,
    tasks: List<Task>,
    format: String
) {
    val file = when (format) {
        "PDF" -> createProjectPdf(context, project, tasks)
        "X" -> createProjectCsv(context, project, tasks)
        "W" -> createProjectTxt(context, project, tasks)
        else -> createProjectTxt(context, project, tasks)
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val mimeType = when (format) {
        "PDF" -> "application/pdf"
        "X" -> "text/csv"
        "W" -> "text/plain"
        else -> "text/plain"
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(intent, "Export project")
    )
}

private fun createProjectPdf(
    context: Context,
    project: Project,
    tasks: List<Task>
): File {
    val file = File(context.cacheDir, "${safeName(project.name)}.pdf")

    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)

    val canvas = page.canvas
    val paint = Paint().apply {
        textSize = 18f
        isFakeBoldText = true
    }

    var y = 50f

    canvas.drawText("ForgePlan - Project Export", 40f, y, paint)

    y += 40f
    paint.textSize = 16f
    canvas.drawText("Project: ${project.name}", 40f, y, paint)

    y += 28f
    paint.textSize = 12f
    paint.isFakeBoldText = false
    canvas.drawText("Status: ${project.status ?: "No status"}", 40f, y, paint)

    y += 20f
    canvas.drawText("Priority: ${project.priority ?: "No priority"}", 40f, y, paint)

    y += 20f
    canvas.drawText("Start: ${project.start_date ?: "No date"}", 40f, y, paint)

    y += 20f
    canvas.drawText("End: ${project.end_date ?: "No date"}", 40f, y, paint)

    y += 35f
    paint.isFakeBoldText = true
    canvas.drawText("Tasks", 40f, y, paint)

    paint.isFakeBoldText = false

    tasks.forEach { task ->
        y += 24f

        if (y > 800f) return@forEach

        canvas.drawText(
            "- ${task.title} | ${task.status ?: "No status"} | ${task.completion_rate ?: 0}%",
            40f,
            y,
            paint
        )
    }

    pdf.finishPage(page)

    FileOutputStream(file).use {
        pdf.writeTo(it)
    }

    pdf.close()

    return file
}

private fun createProjectCsv(
    context: Context,
    project: Project,
    tasks: List<Task>
): File {
    val file = File(context.cacheDir, "${safeName(project.name)}.csv")

    val content = buildString {
        appendLine("Project,Status,Priority,Start,End")
        appendLine("${project.name},${project.status ?: ""},${project.priority ?: ""},${project.start_date ?: ""},${project.end_date ?: ""}")
        appendLine()
        appendLine("Task,Status,Priority,Completion,Start,End")

        tasks.forEach { task ->
            appendLine(
                "${task.title},${task.status ?: ""},${task.priority ?: ""},${task.completion_rate ?: 0},${task.start_date ?: ""},${task.end_date ?: ""}"
            )
        }
    }

    file.writeText(content)

    return file
}

private fun createProjectTxt(
    context: Context,
    project: Project,
    tasks: List<Task>
): File {
    val file = File(context.cacheDir, "${safeName(project.name)}.txt")

    val content = buildString {
        appendLine("ForgePlan - Project Export")
        appendLine()
        appendLine("Project: ${project.name}")
        appendLine("Description: ${project.description ?: "No description"}")
        appendLine("Status: ${project.status ?: "No status"}")
        appendLine("Priority: ${project.priority ?: "No priority"}")
        appendLine("Start: ${project.start_date ?: "No date"}")
        appendLine("End: ${project.end_date ?: "No date"}")
        appendLine()
        appendLine("Tasks:")

        tasks.forEach { task ->
            appendLine("- ${task.title} | ${task.status ?: "No status"} | ${task.completion_rate ?: 0}%")
        }
    }

    file.writeText(content)

    return file
}

private fun safeName(value: String): String {
    return value
        .replace(" ", "_")
        .replace("/", "_")
        .replace("\\", "_")
}