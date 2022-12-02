package com.mindera.swagger.codegen;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.SupportingFile;
import io.swagger.codegen.languages.SpringCodegen;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

public class MinderaSpringCodegen extends SpringCodegen {
    private static final String CONFIG_OPTION_CLASSPATH_TARGET_SPEC = "classpathTargetSpec";
    private static final String CONFIG_OPTION_SWAGGER_UI_VERSION = "swaggerUiVersion";
    private static final String CONFIG_OPTION_IS_CORS_ENABLED = "isCorsEnabled";
    private static final String SWAGGER_UI_VERSION = "2.2.2";

    public MinderaSpringCodegen() {
        super();

        embeddedTemplateDir = templateDir = "MinderaJavaSpring";

        supportedLibraries.clear();
        supportedLibraries.put(DEFAULT_LIBRARY, "Default Spring MVC server stub.");

        modelDocTemplateFiles.clear();
        apiDocTemplateFiles.clear();

        java8 = true;
        interfaceOnly = true;

        cliOptions.add(new CliOption(CONFIG_OPTION_CLASSPATH_TARGET_SPEC, "classpath for given swagger spec"));
        cliOptions.add(new CliOption(CONFIG_OPTION_SWAGGER_UI_VERSION, "version of the swagger ui being used"));
        cliOptions.add(new CliOption(CONFIG_OPTION_IS_CORS_ENABLED, "should add method to the CORS configuration"));
    }

    @Override
    public String getName() {
        return "mindera-spring";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (!additionalProperties.containsKey(CONFIG_OPTION_CLASSPATH_TARGET_SPEC)) {
            throw new RuntimeException("classpathTargetSpec is a required parameter");
        }

        if (!additionalProperties.containsKey(CONFIG_OPTION_SWAGGER_UI_VERSION)) {
            additionalProperties.put(CONFIG_OPTION_SWAGGER_UI_VERSION, SWAGGER_UI_VERSION);
        }

        if (!additionalProperties.containsKey(CONFIG_OPTION_IS_CORS_ENABLED)) {
            additionalProperties.put(CONFIG_OPTION_IS_CORS_ENABLED, false);
        }

        importMapping.put("InputStreamResource", "org.springframework.core.io.InputStreamResource");

        supportingFiles.clear();
        supportingFiles.add(new SupportingFile("swaggerConfig.mustache",
                (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator), "SwaggerConfig.java"));
        supportingFiles.add(new SupportingFile("swaggerController.mustache",
                (sourceFolder + File.separator + configPackage).replace(".", java.io.File.separator), "SwaggerController.java"));
        supportingFiles.add(new SupportingFile("apiProperties.mustache",
                ("src.main.resources").replace(".", java.io.File.separator), "api.properties"));
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co, Map<String, List<CodegenOperation>> operations) {
        super.addOperationToGroup(tag, resourcePath, operation, co, operations);

        if (co.returnType != null && co.returnType.equals("File")) {
            co.returnType = "InputStreamResource";
            co.imports.add("InputStreamResource");
        }

        ObjectNode returnTypeOverride = (ObjectNode) co.vendorExtensions.get("x-returnTypeOverride");
        if (returnTypeOverride != null) {
            String type = returnTypeOverride.get("type").asText();
            String importClass = returnTypeOverride.get("import").asText();
            co.vendorExtensions.replace("x-returnTypeOverride", ImmutableMap.of("type", type, "import", importClass));
            co.imports.add(type);
            importMapping.put(type, importClass);
        }
    }

    public CodegenProperty fromProperty(String name, Property p) {
        CodegenProperty codegenProperty = super.fromProperty(name, p);
        codegenProperty.vendorExtensions.put("builder", "with" + getterAndSetterCapitalize(name));
        return codegenProperty;
    }

    @Override
    public void preprocessSwagger(Swagger swagger) {
        super.preprocessSwagger(swagger);
        String dashFormattedTitle = swagger.getInfo().getTitle().replace(' ', '-').toLowerCase();
        additionalProperties.put("titleDashFormat", dashFormattedTitle);
        additionalProperties.put("titleCamelFormat", CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, dashFormattedTitle));
        additionalProperties.put("basePathWithoutHost", swagger.getBasePath());
        try {
            additionalProperties.put("encodedApiDocsPath", URLEncoder.encode(swagger.getBasePath() + "/api-docs", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CodegenModel fromModel(String name, Model model, Map<String, Model> allDefinitions) {
        CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);
        codegenModel.imports.remove("ApiModel");
        codegenModel.imports.remove("ApiModelProperty");
        return codegenModel;
    }
}