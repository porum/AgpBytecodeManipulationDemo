import com.android.build.api.instrumentation.*
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.LibraryVariant
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

/**
 * plugin
 */
class AsmTransformPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    val android = target.extensions.getByType(AndroidComponentsExtension::class.java)
    android.onVariants { variant ->
      when (variant) {
        is ApplicationVariant -> {
          variant.instrumentation.transformClassesWith(AsmClassVisitorFactoryImpl::class.java, InstrumentationScope.ALL) {}
          variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS)
        }
        is LibraryVariant -> {}
        else -> throw GradleException("Unsupported ${variant.name}")
      }
    }
  }
}

/**
 * impl
 */
abstract class AsmClassVisitorFactoryImpl : AsmClassVisitorFactory<InstrumentationParameters.None> {

  override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor): ClassVisitor {
    return ActivityLifecycleClassVisitor(nextClassVisitor)
  }

  override fun isInstrumentable(classData: ClassData): Boolean {
    return classData.className == "com.panda912.agpbytecodemanipulationdemo.MainActivity"
  }
}

/**
 * class visitor
 */
class ActivityLifecycleClassVisitor(classVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM9, classVisitor) {

  override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    if (name == "onCreate" && descriptor == "(Landroid/os/Bundle;)V") {
      return OnCreateMethodVisitor(mv, access, name, descriptor)
    }
    return mv
  }
}

/**
 * method visitor
 */
class OnCreateMethodVisitor(
  methodVisitor: MethodVisitor,
  access: Int,
  name: String?,
  descriptor: String?
) : AdviceAdapter(Opcodes.ASM9, methodVisitor, access, name, descriptor) {

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