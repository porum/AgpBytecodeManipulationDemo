import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.api.variant.AndroidComponentsExtension
import org.aspectj.bridge.MessageHandler
import org.aspectj.bridge.IMessage
import org.aspectj.tools.ajc.Main
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.configurationcache.extensions.capitalized
import java.io.File
import com.android.build.gradle.BaseExtension
import org.gradle.api.Action
import org.gradle.api.Task

/**
 * aspectj demo plugin
 */
class AspectJDemoPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.dependencies.apply {
      add("implementation", "org.aspectj:aspectjrt:1.9.7")
    }

    val android = target.extensions.getByType(BaseExtension::class.java)

    val androidComponents = target.extensions.getByType(AndroidComponentsExtension::class.java)
    androidComponents.onVariants { variant ->

      target.tasks.whenTaskAdded {
        if (it.name == "compile${variant.buildType?.capitalized()}JavaWithJavac") {
          // Execution optimizations have been disabled for task ':app:compileDebugJavaWithJavac' to ensure correctness due to the following reasons:
          //  - Additional action of task ':app:compileDebugJavaWithJavac' was implemented by the Java lambda 'AspectJDemoPlugin$apply$2$$Lambda$17317/0x0000000806e7e040'. Reason: Using Java lambdas is not supported as task inputs. Please refer to https://docs.gradle.org/7.5.1/userguide/validation_problems.html#implementation_unknown for more details about this problem.
          it.doLast(object : Action<Task> {
            override fun execute(task: Task) {
              val javaCompile = task as JavaCompile
              val javaArgs = arrayOf(
                "-showWeaveInfo",
                "-1.8",
                "-inpath", javaCompile.destinationDir.toString(),
                "-aspectpath", javaCompile.classpath.asPath,
                "-d", javaCompile.destinationDir.toString(),
                "-classpath", javaCompile.classpath.asPath,
                "-bootclasspath", android.bootClasspath.joinToString(File.pathSeparator)
              )
              val messageHandler = MessageHandler(true)
              Main().run(javaArgs, messageHandler)
              val logger = Logging.getLogger("AspectJDemoPlugin")
              for (message in messageHandler.getMessages(null, true)) {
                println(message.message)
                when (message.kind) {
                  IMessage.DEBUG -> logger.log(LogLevel.DEBUG, message.thrown?.toString())
                  IMessage.INFO -> logger.log(LogLevel.INFO, message.thrown?.toString())
                  IMessage.WARNING -> logger.log(LogLevel.WARN, message.thrown?.toString())
                  IMessage.ERROR,
                  IMessage.ABORT,
                  IMessage.FAIL -> logger.log(LogLevel.ERROR, message.thrown?.toString())
                }
              }
            }
          })
        }
      }

    }
  }
}