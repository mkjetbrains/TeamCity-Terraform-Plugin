package jetbrains.buildServer.agent.terraformRunner.cmd

import jetbrains.buildServer.BuildProblemData
import jetbrains.buildServer.agent.BuildFinishedStatus
import jetbrains.buildServer.agent.BuildRunnerContext
import jetbrains.buildServer.agent.FlowGenerator
import jetbrains.buildServer.agent.runner.*
import jetbrains.buildServer.agent.terraformRunner.cmd.commands.TerraformCommandExecution

abstract class TerraformBuildService(
        protected val buildRunnerContext: BuildRunnerContext
) : MultiCommandBuildSession {
    protected val myFlowId: String = FlowGenerator.generateNewFlow()
    protected val myLogger = buildRunnerContext.build.buildLogger.getFlowLogger(myFlowId)
    private val myCommands: List<TerraformCommandExecution> = this.instantiateCommands()
    private val myCommandIterator: Iterator<TerraformCommandExecution> = myCommands.iterator()
    private val myCurrentCommand: TerraformCommandExecution? = null

    abstract fun instantiateCommands(): List<TerraformCommandExecution>

    override fun getNextCommand(): CommandExecution? {
        return when {
            myCurrentCommand != null && myCurrentCommand.hasProblem() -> {
                return null
            }
            myCommandIterator.hasNext() -> {
                myCommandIterator.next()
            }
            else -> null
        }
    }

    override fun sessionStarted() {
        myLogger.message("Working directory: ${buildRunnerContext.workingDirectory}")
    }

    override fun sessionFinished(): BuildFinishedStatus? {
        val problemCommands : List<TerraformCommandExecution> = myCommands.filter { it.hasProblem() }

        if (!myCommandIterator.hasNext() && problemCommands.isEmpty()) {
            return BuildFinishedStatus.FINISHED_SUCCESS
        }

        problemCommands.forEach {
            val buildProblem = it.problemText
            myLogger.logBuildProblem(
                    BuildProblemData.createBuildProblem("Terraform command execution failed", "TerraformExecutionProblem", buildProblem)
            )
        }

        myLogger.disposeFlow()

        return BuildFinishedStatus.FINISHED_WITH_PROBLEMS
    }
}

