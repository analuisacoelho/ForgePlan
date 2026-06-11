package com.example.forgeplan.core.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.forgeplan.core.model.TaskAttachment
import com.example.forgeplan.core.model.TaskAttachmentPayload
import com.example.forgeplan.core.network.SupabaseApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val fileName = getFileName(context, uri)
        val fileType = getFileType(context, uri)

        val safeFileName = fileName
            .replace(" ", "_")
            .replace("/", "_")
            .replace("\\", "_")

        val filePath = "task_$taskId/${System.currentTimeMillis()}_$safeFileName"

        val inputStream = context.contentResolver.openInputStream(uri)

        if (inputStream == null) {
            onError("Não foi possível ler o ficheiro.")
            return
        }

        val fileBytes = inputStream.use { it.readBytes() }

        val requestBody = RequestBody.create(
            fileType.toMediaTypeOrNull(),
            fileBytes
        )

        SupabaseApi.storageService.uploadFile(
            bucket = bucketName,
            filePath = filePath,
            contentType = fileType,
            file = requestBody
        ).enqueue(object : Callback<okhttp3.ResponseBody> {

            override fun onResponse(
                call: Call<okhttp3.ResponseBody>,
                response: Response<okhttp3.ResponseBody>
            ) {
                if (!response.isSuccessful) {
                    onError("Erro ao enviar ficheiro para o Storage: ${response.code()}")
                    return
                }

                val publicUrl = SupabaseApi.publicStorageUrl(
                    bucket = bucketName,
                    filePath = filePath
                )

                val payload = TaskAttachmentPayload(
                    task_id = taskId,
                    file_name = fileName,
                    file_url = publicUrl,
                    file_type = fileType
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

            override fun onFailure(
                call: Call<okhttp3.ResponseBody>,
                t: Throwable
            ) {
                onError(t.message ?: "Erro desconhecido ao enviar ficheiro.")
            }
        })
    }

    fun getAttachmentsByTaskId(
        taskId: Long,
        onSuccess: (List<TaskAttachment>) -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.getTaskAttachmentsByTaskId("eq.$taskId")
            .enqueue(object : Callback<List<TaskAttachment>> {

                override fun onResponse(
                    call: Call<List<TaskAttachment>>,
                    response: Response<List<TaskAttachment>>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body() ?: emptyList())
                    } else {
                        onError("Erro ao carregar anexos: ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<TaskAttachment>>,
                    t: Throwable
                ) {
                    onError(t.message ?: "Erro desconhecido")
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