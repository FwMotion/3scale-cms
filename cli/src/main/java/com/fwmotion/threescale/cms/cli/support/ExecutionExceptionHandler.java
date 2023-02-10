/*
 *  Copyright 2021 The original authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.fwmotion.threescale.cms.cli.support;

import java.net.ConnectException;
import java.net.UnknownHostException;

import com.fwmotion.threescale.cms.cli.util.Colors;
import com.fwmotion.threescale.cms.cli.util.ConfigurationContext;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

public class ExecutionExceptionHandler implements IExecutionExceptionHandler {

    public record ExitCodeErrorMessagePair(int exitCode, String errorMessage) {
    }

    private final ConfigurationContext context;

    public ExecutionExceptionHandler(ConfigurationContext context) {
        this.context = context;
    }

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) throws Exception {
        var exitCodeErrorMessagePair = loadMessageAndExitCode(ex);
        if (exitCodeErrorMessagePair == null) {
            throw ex;
        }

        System.err.println(Colors.ANSI_RED + exitCodeErrorMessagePair.errorMessage() + Colors.ANSI_RESET);
        return exitCodeErrorMessagePair.exitCode();
    }

    private ExitCodeErrorMessagePair loadMessageAndExitCode(Exception ex) {
        Throwable rootCause = getRootCause(ex);

        if (rootCause instanceof ConnectException || rootCause instanceof UnknownHostException) {
            return new ExitCodeErrorMessagePair(
                    CommandLine.ExitCode.SOFTWARE,
                    "Couldn't connect to 3scale CMS API at %s.".formatted(context.getCurrentContext().getProviderDomain()));
        }

        return null;
    }

    private static Throwable getRootCause(Throwable ex) {
        Throwable parent;

        while ((parent = ex.getCause()) != null) {
            ex = parent;
        }

        return ex;
    }
}
