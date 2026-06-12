package com.example.forgeplan.core.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.forgeplan.core.model.TaskAttachment
import com.example.forgeplan.core.model.TaskAttachmentPayload
import com.example.forgeplan.core.network.SupabaseApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskAttachmentRepository {

    /**
     * Faz upload do ficheiro para o Supabase Storage (bucket task-attachments)
     * e guarda a URL pública na tabela task_attachments.
     */
    fun uploadAttachment(
        context: Context,
        taskId: Long,
        uri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val fileName = getFileName(context, uri)
        val mimeType = getFileType(context, uri)
        val fileBytes = readBytes(context, uri)

        if (fileBytes == null) {
            onError("Não foi possível ler o ficheiro.")
            return
        }

        // Caminho único no bucket: task-attachments/taskId/timestamp_filename
        val filePath = "$taskId/${System.currentTimeMillis()}_$fileName"
        val requestBody = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())

        SupabaseApi.storageService.uploadFile(
            bucket = "task-attachments",
            filePath = filePath,
            contentType = mimeType,
            file = requestBody
        ).enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(
                call: Call<okhttp3.ResponseBody>,
                response: Response<okhttp3.ResponseBody>
            ) {
                if (response.isSuccessful || response.code() == 200) {
                    // Constrói a URL pública e guarda na BD
                    val publicUrl = SupabaseApi.publicStorageUrl("task-attachments", filePath)
                    val payload = TaskAttachmentPayload(
                        task_id = taskId,
                        file_name = fileName,
                        file_url = publicUrl,
                        file_type = mimeType
                    )
                    SupabaseApi.service.createTaskAttachment(payload)
                        .enqueue(object : Callback<List<TaskAttachment>> {
                            override fun onResponse(call: Call<List<TaskAttachment>>, response: Response<List<TaskAttachment>>) {
                                if (response.isSuccessful) onSuccess()
                                else onError("Erro ao guardar anexo: ${response.code()}")
                            }
                            override fun onFailure(call: Call<List<TaskAttachment>>, t: Throwable) {
                                onError(t.message ?: "Erro desconhecido ao guardar anexo.")
                            }
                        })
                } else {
                    onError("Erro ao fazer upload: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                onError(t.message ?: "Erro desconhecido no upload.")
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
                    if (response.isSuccessful) onSuccess(response.body() ?: emptyList())
                    else onError("Erro ao carregar anexos: ${response.code()}")
                }
                override fun onFailure(call: Call<List<TaskAttachment>>, t: Throwable) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }

    private fun readBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var fileName = "attachment_${System.currentTimeMillis()}"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex >= 0) fileName = it.getString(nameIndex)
        }
        return fileName
    }

    private fun getFileType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "application/octet-stream"
    }
}