import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.artifact.MultipleArtifact
import com.android.build.gradle.BaseExtension
import javassist.ClassPool
import javassist.CtClass
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.io.FileInputStream

/**
 * plugin
 */
class JavassistDemoPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    val android = target.extensions.getByType(BaseExtension::class.java)

    val androidComponents = target.extensions.getByType(AndroidComponentsExtension::class.java)
    androidComponents.onVariants { variant ->
      // register task
      val taskProvider = target.tasks.register(
        "${variant.name}InjectActivityLifecycleTraceTask",
        InjectActivityLifecycleTraceTask::class.java
      ) {
        // set android sdk path.
        it.androidSdkDir.set(AndroidJarProvider.getAndroidJar(android).absolutePath)
        // set compile classpath. The returned [FileCollection] should not be resolved until execution time.
        it.compileClasspath.set(variant.compileClasspath)
      }

      variant.artifacts.use<InjectActivityLifecycleTraceTask>(taskProvider)
        .wiredWith(
          InjectActivityLifecycleTraceTask::allClasses,
          InjectActivityLifecycleTraceTask::output
        )
        .toTransform(MultipleArtifact.ALL_CLASSES_DIRS)
    }
  }
}

/**
 * inject bytecode task
 */
abstract class InjectActivityLifecycleTraceTask : DefaultTask() {

  @get:Input
  abstract val androidSdkDir: Property<String>

  @get:Input
  abstract val compileClasspath: Property<FileCollection>

  @get:InputFiles
  abstract val allClasses: ListProperty<Directory>

  @get:OutputDirectory
  abstract val output: DirectoryProperty

  @TaskAction
  fun taskAction() {
    val classPool = ClassPool(ClassPool.getDefault())
    classPool.appendClassPath(androidSdkDir.get())
    compileClasspath.get().forEach {
      classPool.appendClassPath(it.absolutePath)
    }

    allClasses.get().forEach { directory ->
      println("Directory : ${directory.asFile.absolutePath}")
      directory.asFile.walk().filter { it.isFile && it.extension == "class" }.forEach { file ->
        FileInputStream(file).use {
          val ctClass = classPool.makeClass(it)
          if (ctClass.superclass.name == "androidx.appcompat.app.AppCompatActivity") {
            val params = arrayOf<CtClass>(classPool.get("android.os.Bundle"))
            val onCreateCtMethod = ctClass.getDeclaredMethod("onCreate", params)
            onCreateCtMethod.insertBefore("android.util.Log.i(\"Javassist\", \"onCreate\");")
          }
          ctClass.writeFile(output.get().asFile.absolutePath)
        }
      }
    }
  }

}

interface AndroidJarProvider {
  fun getAndroidJar(android: BaseExtension): File

  companion object DEFAULT : AndroidJarProvider {
    override fun getAndroidJar(android: BaseExtension): File {
      val sdkPath = arrayOf(
        android.sdkDirectory.absolutePath,
        "platforms",
        android.compileSdkVersion
      ).joinToString(File.separator)
      return File(sdkPath, "android.jar")
    }
  }
}