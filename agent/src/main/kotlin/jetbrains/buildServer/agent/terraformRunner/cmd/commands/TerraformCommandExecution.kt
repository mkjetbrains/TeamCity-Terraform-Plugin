package jetbrains.buildServer.agent.terraformRunner.cmd.commands

import com.google.gson.Gson
import jetbrains.buildServer.agent.BuildRunnerContext
import jetbrains.buildServer.agent.runner.CommandExecution
import jetbrains.buildServer.agent.runner.ProgramCommandLine
import jetbrains.buildServer.agent.runner.TerminationAction
import jetbrains.buildServer.agent.terraformRunner.TerraformCommandLineConstants as RunnerConst
import jetbrains.buildServer.agent.terraformRunner.cmd.CommandLineBuilder
import jetbrains.buildServer.runner.terraform.TerraformRunnerInstanceConfiguration
import jetbrains.buildServer.runner.terraform.TerraformRunnerConstants as CommonConst

import java.io.File
import java.io.FileWriter
import java.util.*

abstract class TerraformCommandExecution(
    val buildRunnerContext: BuildRunnerContext,
    flowId: String
) : CommandExecution {
    val buildProblemMaxLength = 25

    protected val myLogger = buildRunnerContext.build.buildLogger.getFlowLogger(flowId)
    protected var myHasProblem: Boolean = false
    protected var myCommandLineTruncated: String = ""
    var problemText: String = "Terraform command execution failed"

    private fun truncate(string: String): String {
        if (string.length > buildProblemMaxLength) {
            return string.substring(0, buildProblemMaxLength - 1) + "..."
        }
        return string
    }

    override fun processStarted(programCommandLine: String, workingDirectory: File) {
        myCommandLineTruncated = truncate(programCommandLine)
        myLogger.message("##teamcity[blockOpened name='$myCommandLineTruncated']") //#FIXME looks like shit, might be a better way (push them from buildservice? any class to compose the service messages?)
        myLogger.message("Starting: $programCommandLine")
        problemText = "Terraform command '$myCommandLineTruncated' failed"
    }

    override fun onStandardOutput(text: String) {
        text.lines().forEach {
            myLogger.message(it)
        }
    }

    override fun onErrorOutput(text: String) {
        text.lines().forEach {
            myLogger.error(it)
        }
    }

    override fun processFinished(exitCode: Int) {
        myLogger.message("##teamcity[blockClosed name='$myCommandLineTruncated']")
        myLogger.apply {
            if (exitCode != 0) {
                myHasProblem = true
                error("Command failed with code $exitCode")
            }
        }
    }

    override fun interruptRequested(): TerminationAction = TerminationAction.KILL_PROCESS_TREE

    override fun isCommandLineLoggingEnabled(): Boolean = false

    protected open fun getExecutablePath(): String {
        if (buildRunnerContext.isVirtualContext) {
            return RunnerConst.COMMAND_TERRAFORM
        }
        return File(
            buildRunnerContext.configParameters.getOrDefault(
                CommonConst.AGENT_PARAM_TERRAFORM_PATH,
                ""
            ),
            RunnerConst.COMMAND_TERRAFORM
        ).absolutePath
    }

    protected open fun prepareArguments(
        config: TerraformRunnerInstanceConfiguration,
        builder: CommandLineBuilder
    ): CommandLineBuilder {
        return builder
    }

    private fun prepareCommonArguments(
        config: TerraformRunnerInstanceConfiguration,
        builder: CommandLineBuilder
    ): CommandLineBuilder {
        val extraArgs = config.getExtraArgs()
        if (!extraArgs.isNullOrEmpty()) {
            builder.addArgument(value = extraArgs)
        }

        return builder
    }

    protected fun preparePrefixedSystemParametersAsArguments(
        builder: CommandLineBuilder
    ): CommandLineBuilder {
        builder.addArgument(
            RunnerConst.PARAM_VAR_FILE,
            saveArgumentsToFile()
        )

        return builder
    }

    protected fun checkTerraformPrefixedSystemParameters(): Boolean {
        buildRunnerContext.build.sharedBuildParameters.systemProperties.forEach { param ->
            if (param.key.startsWith(CommonConst.BUILD_PARAM_SYSTEM_TERRAFORM_PREFIX)) {
                return true
            }
        }
        return false
    }

    private fun saveArgumentsToFile(
    ): String {
        val gson = Gson()
        val varFile = File(
            buildRunnerContext.build.buildTempDirectory.absolutePath,
            "terraform_varfile_${UUID.randomUUID()}.json"
        ).normalize()
        val writer = FileWriter(varFile)
        val json = gson.toJson(
            buildRunnerContext.build.sharedBuildParameters.systemProperties.filterKeys {
                it.startsWith(CommonConst.BUILD_PARAM_SYSTEM_TERRAFORM_PREFIX)
            }
        )
        writer.run {
            write(json)
            close()
        }

        return varFile.absolutePath
    }

    override fun makeProgramCommandLine(): ProgramCommandLine {
        val builder = CommandLineBuilder()
        val config = TerraformRunnerInstanceConfiguration(buildRunnerContext.runnerParameters)

        builder.executablePath = getExecutablePath()
        builder.workingDir = buildRunnerContext.workingDirectory.path
        prepareArguments(config, builder)
        prepareCommonArguments(config, builder)

        return builder.build()
    }

    override fun beforeProcessStarted() {
    }

    fun hasProblem(): Boolean {
        return myHasProblem
    }
}