package com.mindera.swagger.codegen;

import com.google.common.base.CaseFormat;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.SupportingFile;
import io.swagger.codegen.languages.AbstractJavaCodegen;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;

import java.io.File;
import java.util.Map;

public class MinderaJavaPlay2Codegen extends AbstractJavaCodegen {

    public MinderaJavaPlay2Codegen() {
        super();

        supportsInheritance = true;
        outputFolder = "generated-code" + File.separator + "java";

        embeddedTemplateDir = templateDir = "MinderaJavaPlay2";
        invokerPackage = "com.mindera.client";
        apiPackage = invokerPackage + ".api";
        modelPackage = invokerPackage + ".model";

        modelDocTemplateFiles.clear();
        apiDocTemplateFiles.clear();
        apiTestTemplateFiles.clear();

        additionalProperties.put("jackson", "true");
        additionalProperties.put("gson", "false");
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "mindera-play2";
    }

    @Override
    public String getHelp() {
        return "Generates a Java client library for Play Framework 2.";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        final String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");
        final String authFolder = (sourceFolder + '/' + invokerPackage + ".auth").replace(".", "/");

        supportingFiles.add(new SupportingFile("apiClient.mustache", invokerFolder, "ApiClient.java"));
        supportingFiles.add(new SupportingFile("apiException.mustache", invokerFolder, "ApiException.java"));
        supportingFiles.add(new SupportingFile("pair.mustache", invokerFolder, "Pair.java"));

        supportingFiles.add(new SupportingFile("auth/authentication.mustache", authFolder, "Authentication.java"));
        supportingFiles.add(new SupportingFile("auth/cookieAuth.mustache", authFolder, "CookieAuth.java"));
        supportingFiles.add(new SupportingFile("auth/apiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"));

        importMapping.remove("ApiModelProperty");
        importMapping.remove("ApiModel");
    }

    @Override
    public void preprocessSwagger(Swagger swagger) {
        super.preprocessSwagger(swagger);

        String dashFormattedTitle = swagger.getInfo().getTitle().replace(' ', '-').toLowerCase();
        additionalProperties.put("titleDashFormat", dashFormattedTitle);
        additionalProperties.put("titleCamelFormat", CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, dashFormattedTitle));
        additionalProperties.put("basePathWithoutHost", swagger.getBasePath().startsWith("/") ? swagger.getBasePath() : "/" + swagger.getBasePath());
    }

    public CodegenProperty fromProperty(String name, Property p) {
        CodegenProperty codegenProperty = super.fromProperty(name, p);
        codegenProperty.vendorExtensions.put("builder", "with" + getterAndSetterCapitalize(name));
        return codegenProperty;
    }

    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);
        model.imports.remove("ApiModelProperty");
        model.imports.remove("ApiModel");
        model.imports.add("JsonProperty");
    }

    @Override
    public CodegenModel fromModel(String name, Model model, Map<String, Model> allDefinitions) {
        CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);
        codegenModel.imports.remove("ApiModel");
        return codegenModel;
    }
}
