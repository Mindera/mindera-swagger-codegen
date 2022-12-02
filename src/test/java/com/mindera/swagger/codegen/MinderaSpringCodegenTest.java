package com.mindera.swagger.codegen;

import io.swagger.codegen.ClientOptInput;
import io.swagger.codegen.ClientOpts;
import io.swagger.codegen.DefaultGenerator;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.nio.file.Paths;

public class MinderaSpringCodegenTest {

    @Test
    public void testPetstoreGeneratedCodeCompilation() throws Exception {
        final File folder = getTemporaryFolder();

        try {
            testGeneratedCodeCompilation("src/test/resources/petstore.json", folder);
            File order = new File(folder, "/src/main/java/io/swagger/model/Order.java");
            Assert.assertTrue(order.exists());
        } finally {
            FileUtils.deleteDirectory(folder);
        }
    }

    @Test
    public void testXmsGeneratedCodeCompilation() throws Exception {
        final File folder = getTemporaryFolder();

        testGeneratedCodeCompilation("src/test/resources/test.yml", folder);

        FileUtils.deleteDirectory(folder);
    }

    private File getTemporaryFolder() throws Exception {
        final File folder = new File(Paths.get(FileUtils.getTempDirectoryPath(), String.format("%s_%s", getClass().getSimpleName().toLowerCase(), RandomStringUtils.randomAlphanumeric(32))).toAbsolutePath().toString());

        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                throw new Exception("Failed to create temporary folder");
            }
        }
        return folder;
    }

    private void testGeneratedCodeCompilation(String apiLocation, final File folder) throws Exception {
        System.setProperty("debugSwagger", "true");

        File output = new File(folder.getAbsolutePath());
        Swagger swagger = (new SwaggerParser()).read(apiLocation);

        MinderaSpringCodegen codegenConfig = new MinderaSpringCodegen();

        codegenConfig.additionalProperties().put("classpathTargetSpec", "classpath:api.yml");
        codegenConfig.setOutputDir(output.getAbsolutePath());

        ClientOptInput clientOptInput = (new ClientOptInput()).opts(new ClientOpts()).swagger(swagger).config(codegenConfig);
        (new DefaultGenerator()).opts(clientOptInput).generate();

        File pomFile = new File(folder, "pom.xml");
        URL pomUrl = getClass().getClassLoader().getResources("mindera_spring_mvc_server_pom.xml").nextElement();
        FileUtils.copyURLToFile(pomUrl, pomFile);

        Process process = new ProcessBuilder("/usr/local/bin/mvn", "clean", "compile")
                .directory(output)
                .redirectErrorStream(true)
                .redirectOutput(Redirect.INHERIT)
                .start();
        Assert.assertEquals(process.waitFor(), 0);
    }
}