package com.ftpix.homedash.plugins.dockercompose.models;

import java.util.List;

/**
 * Created by gz on 4/22/17.
 */
public class CommandOutput {
    public final static int SUCCESS = 0;

    private int returnCode;
    private List<String> output, errorOutput;


    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public List<String> getOutput() {
        return output;
    }

    public void setOutput(List<String> output) {
        this.output = output;
    }

    public List<String> getErrorOutput() {
        return errorOutput;
    }

    public void setErrorOutput(List<String> errorOutput) {
        this.errorOutput = errorOutput;
    }
}
