package com.example.forgeplan.core.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.forgeplan.core.model.TaskAttachment
import com.example.forgeplan.core.model.TaskAttachmentPayload
import com.example.forgeplan.core.network.SupabaseApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskAttachmentRepository {

    fun uploadAttachment(
        context: Context,
        taskId: Long,
        uri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val fileName = getFileName(context, uri)

        val payload = TaskAttachmentPayload(
            task_id = taskId,
            file_name = fileName,
            file_url = uri.toString(),
            file_type = getFileType(context, uri)
        )

        SupabaseApi.service.createTaskAttachment(payload)
            .enqueue(object : Callback<List<TaskAttachment>> {
                override fun onResponse(
                    call: Call<List<TaskAttachment>>,
                    response: Response<List<TaskAttachment>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError("Erro ao guardar anexo: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<TaskAttachment>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido ao guardar anexo.")
                }
            })
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var fileName = "attachment_${System.currentTimeMillis()}"

        val cursor = context.contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            if (it.moveToFirst() && nameIndex >= 0) {
                fileName = it.getString(nameIndex)
            }
        }

        return fileName
    }

    private fun getFileType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "application/octet-stream"
    }
}