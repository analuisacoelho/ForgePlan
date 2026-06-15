package com.example.forgeplan.core.repository

import com.example.forgeplan.ForgePlanApplication
import com.example.forgeplan.core.database.DatabaseProvider
import com.example.forgeplan.core.database.entity.TaskAssignmentEntity
import com.example.forgeplan.core.model.TaskAssignment
import com.example.forgeplan.core.network.NetworkUtils
import com.example.forgeplan.core.network.SupabaseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskAssignmentRepository {

    // padrão offline-first
    private val context get() = ForgePlanApplication.instance
    private val db get() = DatabaseProvider.getDatabase(context) //evita memory leaks

    fun getAssignmentsByTaskId(
        taskId: Long,
        onSuccess: (List<TaskAssignment>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getTaskAssignmentsByTaskId("eq.$taskId")
                .enqueue(object : Callback<List<TaskAssignment>> {
                    override fun onResponse(call: Call<List<TaskAssignment>>, response: Response<List<TaskAssignment>>) {
                        if (response.isSuccessful) {
                            val list = response.body() ?: emptyList()
                            CoroutineScope(Dispatchers.IO).launch {
                                db.taskAssignmentDao().insertAll(list.map {
                                    TaskAssignmentEntity(task_id = it.task_id, user_id = it.user_id, is_synced = true)
                                })
                            }
                            onSuccess(list)
                        } else loadLocalByTaskId(taskId, onSuccess)
                    }
                    override fun onFailure(call: Call<List<TaskAssignment>>, t: Throwable) {
                        loadLocalByTaskId(taskId, onSuccess)
                    }
                })
        } else {
            loadLocalByTaskId(taskId, onSuccess)
        }
    }

    private fun loadLocalByTaskId(taskId: Long, onSuccess: (List<TaskAssignment>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val local = db.taskAssignmentDao().getByTaskId(taskId).map {
                TaskAssignment(task_id = it.task_id, user_id = it.user_id, assigned_at = null)
            }
            withContext(Dispatchers.Main) { onSuccess(local) }
        }
    }

    /**
     * Devolve os task_id de todas as tarefas atribuídas ao utilizador.
     * task_id em task_assignments é gravado como o id "público" da task
     * (remote_id ?: id, igual ao que TaskRepository.toModel() devolve),
     * por isso não precisa de tradução extra ao comparar com Task.id na UI.
     */
    fun getTaskIdsByUserId(
        userId: Long,
        onSuccess: (List<Long>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getTaskAssignmentsByUserId("eq.$userId")
                .enqueue(object : Callback<List<TaskAssignment>> {
                    override fun onResponse(call: Call<List<TaskAssignment>>, response: Response<List<TaskAssignment>>) {
                        if (response.isSuccessful) {
                            val list = response.body() ?: emptyList()
                            CoroutineScope(Dispatchers.IO).launch {
                                db.taskAssignmentDao().insertAll(list.map {
                                    TaskAssignmentEntity(task_id = it.task_id, user_id = it.user_id, is_synced = true)
                                })
                            }
                            onSuccess(list.map { it.task_id })
                        } else loadLocalIdsByUserId(userId, onSuccess)
                    }
                    override fun onFailure(call: Call<List<TaskAssignment>>, t: Throwable) {
                        loadLocalIdsByUserId(userId, onSuccess)
                    }
                })
        } else {
            loadLocalIdsByUserId(userId, onSuccess)
        }
    }

    private fun loadLocalIdsByUserId(userId: Long, onSuccess: (List<Long>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val rows = db.taskAssignmentDao().getByUserId(userId)

            // row.task_id foi gravado como o id "público" da task (remote_id ?: id).
            // Resolve para o id atual que a UI usa via TaskDao (cobre o caso de
            // uma task ter sido criada offline e mais tarde sincronizada, mudando
            // de id local para remote_id).
            val resolvedIds = rows.mapNotNull { row ->
                val task = db.taskDao().getTaskByRemoteId(row.task_id)
                    ?: db.taskDao().getTaskById(row.task_id)
                task?.let { it.remote_id ?: it.id } ?: row.task_id
            }.distinct()

            withContext(Dispatchers.Main) { onSuccess(resolvedIds) }
        }
    }

    fun assignUserToTask(
        assignment: TaskAssignment,
        onSuccess: (TaskAssignment?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.assignUserToTask(assignment)
                .enqueue(object : Callback<List<TaskAssignment>> {
                    override fun onResponse(call: Call<List<TaskAssignment>>, response: Response<List<TaskAssignment>>) {
                        if (response.isSuccessful) {
                            onSuccess(response.body()?.firstOrNull())
                        } else if (response.code() == 409) {
                            onError("Este utilizador já está associado à tarefa.")
                        } else {
                            saveAssignmentLocally(assignment, onSuccess)
                        }
                    }
                    override fun onFailure(call: Call<List<TaskAssignment>>, t: Throwable) {
                        saveAssignmentLocally(assignment, onSuccess)
                    }
                })
        } else {
            saveAssignmentLocally(assignment, onSuccess)
            // outros erros HTTP → guarda offline para sincronizar depois
        }
    }

    private fun saveAssignmentLocally(assignment: TaskAssignment, onSuccess: (TaskAssignment?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            db.taskAssignmentDao().insertAll(listOf(
                TaskAssignmentEntity(task_id = assignment.task_id, user_id = assignment.user_id, is_synced = false)
            ))
            withContext(Dispatchers.Main) { onSuccess(assignment) }
        }
    }

    fun removeUserFromTask(
        taskId: Long,
        userId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.removeUserFromTask(
                taskIdFilter = "eq.$taskId",
                userIdFilter = "eq.$userId"
            ).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        CoroutineScope(Dispatchers.IO).launch {
                            db.taskAssignmentDao().deleteByTaskAndUser(taskId, userId)
                        }
                        onSuccess()
                    } else onError("Erro ao remover utilizador da tarefa: ${response.code()}")
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    CoroutineScope(Dispatchers.IO).launch {
                        db.taskAssignmentDao().deleteByTaskAndUser(taskId, userId)
                    }
                    onSuccess()
                }
            })
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                db.taskAssignmentDao().deleteByTaskAndUser(taskId, userId)
                withContext(Dispatchers.Main) { onSuccess() }
            }
        }
    }

    suspend fun getUserIdsForTask(taskId: Long): List<Long> =
        withContext(Dispatchers.IO) {
            // suspend fun = pode ser chamada diretamente de uma coroutine no ViewModel
            // sem precisar de callbacks onSuccess/onError
            // usada em contextos onde o resultado é necessário imediatamente
            try {
                val response = SupabaseApi.service
                    .getTaskAssignmentsByTaskId("eq.$taskId")
                    .execute() // .execute() = síncrono
                response.body()?.map { it.user_id } ?: emptyList()
            } catch (e: Exception) {
                emptyList() // qualquer erro, lista vazia sem crashar
            }
        }
}