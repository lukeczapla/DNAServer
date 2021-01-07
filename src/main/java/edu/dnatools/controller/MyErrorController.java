package edu.dnatools.controller;

/**
 * Created by luke on 11/21/16.
 */
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@RestController
@RequestMapping("/error")
public class MyErrorController implements ErrorController {

    @Autowired
    private ErrorAttributes errorAttributes;

    @Autowired
    public MyErrorController(ErrorAttributes errorAttributes) {
        Assert.notNull(errorAttributes, "ErrorAttributes must not be null");
        this.errorAttributes = errorAttributes;
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }

    @RequestMapping
    public String error(WebRequest aRequest){
        Map<String, Object> body = getErrorAttributes(aRequest,getTraceParameter(aRequest));
        String trace = (String) body.get("trace");
        if(trace != null){
            String[] lines = trace.split("\n\t");
            body.put("trace", lines);
        }
        return "Error: " + body.get("error");
    }

    private boolean getTraceParameter(WebRequest request) {
        String parameter = request.getParameter("trace");
        if (parameter == null) {
            return false;
        }
        return !parameter.toLowerCase().equals("false");
    }

    private Map<String, Object> getErrorAttributes(WebRequest request, boolean includeStackTrace) {
        return errorAttributes.getErrorAttributes(request,includeStackTrace ? ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STACK_TRACE):ErrorAttributeOptions.defaults());
    }
}