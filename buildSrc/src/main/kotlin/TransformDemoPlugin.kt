import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.api.transform.Format
import com.android.build.api.transform.Status
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * old transform api sample
 */
class TransformDemoPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    val android = target.extensions.getByType(BaseExtension::class.java)
    android.registerTransform(ActivityLifecycleTraceTransform())
  }
}

class ActivityLifecycleTraceTransform : Transform() {
  override fun getName(): String {
    return "ActivityLifecycleTrace"
  }

  override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
    return TransformManager.CONTENT_CLASS
  }

  override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
    return TransformManager.SCOPE_FULL_PROJECT
  }

  override fun isIncremental(): Boolean {
    return true
  }

  override fun transform(transformInvocation: TransformInvocation) {
    super.transform(transformInvocation)

    transformInvocation.inputs.forEach { transformInput ->
      transformInput.directoryInputs.forEach { directoryInput ->
        val inputDir = directoryInput.file
        val outputDir = transformInvocation.outputProvider.getContentLocation(
          directoryInput.name,
          directoryInput.contentTypes,
          directoryInput.scopes,
          Format.DIRECTORY
        )

        if (transformInvocation.isIncremental) {
          directoryInput.changedFiles
            .filter { it.key.startsWith(inputDir) }
            .forEach { (changedFile, status) ->
              val outputFile = FileUtils.getOutputFile(inputDir, changedFile, outputDir)
              if (status == Status.REMOVED) {
                FileUtils.deleteIfExists(outputFile)
              } else if (status == Status.ADDED || status == Status.CHANGED) {
                doTransform(changedFile, outputFile)
              }
            }
        } else {
          outputDir.deleteRecursively()
          inputDir.walk().toList().parallelStream().filter { it.isFile }.forEach { inputFile ->
            val outputFile = FileUtils.getOutputFile(inputDir, inputFile, outputDir)
            doTransform(inputFile, outputFile)
          }
        }
      }

      transformInput.jarInputs.forEach { jarInput ->
        val inputJar = jarInput.file
        val outputJar = transformInvocation.outputProvider.getContentLocation(
          jarInput.name,
          jarInput.contentTypes,
          jarInput.scopes,
          Format.JAR
        )
        inputJar.copyTo(outputJar, true)
      }

    }

  }

  private fun doTransform(inputFile: File, outputFile: File) {
    if (inputFile.invariantSeparatorsPath.endsWith("com/panda912/agpbytecodemanipulationdemo/MainActivity.class")) {
      val bytes = inputFile.readBytes()
      FileUtils.ensureParentDirsCreated(outputFile)

      val classReader = ClassReader(bytes)
      val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
      classReader.accept(object : ClassVisitor(Opcodes.ASM9, classWriter) {
        override fun visitMethod(
          access: Int,
          name: String?,
          descriptor: String?,
          signature: String?,
          exceptions: Array<out String>?
        ): MethodVisitor {
          val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
          if (name != "onCreate" || descriptor != "(Landroid/os/Bundle;)V") {
            return mv
          }
          return object : AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
            override fun onMethodEnter() {
              mv.visitLdcInsn("ASM")
              mv.visitLdcInsn("onCreate")
              mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "android/util/Log",
                "i",
                "(Ljava/lang/String;Ljava/lang/String;)I",
                false
              )
              mv.visitInsn(Opcodes.POP)
            }
          }
        }
      }, 0)
      outputFile.writeBytes(classWriter.toByteArray())
    } else {
      inputFile.copyTo(outputFile, true)
    }
  }

}


/**
 * Created by panda on 2021/8/17 14:56.
 */
object FileUtils {

  /**
   * Converts a /-based path into a path using the system dependent separator.
   *
   * @param path the system independent path to convert
   * @return the system dependent path
   */
  fun toSystemDependentPath(path: String): String =
    if (File.separatorChar != '/') path.replace('/', File.separatorChar) else path

  /**
   * Converts a system-dependent path into a /-based path.
   *
   * @param path the system dependent path
   * @return the system independent path
   */
  fun toSystemIndependentPath(path: String): String =
    if (File.separatorChar != '/') path.replace(File.separatorChar, '/') else path

  /**
   * Computes the relative of a file or directory with respect to a directory.
   * For example, if the file's absolute path is `/a/b/c` and the directory
   * is `/a`, this method returns `b/c`.
   *
   * @param file the path that may not correspond to any existing path in the filesystem
   * @param dir the directory to compute the path relative to
   * @return the relative path from `dir` to `file`; if `file` is a directory
   * the path comes appended with the file separator (see documentation on `relativize`
   * on java's `URI` class)
   */
  fun relativePossiblyNonExistingPath(file: File, dir: File): String =
    toSystemDependentPath(dir.toURI().relativize(file.toURI()).path)

  /**
   * eg. input directory's absolute path is `/a/b`, input file's absolute path is `/a/b/c/A.class`,
   * output directory's absolute path is @{code /d/e}, then the output file's absolute path is `/d/e/c/A.class`
   *
   * @param inputDir  input directory's absolute path.
   * @param inputFile input file's absolute path. (a fully-qualified class name)
   * @param outputDir output directory's absolute path.
   * @return output file's absolute path
   */
  fun getOutputFile(inputDir: File, inputFile: File, outputDir: File): File =
    File(outputDir, toSystemIndependentPath(relativePossiblyNonExistingPath(inputFile, inputDir)))

  /**
   * Deletes a file or an empty directory if it exists.
   *
   * @param file the file or directory to delete. The file/directory may not exist; if the
   * directory exists, it must be empty.
   */
  @Throws(IOException::class)
  fun deleteIfExists(file: File): Boolean = Files.deleteIfExists(file.toPath())

  /**
   * ensure the file's parent directories has been created.
   */
  fun ensureParentDirsCreated(file: File): Boolean =
    with(file.parentFile) {
      if (!this.exists())
        this.mkdirs()
      else
        true
    }

}