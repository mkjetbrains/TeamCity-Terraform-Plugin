package jetbrains.buildServer.runner.terraform

object TerraformRunnerConstants {
    // plugin-level data
    const val RUNNER_DESCRIPTION = "Runner for Terraform CLI commands execution"
    const val RUNNER_DISPLAY_NAME = "Terraform"
    const val RUNNER_TYPE = "terraform-runner"

    // runner parameters bean data
    const val RUNNER_SETTING_VERSION_KEY = "version"
    const val RUNNER_SETTING_VERSION_TFENV_VERSION = "versionToFetch"
    const val RUNNER_SETTING_COMMAND_KEY = "command"
    const val RUNNER_SETTING_CUSTOM_COMMAND_KEY = "customCommand"
    const val RUNNER_SETTING_PLAN_CUSTOM_OUT_KEY = "outputPath"
    const val RUNNER_SETTING_INIT_STAGE_DO_INIT_KEY = "doInit"
    const val RUNNER_SETTING_INIT_STAGE_USE_WORKSPACE_KEY = "useWorkspace"
    const val RUNNER_SETTING_INIT_STAGE_CREATE_WORKSPACE_IF_NOT_FOUND_KEY = "createWorkspace"
    const val RUNNER_SETTING_APPLY_CUSTOM_BACKUP_KEY = "stateBackupPath"
    const val RUNNER_SETTING_ADDITIONAL_ARGS = "additionalArguments"

    // prefix for Terraform system parameters
    const val BUILD_PARAM_SYSTEM_TERRAFORM_PREFIX = "system.terraform."

    // detection variables
    const val BUILD_PARAM_SEARCH_TF_PATH = "teamcity.terraform.detector.search.paths"
    const val BUILD_PARAM_SEARCH_TFENV_PATH = "teamcity.tfenv.detector.search.paths"

    const val AGENT_PARAM_TERRAFORM_PREFIX = "terraform"
    const val AGENT_PARAM_TFENV_PREFIX = "tfenv"
    const val AGENT_PARAM_PATH_POSTFIX = "path"
    const val AGENT_PARAM_PATH_VERSION = "version"

    const val AGENT_PARAM_TERRAFORM_PATH = AGENT_PARAM_TERRAFORM_PREFIX +
            ".${AGENT_PARAM_PATH_POSTFIX}"
    const val AGENT_PARAM_TERRAFORM_VERSION = AGENT_PARAM_TERRAFORM_PREFIX +
            ".${AGENT_PARAM_PATH_VERSION}"

    const val AGENT_PARAM_TFENV_PATH = AGENT_PARAM_TFENV_PREFIX +
            ".${AGENT_PARAM_PATH_POSTFIX}"
    const val AGENT_PARAM_TFENV_VERSION = AGENT_PARAM_TFENV_PREFIX +
            ".${AGENT_PARAM_PATH_VERSION}"

    // build parameters
    const val BUILD_PARAM_OUT_ARTIFACT_PATH = "$AGENT_PARAM_TERRAFORM_PREFIX.plan.output"

}

fun getVersionedPathVarName(version: String): String {
    return "${TerraformRunnerConstants.AGENT_PARAM_TERRAFORM_PREFIX}.${version}.${TerraformRunnerConstants.AGENT_PARAM_PATH_POSTFIX}"
}