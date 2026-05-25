package com.example.forgeplan.core.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.forgeplan.core.model.TaskAttachment
import com.example.forgeplan.core.model.TaskAttachmentPayload
import com.example.forgeplan.core.network.SupabaseApi
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskAttachmentRepository {

    private val bucketName = "task-attachments"

    fun uploadAttachment(
        context: Context,
        taskId: Long,
        uri: Uri,
        onSuccess: (TaskAttachment?) -> Unit,
        onError: (String) -> Unit
    ) {
        val fileName = getFileName(context, uri)
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

        val bytes = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        }

        if (bytes == null) {
            onError("Não foi possível ler o ficheiro.")
            return
        }

        val safeName = fileName
            .replace(" ", "_")
            .replace("/", "_")
            .replace("\\", "_")

        val filePath = "tasks/$taskId/${System.currentTimeMillis()}_$safeName"

        val requestBody = RequestBody.create(
            MediaType.parse(mimeType),
            bytes
        )

        SupabaseApi.storageService.uploadFile(
            bucket = bucketName,
            filePath = filePath,
            contentType = mimeType,
            file = requestBody
        ).enqueue(object : Callback<Unit> {
            override fun onResponse(
                call: Call<Unit>,
                response: Response<Unit>
            ) {
                if (response.isSuccessful) {
                    val publicUrl = SupabaseApi.publicStorageUrl(
                        bucket = bucketName,
                        filePath = filePath
                    )

                    saveAttachmentMetadata(
                        taskId = taskId,
                        fileUrl = publicUrl,
                        fileName = fileName,
                        fileType = mimeType,
                        onSuccess = onSuccess,
                        onError = onError
                    )
                } else {
                    onError("Erro ao fazer upload do anexo: ${response.code()}")
                }
            }

            override fun onFailure(
                call: Call<Unit>,
                t: Throwable
            ) {
                onError(t.message ?: "Erro desconhecido no upload.")
            }
        })
    }

    private fun saveAttachmentMetadata(
        taskId: Long,
        fileUrl: String,
        fileName: String,
        fileType: String,
        onSuccess: (TaskAttachment?) -> Unit,
        onError: (String) -> Unit
    ) {
        val payload = TaskAttachmentPayload(
            task_id = taskId,
            file_url = fileUrl,
            file_name = fileName,
            file_type = fileType,
            is_synced = true
        )

        SupabaseApi.service.createTaskAttachment(payload)
            .enqueue(object : Callback<List<TaskAttachment>> {
                override fun onResponse(
                    call: Call<List<TaskAttachment>>,
                    response: Response<List<TaskAttachment>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body()?.firstOrNull())
                    } else {
                        onError("Erro ao guardar anexo na BD: ${response.code()}")
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

    private fun getFileName(
        context: Context,
        uri: Uri
    ): String {
        var fileName = "attachment"

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
}