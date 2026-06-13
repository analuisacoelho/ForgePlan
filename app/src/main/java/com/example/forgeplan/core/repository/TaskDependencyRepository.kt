package com.example.forgeplan.core.repository

import com.example.forgeplan.ForgePlanApplication
import com.example.forgeplan.core.database.DatabaseProvider
import com.example.forgeplan.core.database.entity.TaskDependencyEntity
import com.example.forgeplan.core.model.TaskDependency
import com.example.forgeplan.core.network.NetworkUtils
import com.example.forgeplan.core.network.SupabaseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaskDependencyRepository {

    private val context get() = ForgePlanApplication.instance
    private val db get() = DatabaseProvider.getDatabase(context)

    fun getDependencies(
        taskId: Long,
        onSuccess: (List<TaskDependency>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (NetworkUtils.isOnline(context)) {
            SupabaseApi.service.getDependenciesByTaskId("eq.$taskId")
                .enqueue(object : Callback<List<TaskDependency>> {
                    override fun onResponse(call: Call<List<TaskDependency>>, response: Response<List<TaskDependency>>) {
                        if (response.isSuccessful) {
                            val deps = response.body() ?: emptyList()
                            CoroutineScope(Dispatchers.IO).launch {
                                db.taskDependencyDao().deleteByTaskId(taskId)
                                db.taskDependencyDao().insertAll(deps.map {
                                    TaskDependencyEntity(task_id = taskId, depends_on_task_id = it.depends_on_task_id)
                                })
                            }
                            onSuccess(deps)
                        } else {
                            loadLocal(taskId, onSuccess)
                        }
                    }
                    override fun onFailure(call: Call<List<TaskDependency>>, t: Throwable) {
                        loadLocal(taskId, onSuccess)
                    }
                })
        } else {
            loadLocal(taskId, onSuccess)
        }
    }

    private fun loadLocal(taskId: Long, onSuccess: (List<TaskDependency>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val local = db.taskDependencyDao().getByTaskId(taskId).map {
                TaskDependency(task_id = it.task_id, depends_on_task_id = it.depends_on_task_id)
            }
            withContext(Dispatchers.Main) { onSuccess(local) }
        }
    }

    fun createDependency(
        dependency: TaskDependency,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        SupabaseApi.service.createDependency(dependency)
            .enqueue(object : Callback<List<TaskDependency>> {
                override fun onResponse(call: Call<List<TaskDependency>>, response: Response<List<TaskDependency>>) {
                    if (response.isSuccessful) onSuccess()
                    else onError("Erro ao criar dependência")
                }
                override fun onFailure(call: Call<List<TaskDependency>>, t: Throwable) {
                    onError(t.message ?: "Erro desconhecido")
                }
            })
    }
}