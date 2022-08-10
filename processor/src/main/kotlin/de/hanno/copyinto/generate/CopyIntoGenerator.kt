package de.hanno.copyinto.generate

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import de.hanno.copyinto.api.CopyInto
import java.io.IOException

class CopyIntoGeneratorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val codeGenerator = environment.codeGenerator
        val logger = environment.logger

        return CopyIntoGenerator(logger, codeGenerator)
    }
}

class CopyIntoGenerator(val logger: KSPLogger, val codeGenerator: CodeGenerator) : SymbolProcessor {

    private fun packageNameOrNull(it: KSClassDeclaration): String? {
        val packageNameOrEmpty = it.packageName.asString()
        return packageNameOrEmpty.ifEmpty { null }
    }

    val propertyDeclarationsPerClass = mutableMapOf<KSClassDeclaration, List<KSPropertyDeclaration>>()
    val visitor = FindPropertiesVisitor()
    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }

        val matchingClassDeclarations = resolver.getSymbolsWithAnnotation(CopyInto::class.qualifiedName.toString())
            .filterIsInstance<KSClassDeclaration>().toList()

        matchingClassDeclarations.map {it.accept(visitor, Unit) }

        propertyDeclarationsPerClass.forEach { (classDeclaration, propertyDeclarations) ->

            val fqClassName = classDeclaration.qualifiedName!!.asString()
            val simpleClassName = classDeclaration.simpleName.asString()
            val packageNameOrNull = packageNameOrNull(classDeclaration)
            val fileName = simpleClassName + "CopyInto"


            // TODO: Check why the hell files are tried to be written multiple times instead of this nasty hack
            if (codeGenerator.generatedFile.toList().none { it.nameWithoutExtension == fileName }) {
                codeGenerator.createNewFile(Dependencies.ALL_FILES, packageNameOrNull ?: "", fileName, "kt")
                    .use { stream ->
                        val propertiesAssignments = propertyDeclarations.joinToString("\n") {
                            "\ttarget." + it.simpleName.asString() + " = this." + it.simpleName.asString()
                        }

                        try {
                            stream.write(
                                """${if(packageNameOrNull != null) "package $packageNameOrNull" else ""}
                                    |
                                    |import $fqClassName
                                    |
                                    |fun $simpleClassName.copyInto(target: $simpleClassName): Unit {
                                    |$propertiesAssignments
                                    |}
                                """.trimMargin().toByteArray(Charsets.UTF_8)
                            )
                        } catch (e: IOException) {
                            logger.error("Cannot write to file $fileName")
                        }
                    }
            }
        }

        invoked = true
        return emptyList()
    }

    inner class FindPropertiesVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            propertyDeclarationsPerClass[classDeclaration] = classDeclaration.getDeclaredProperties().filter { it.isMutable }.toList()
        }

        override fun visitFile(file: KSFile, data: Unit) {
            file.declarations.toList().map { it.accept(this, Unit) }
        }
    }
}