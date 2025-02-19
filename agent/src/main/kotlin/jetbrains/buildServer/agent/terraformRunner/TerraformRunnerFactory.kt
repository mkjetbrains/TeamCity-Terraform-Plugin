package jetbrains.buildServer.agent.terraformRunner

import jetbrains.buildServer.agent.AgentBuildRunnerInfo
import jetbrains.buildServer.agent.BuildAgentConfiguration
import jetbrains.buildServer.agent.BuildRunnerContext
import jetbrains.buildServer.agent.runner.MultiCommandBuildSessionFactory
import jetbrains.buildServer.agent.terraformRunner.cmd.TerraformBuildService
import jetbrains.buildServer.agent.terraformRunner.cmd.commands.*
import jetbrains.buildServer.agent.terraformRunner.cmd.commands.tfenv.TFEnvInstallCommandExecution
import jetbrains.buildServer.agent.terraformRunner.cmd.commands.tfenv.TFEnvUseCommandExecution
import jetbrains.buildServer.agent.terraformRunner.cmd.commands.workspace.WorkspaceNewCommandExecution
import jetbrains.buildServer.agent.terraformRunner.cmd.commands.workspace.WorkspaceSelectCommandExecution
import jetbrains.buildServer.runner.terraform.TerraformCommandType
import jetbrains.buildServer.runner.terraform.TerraformRunnerConstants
import jetbrains.buildServer.runner.terraform.TerraformRunnerInstanceConfiguration
import jetbrains.buildServer.runner.terraform.TerraformVersionMode

class TerraformRunnerFactory : MultiCommandBuildSessionFactory {
    override fun createSession(runnerContext: BuildRunnerContext): TerraformBuildService {
        val config = TerraformRunnerInstanceConfiguration(runnerContext.runnerParameters)
        return when {
            config.getCommand() == TerraformCommandType.APPLY -> {
                object : TerraformBuildService(runnerContext) {
                    override fun instantiateCommands(): List<TerraformCommandExecution> {
                        return instantiateApplyCommands(runnerContext, myFlowId)
                    }
                }
            }
            config.getCommand() == TerraformCommandType.PLAN -> {
                object : TerraformBuildService(runnerContext) {
                    override fun instantiateCommands(): List<TerraformCommandExecution> {
                        return instantiatePlanCommands(runnerContext, myFlowId)
                    }
                }
            }
            config.getCommand() == TerraformCommandType.CUSTOM -> {
                object : TerraformBuildService(runnerContext) {
                    override fun instantiateCommands(): List<TerraformCommandExecution> {
                        return instantiateCustomCommands(runnerContext, myFlowId)
                    }
                }
            }
            else -> throw IllegalStateException("No matching build service found for the specified command")
        }
    }

    private fun instantiateTFEnvCommands(
        buildRunnerContext: BuildRunnerContext,
        flowId: String
    ): ArrayList<TerraformCommandExecution> {
        val config = TerraformRunnerInstanceConfiguration(buildRunnerContext.runnerParameters)
        if (config.getVersionMode() == TerraformVersionMode.TFENV) {
            return arrayListOf(
                TFEnvInstallCommandExecution(buildRunnerContext, flowId),
                TFEnvUseCommandExecution(buildRunnerContext, flowId)
            )
        }
        return ArrayList()
    }

    private fun instantiateInitStageCommands(
        buildRunnerContext: BuildRunnerContext,
        flowId: String
    ): ArrayList<TerraformCommandExecution> {
        val commands: ArrayList<TerraformCommandExecution> = ArrayList()
        val config = TerraformRunnerInstanceConfiguration(buildRunnerContext.runnerParameters)
        val workspaceName = config.getUseWorkspace()!!

        if (config.getVersionMode() == TerraformVersionMode.TFENV) {
            commands.addAll(
                instantiateTFEnvCommands(buildRunnerContext, flowId)
            )
        }

        if (config.getDoCreateWorkspaceIfNotFound()) {
            commands.addAll(
                arrayListOf(
                    WorkspaceSelectCommandExecution(buildRunnerContext, flowId, workspaceName),
                    WorkspaceNewCommandExecution(buildRunnerContext, flowId, workspaceName)
                )
            )
        } else {
            commands.add(
                WorkspaceSelectCommandExecution(buildRunnerContext, flowId, workspaceName)
            )
        }

        if (config.getDoInit()) {
            commands.add(
                CustomCommandExecution(buildRunnerContext, flowId, TerraformCommandType.INIT.name)
            )
        }

        return commands
    }


    fun instantiateCustomCommands(
        buildRunnerContext: BuildRunnerContext,
        flowId: String
    ): List<TerraformCommandExecution> {
        val config = TerraformRunnerInstanceConfiguration(buildRunnerContext.runnerParameters)
        val commands: ArrayList<TerraformCommandExecution> = ArrayList()
        commands.addAll(instantiateInitStageCommands(buildRunnerContext, flowId))
        commands.add(
            CustomCommandExecution(buildRunnerContext, flowId, config.getCustomCommand()!!)
        )
        return commands
    }

    fun instantiateApplyCommands(
        buildRunnerContext: BuildRunnerContext,
        flowId: String
    ): List<TerraformCommandExecution> {
        val commands: ArrayList<TerraformCommandExecution> = ArrayList()
        commands.addAll(instantiateInitStageCommands(buildRunnerContext, flowId))
        commands.add(
            ApplyCommandExecution(buildRunnerContext, flowId)
        )
        return commands
    }

    fun instantiatePlanCommands(
        buildRunnerContext: BuildRunnerContext,
        flowId: String
    ): List<TerraformCommandExecution> {
        val commands: ArrayList<TerraformCommandExecution> = ArrayList()
        commands.addAll(instantiateInitStageCommands(buildRunnerContext, flowId))
        commands.add(
            PlanCommandExecution(buildRunnerContext, flowId)
        )
        return commands
    }

    override fun getBuildRunnerInfo(): AgentBuildRunnerInfo {
        return TerraformRunner()
    }

    companion object {
        class TerraformRunner : AgentBuildRunnerInfo {
            override fun getType(): String {
                return TerraformRunnerConstants.RUNNER_TYPE
            }

            override fun canRun(buildAgentConfiguration: BuildAgentConfiguration): Boolean {
                return true
            }
        }
    }
}