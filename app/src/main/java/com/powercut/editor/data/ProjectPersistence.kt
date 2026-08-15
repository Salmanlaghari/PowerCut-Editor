package com.powercut.editor.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

object ProjectPersistence {

    private const val PROJECTS_DIR = "projects"
    private const val FILE_EXTENSION = ".powercut"

    fun getProjectsDir(context: Context): File {
        val dir = File(context.filesDir, PROJECTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun listProjectFiles(context: Context): List<File> {
        val dir = getProjectsDir(context)
        return dir.listFiles { file ->
            file.isFile && file.extension.equals("powercut", ignoreCase = true)
        }?.toList() ?: emptyList()
    }

    fun generateProjectId(): String = UUID.randomUUID().toString()

    @Singleton
    class Repository @Inject constructor(
        @ApplicationContext private val context: Context
    ) {
        suspend fun saveProject(project: VideoProject): Result<String> =
            withContext(Dispatchers.IO) {
                try {
                    val projectId = generateProjectId()
                    val fileName = "${project.name.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")}_$projectId$FILE_EXTENSION"
                    val file = File(getProjectsDir(context), fileName)

                    val json = VideoProject.toJson(project)
                    FileOutputStream(file).use { fos ->
                        fos.write(json.toString(2).toByteArray(Charsets.UTF_8))
                        fos.flush()
                    }
                    Result.success(file.absolutePath)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        suspend fun saveProject(project: VideoProject, file: File): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val json = VideoProject.toJson(project)
                    FileOutputStream(file).use { fos ->
                        fos.write(json.toString(2).toByteArray(Charsets.UTF_8))
                        fos.flush()
                    }
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        suspend fun loadProject(file: File): Result<VideoProject> =
            withContext(Dispatchers.IO) {
                try {
                    val jsonString = FileInputStream(file).use { fis ->
                        fis.readBytes().toString(Charsets.UTF_8)
                    }
                    val json = JSONObject(jsonString)
                    val project = VideoProject.fromJson(json)
                    Result.success(project)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        suspend fun loadProject(projectId: String): Result<VideoProject> =
            withContext(Dispatchers.IO) {
                try {
                    val projectsDir = getProjectsDir(context)
                    val matchingFiles = projectsDir.listFiles { file ->
                        file.isFile &&
                            file.extension.equals("powercut", ignoreCase = true) &&
                            file.nameWithoutExtension.endsWith("_$projectId")
                    } ?: emptyArray()

                    if (matchingFiles.isEmpty()) {
                        return@withContext Result.failure(
                            FileNotFoundException("Project not found: $projectId")
                        )
                    }

                    loadProject(matchingFiles.first())
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        suspend fun deleteProject(file: File): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    if (file.exists()) {
                        file.delete()
                    }
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
    }
}
