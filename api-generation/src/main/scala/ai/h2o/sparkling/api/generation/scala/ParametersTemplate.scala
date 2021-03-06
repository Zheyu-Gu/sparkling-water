/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.h2o.sparkling.api.generation.scala

import ai.h2o.sparkling.api.generation.common._

object ParametersTemplate extends ScalaEntityTemplate with ParameterResolver {

  def apply(parameterSubstitutionContext: ParameterSubstitutionContext): String = {
    val h2oParameterFullName = parameterSubstitutionContext.h2oParameterClass.getName.replace('$', '.')

    val parameters = resolveParameters(parameterSubstitutionContext)
    val imports = Seq(h2oParameterFullName) ++ parameters.filter(_.dataType.isEnum).map(_.dataType.getCanonicalName)
    val parents = Seq("H2OAlgoParamsBase") ++ parameterSubstitutionContext.explicitFields.map(_.implementation)

    val entitySubstitutionContext = EntitySubstitutionContext(
      parameterSubstitutionContext.namespace,
      parameterSubstitutionContext.entityName,
      parents,
      imports)

    generateEntity(entitySubstitutionContext, "trait") {
      s"""${generateParamTag(parameterSubstitutionContext)}
         |  //
         |  // Parameter definitions
         |  //
         |${generateParameterDefinitions(parameters)}
         |
         |  //
         |  // Default values
         |  //
         |  setDefault(
         |${generateDefaultValues(parameters, parameterSubstitutionContext.explicitDefaultValues)})
         |
         |  //
         |  // Getters
         |  //
         |${generateGetters(parameters)}
         |
         |  //
         |  // Setters
         |  //
         |${generateSetters(parameters)}
         |
         |  override private[sparkling] def getH2OAlgorithmParams(): Map[String, Any] = {
         |    super.getH2OAlgorithmParams() ++ get${parameterSubstitutionContext.entityName}()
         |  }
         |
         |  private[sparkling] def get${parameterSubstitutionContext.entityName}(): Map[String, Any] = {
         |      Map(
         |${generateH2OAssignments(parameters)})
         |  }
         |
         |  override private[sparkling] def getSWtoH2OParamNameMap(): Map[String, String] = {
         |    super.getSWtoH2OParamNameMap() ++
         |      Map(
         |${generateSWToH2OParamNameAssociations(parameters)})
         |  }
      """.stripMargin
    }
  }

  private def generateParamTag(parameterSubstitutionContext: ParameterSubstitutionContext): String = {
    if (parameterSubstitutionContext.generateParamTag) {
      s"  protected def paramTag = reflect.classTag[${parameterSubstitutionContext.h2oParameterClass.getSimpleName}]\n"
    } else {
      ""
    }
  }

  private def generateParameterDefinitions(parameters: Seq[Parameter]): String = {
    parameters
      .map { parameter =>
        val constructorMethod = resolveParameterConstructorMethod(parameter.dataType, parameter.defaultValue)
        s"""  private val ${parameter.swName} = ${constructorMethod}(
           |    name = "${parameter.swName}",
           |    doc = "${parameter.comment}")""".stripMargin
      }
      .mkString("\n\n")
  }

  private def generateDefaultValues(parameters: Seq[Parameter], explicitDefaultValues: Map[String, String]): String = {
    parameters
      .map { parameter =>
        val defaultValue = if (parameter.dataType.isEnum) {
          s"${parameter.dataType.getSimpleName}.${parameter.defaultValue}.name()"
        } else {
          parameter.defaultValue
        }
        val finalDefaultValue = explicitDefaultValues.getOrElse(parameter.h2oName, stringify(defaultValue))
        s"    ${parameter.swName} -> $finalDefaultValue"
      }
      .mkString(",\n")
  }

  private def stringify(value: Any): String = value match {
    case f: java.lang.Float => s"${f.toString.toLowerCase}f"
    case d: java.lang.Double => d.toString.toLowerCase
    case l: java.lang.Long => s"${l}L"
    case a: Array[_] => s"Array(${a.map(stringify).mkString(", ")})"
    case v if v == null => null
    case v => v.toString
  }

  private def generateGetters(parameters: Seq[Parameter]): String = {
    parameters
      .map { parameter =>
        val resolvedType = resolveParameterType(parameter.dataType)
        s"  def get${parameter.swName.capitalize}(): $resolvedType = $$(${parameter.swName})"
      }
      .mkString("\n\n")
  }

  private def generateSetters(parameters: Seq[Parameter]): String = {
    parameters
      .map { parameter =>
        if (parameter.dataType.isEnum) {
          s"""  def set${parameter.swName.capitalize}(value: String): this.type = {
             |    val validated = EnumParamValidator.getValidatedEnumValue[${parameter.dataType.getSimpleName}](value)
             |    set(${parameter.swName}, validated)
             |  }
           """.stripMargin
        } else if (parameter.dataType.isArray && parameter.dataType.getComponentType.isEnum) {
          val enumType = parameter.dataType.getComponentType.getCanonicalName
          s"""  def set${parameter.swName.capitalize}(value: Array[String]): this.type = {
             |    val validated = EnumParamValidator.getValidatedEnumValues[$enumType](value, nullEnabled = true)
             |    set(${parameter.swName}, validated)
             |  }
           """.stripMargin
        } else {
          s"""  def set${parameter.swName.capitalize}(value: ${resolveParameterType(parameter.dataType)}): this.type = {
             |    set(${parameter.swName}, value)
             |  }
           """.stripMargin
        }
      }
      .mkString("\n")
  }

  private def generateH2OAssignments(parameters: Seq[Parameter]): String = {
    parameters
      .map { parameter =>
        s"""        "${parameter.h2oName}" -> get${parameter.swName.capitalize}()"""
      }
      .mkString(",\n")
  }

  private def generateSWToH2OParamNameAssociations(parameters: Seq[Parameter]): String = {
    parameters
      .map { parameter =>
        s"""        "${parameter.swName}" -> "${parameter.h2oName}""""
      }
      .mkString(",\n")
  }

  private def resolveParameterType(dataType: Class[_]): String = {
    if (dataType.isEnum) {
      "String"
    } else if (dataType.isArray) {
      s"Array[${resolveParameterType(dataType.getComponentType)}]"
    } else {
      dataType.getSimpleName.capitalize
    }
  }

  private def resolveParameterConstructorMethodType(dataType: Class[_], defaultValue: Any): String = {
    if (dataType.isEnum) {
      "string"
    } else if (dataType.isArray) {
      s"${resolveParameterConstructorMethodType(dataType.getComponentType, defaultValue)}Array"
    } else {
      dataType.getSimpleName.toLowerCase
    }
  }

  private def resolveParameterConstructorMethod(dataType: Class[_], defaultValue: Any): String = {
    val rawPrefix = resolveParameterConstructorMethodType(dataType, defaultValue)
    val finalPrefix = if (defaultValue == null) s"nullable${rawPrefix.capitalize}" else rawPrefix
    finalPrefix + "Param"
  }
}
