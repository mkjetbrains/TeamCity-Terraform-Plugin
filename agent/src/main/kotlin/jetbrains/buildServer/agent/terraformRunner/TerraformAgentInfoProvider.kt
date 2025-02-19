package jetbrains.buildServer.agent.terraformRunner

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.agent.AgentLifeCycleAdapter
import jetbrains.buildServer.agent.AgentLifeCycleListener
import jetbrains.buildServer.agent.BuildAgent
import jetbrains.buildServer.agent.BuildAgentConfiguration
import jetbrains.buildServer.agent.terraformRunner.detect.TFEnvDetector
import jetbrains.buildServer.agent.terraformRunner.detect.TerraformDetector
import jetbrains.buildServer.agent.terraformRunner.detect.TFExecutableInstance
import jetbrains.buildServer.runner.terraform.getVersionedPathVarName
import jetbrains.buildServer.runner.terraform.TerraformRunnerConstants as CommonConst
import jetbrains.buildServer.util.EventDispatcher

class TerraformAgentInfoProvider(
        private val myConfig: BuildAgentConfiguration,
        events: EventDispatcher<AgentLifeCycleListener>,
        tfDetectors: List<TerraformDetector>,
        tfEnvDetectors: List<TFEnvDetector>
) {
    private val myTFHolder = TerraformInstanceHolder()
    private val myTFEnvHolder = TerraformInstanceHolder()
    private val LOG = Logger.getInstance(this.javaClass.name)

    init {
        events.addListener(
            object : AgentLifeCycleAdapter() {
                override fun afterAgentConfigurationLoaded(agent: BuildAgent) {
                    registerDetectedTerraformInstances(tfDetectors, agent.configuration)
                    registerDetectedTFEnvInstances(tfEnvDetectors, agent.configuration)
                }
            }
        )
    }

    private fun registerDetectedTerraformInstances(
        detectors: List<TerraformDetector>,
        configuration: BuildAgentConfiguration?
    ) {
        for (detector in detectors) {
            LOG.debug("Detecting Terraform with ${detector.javaClass.name}.")
            for (entry in detector.detectTerraformInstances(configuration!!).entries) {
                LOG.debug("Processing detected Terraform instance [${entry.key}][${entry.value.version}]")
                myTFHolder.addInstance(entry.key, entry.value)
            }
        }

        if (!myTFHolder.isEmpty()) {
            registerMainTFInstance(myTFHolder.getMainInstance())
            registerTFInstances(myTFHolder.getInstances())
        } else {
            LOG.info(
                "No Terraform instance detected. If it is not available on PATH, " +
                        "please provide a custom path with ${CommonConst.BUILD_PARAM_SEARCH_TF_PATH} agent property."
            )
        }
    }

    private fun registerDetectedTFEnvInstances(
            detectors: List<TFEnvDetector>,
            configuration: BuildAgentConfiguration?
    ) {
        for (detector in detectors) {
            LOG.debug("Detecting TFEnv with ${detector.javaClass.name}.")
            for (entry in detector.detectTFEnvInstances(configuration!!).entries) {
                LOG.debug("Processing detected TFEnv instance [${entry.key}][${entry.value.version}]")
                myTFEnvHolder.addInstance(entry.key, entry.value)
            }
        }

        if (!myTFEnvHolder.isEmpty()) {
            registerMainTFEnvInstance(myTFEnvHolder.getMainInstance())
        } else {
            LOG.info(
                    "No TFEnv instance detected. If it is not available on PATH, " +
                            "please provide a custom path with ${CommonConst.BUILD_PARAM_SEARCH_TFENV_PATH} agent property."
            )
        }
    }

    private fun registerTFInstances(instances: java.util.HashMap<String, TFExecutableInstance>) {
        for (instance in instances.values) {
            LOG.info("Registering detected Terraform instance at ${instance.executablePath}")

            val terraformVersionedPathName = getVersionedPathVarName(instance.version)
            myConfig.addConfigurationParameter(terraformVersionedPathName, instance.executablePath)
        }
    }

    private fun registerMainTFInstance(mainInstance: TFExecutableInstance) { //#FIXME no need to save .path if it is equal to agent work dir
        LOG.info("Registering detected Terraform instance at ${mainInstance.executablePath} as main instance")

        myConfig.addConfigurationParameter(CommonConst.AGENT_PARAM_TERRAFORM_VERSION, mainInstance.version)
        if (!mainInstance.isDefault) {
            myConfig.addConfigurationParameter(CommonConst.AGENT_PARAM_TERRAFORM_PATH, mainInstance.executablePath)
        }
    }

    private fun registerMainTFEnvInstance(mainInstance: TFExecutableInstance) {
        LOG.info("Registering detected TFEnv instance at ${mainInstance.executablePath} as main instance")

        myConfig.addConfigurationParameter(CommonConst.AGENT_PARAM_TFENV_VERSION, mainInstance.version)
        if (!mainInstance.isDefault) {
            myConfig.addConfigurationParameter(CommonConst.AGENT_PARAM_TFENV_PATH, mainInstance.executablePath)
        }
    }

    companion object {
        class TerraformInstanceHolder {
            private val myInstances = HashMap<String, TFExecutableInstance>()

            fun addInstance(path: String, TFExecutableInstance: TFExecutableInstance) {
                myInstances[path] = TFExecutableInstance
            }

            fun getInstances(): HashMap<String, TFExecutableInstance> {
                return myInstances
            }

            fun isEmpty(): Boolean {
                return myInstances.isEmpty()
            }

            fun getMainInstance(): TFExecutableInstance {
                return myInstances.values.maxOrNull()!!
            }
        }
    }
}