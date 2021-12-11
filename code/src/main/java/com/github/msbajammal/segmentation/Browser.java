package com.github.msbajammal.segmentation;

import java.util.Map;
import java.util.HashMap;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.Response;

public class Browser extends ChromeDriver {
    public Browser() {
        super(_initOptions());
//        manage().window().setSize(new Dimension(1600, 800));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                quit();
            }
        });
    }

    private static ChromeOptions _initOptions() {
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--disable-gpu");
//        options.addArguments("--headless");
//        options.addArguments("--disable-web-security");
        return options;
    }
//    File f = new File(ClassLoader.getSystemClassLoader().getResource("logback.xml").getFile());

    public Map<String, Object> send(String commandName, Map<String, Object> parameters) {
        // Sends a command to cdp
        Map<String, Object> command = new HashMap<>();
        command.put("cmd", commandName);
        command.put("params", parameters);
        try {
            Response response = execute("executeCdpCommand", command);
            return (Map<String, Object>) (response.getValue());
        } catch (Exception e) {
            String exceptionMsg = e.getMessage();
            if (exceptionMsg.contains("\"message\":\"Invalid parameters\"")) {
                exceptionMsg = "invalid parameters for the command '"+commandName+"'. check command's docs.";
            }

            if (exceptionMsg.contains("\"message\":\"'"+commandName+"' wasn't found\"")) {
                exceptionMsg = "unknown command '"+commandName+"'.";
            }

            throw new WebDriverException(exceptionMsg);
        }
    }

    public Map<String, Object> send(String command) {
        return send(command, new HashMap<>());
    }

    public Map<String, Object> evaluate(String script) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("returnByValue", Boolean.TRUE);
        parameters.put("expression", script);
        return send("Runtime.evaluate", parameters);
    }

    public void goTo(String url) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("url", url);
        send("Page.navigate", parameters);
    }
}
