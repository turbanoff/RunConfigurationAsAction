package org.turbanov.actions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity
import java.util.*

/**
 * @author Andrey Turbanov
 * @since 08.04.2024
 */
class BootstrapActivity() : ProjectActivity {
    private val bootstraps = Collections.synchronizedMap(IdentityHashMap<Project, Bootstrap>())

    init {
        val connect = ApplicationManager.getApplication().getMessageBus().connect()
        connect.subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
            override fun projectClosing(project: Project) {
                val bootstrap = bootstraps.remove(project)
                bootstrap?.projectClosed()
            }
        })
    }

    override suspend fun execute(project: Project) {
        val bootstrap = Bootstrap(project)
        bootstraps.put(project, bootstrap)
        bootstrap.projectOpened()
    }
}
