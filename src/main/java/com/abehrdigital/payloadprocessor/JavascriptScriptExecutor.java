package com.abehrdigital.payloadprocessor;

import com.abehrdigital.payloadprocessor.utils.JavascriptRoutineMethodConverter;
import com.abehrdigital.payloadprocessor.utils.RandomStringGenerator;
import com.abehrdigital.payloadprocessor.utils.RoutineScriptWrapperFunctions;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.StringWriter;

public class JavascriptScriptExecutor {
    private static final String ENGINE_NAME = "Nashorn";
    private static final int JAVA_CLASS_NAME_IN_ENGINE_LENGTH = 6;

    private StringWriter engineScriptWriter;
    private String javaClassNameInJavaScriptEngine;
    private String scriptForExecution;
    private ScriptEngine scriptEngine;
    private RoutineScriptService scriptService;
    private int scriptHashCodeBeforeConversion;

    public JavascriptScriptExecutor(String scriptForExecution, RoutineScriptService scriptService) {
        this.scriptForExecution = scriptForExecution;
        this.scriptService = scriptService;
        init();
    }

    private void init() {
        engineScriptWriter = new StringWriter();
        scriptHashCodeBeforeConversion = scriptForExecution.hashCode();
        javaClassNameInJavaScriptEngine = RandomStringGenerator.generateWithDefaultChars(JAVA_CLASS_NAME_IN_ENGINE_LENGTH);
        scriptEngine = new ScriptEngineManager().getEngineByName(ENGINE_NAME);
        scriptEngine.put(javaClassNameInJavaScriptEngine, scriptService);
        scriptForExecution = RoutineScriptWrapperFunctions.addWrapperScripts(scriptForExecution);
        scriptForExecution = getRoutineBodyWithConvertedJavaMethods(javaClassNameInJavaScriptEngine);
        redirectEngineOutputToWriter();
    }

    private void redirectEngineOutputToWriter() {
        ScriptContext context = scriptEngine.getContext();
        context.setWriter(engineScriptWriter);
        context.setErrorWriter(engineScriptWriter);
    }

    private String getRoutineBodyWithConvertedJavaMethods(String prefix) {
        return JavascriptRoutineMethodConverter.convertScriptJavaMethodsWithClassPrefix(
                scriptForExecution,
                scriptService.getClass().getDeclaredMethods(),
                prefix
        );
    }

    public String execute() throws ScriptException {
        scriptEngine.eval(scriptForExecution);
        return engineScriptWriter.toString();
    }

    public int getScriptHashCode() {
        return scriptHashCodeBeforeConversion;
    }
}
