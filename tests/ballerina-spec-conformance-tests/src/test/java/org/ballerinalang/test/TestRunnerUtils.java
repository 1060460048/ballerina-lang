/*
 * Copyright (c) 2021, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.test;

import io.ballerina.tools.diagnostics.Diagnostic;
import org.testng.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Test util class for all spec conformance test cases.
 *
 * @since 2.0.0
 */
public class TestRunnerUtils {

    private static final String START_TEST_CASE = "Test-Case";
    private static final String DESCRIPTION = "Description";
    private static final String AUTHOR = "Author";
    private static final String FAIL_ISSUE = "Fail-Issue";
    private static final String LABELS = "Labels";
    public static final String OUTPUT = "output";
    public static final String PANIC = "panic";
    public static final String ERROR = "error";
    public static final String SKIPPED_TEST = "skipped-test";
    private static final String EMPTY_STRING = "";
    private static final String COLON = ":";
    private static final String NEW_LINE_CHARACTER = "\n";
    private static final String BAL_EXTENSION = ".bal";
    public static final String RESOURCE_DIR = "src/test/resources/";
    public static final String TEMP_DIR = "test-src/TempFiles/";
    private static final String IMPORT_BALLERINAI = "import ballerinai/io;";
    private static int fileCount = 0;
    private static int absLineNum;


    public static void readTestFile(String fileName, String path, List<Object[]> testCases, Set<String> labels)
                                                                                                    throws IOException {
        File testFile = new File(path);
        FileReader fileReader = new FileReader(testFile);
        BufferedReader buffReader = new BufferedReader(fileReader);
        String line = buffReader.readLine();
        absLineNum = 1;
        String pattern = String.format("^(%s)\\s*:\\s*.*", START_TEST_CASE);
        while (line != null) {
            if (Pattern.matches(pattern, line)) {
                line = createTestFile(fileName, line, buffReader, testCases, labels);
            } else {
                line = buffReader.readLine();
                absLineNum++;
            }
        }
        buffReader.close();
    }

    private static String createTestFile(String fileName, String kindOfTestFile, BufferedReader buffReader,
                                         List<Object[]>  testCases, Set<String> labels) throws IOException {
        String tempFileName = fileName.substring(0, fileName.indexOf(".")) + fileCount++;
        String tempDir = TEMP_DIR + tempFileName + BAL_EXTENSION;
        File tempBalFile = new File(RESOURCE_DIR + tempDir);
        String kindOfTestCase = readHeadersOfTestCase(buffReader, kindOfTestFile, labels);
        if (kindOfTestCase.equals(SKIPPED_TEST)) {
            return EMPTY_STRING;
        }
        List<String> outputValues = new ArrayList<>();
        List<Integer> errLines = new ArrayList<>();
        Object[] testCase = new Object[6];
        testCase[0] = kindOfTestCase;
        FileWriter tempFileWriter = new FileWriter(tempBalFile);
        boolean isOutputTest = kindOfTestCase.equals(OUTPUT);
        if (isOutputTest) {
            tempFileWriter.write(IMPORT_BALLERINAI + NEW_LINE_CHARACTER);
        }
        String line;
        int relativeLineNum = 0;
        while ((line = buffReader.readLine()) != null) {
            if (!isOutputTest) {
                relativeLineNum = relativeLineNum + 1;
            }
            if (line.startsWith(START_TEST_CASE)) {
                break;
            }
            String pattern = String.format(".*//\\s*@\\s*(%s)\\s*.*", kindOfTestCase);
            if (Pattern.matches(pattern, line)) {
                String output = line.substring(line.indexOf(kindOfTestCase) + kindOfTestCase.length()).trim();
                if (!kindOfTestCase.equals(OUTPUT)) {
                    errLines.add(relativeLineNum);
                }
                outputValues.add(output);
            } else if (Pattern.matches(".*//.*", line)) {
                throw new IllegalStateException("Incorrect output(comment)");
            }
            tempFileWriter.write(line + NEW_LINE_CHARACTER);
        }
        tempFileWriter.close();
        testCase[1] = tempDir;
        testCase[2] = outputValues;
        testCase[3] = errLines;
        testCase[4] = fileName;
        testCase[5] = absLineNum;
        absLineNum = absLineNum + relativeLineNum;
        testCases.add(testCase);
        return line;
    }

    private static String getKindOfTestCase(String kindOfTestCase) {
        String pattern = String.format(".*:\\s*(%s|%s|%s)$", OUTPUT, ERROR, PANIC);
        if (Pattern.matches(pattern, kindOfTestCase.trim())) {
            return kindOfTestCase.substring(kindOfTestCase.indexOf(COLON) + 1).trim();
        }
        throw new AssertionError(String.format("Incorrect test type in line %d expected OUTPUT, ERROR, PANIC",
                absLineNum));
    }

    private static String readHeadersOfTestCase(BufferedReader buffReader, String kindOfTestFile,
                                                Set<String> selectedLabels) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line;

        String pattern = String.format("^(%s|%s|%s|%s)\\s*:\\s*.*", DESCRIPTION, AUTHOR, FAIL_ISSUE, LABELS);
        String kind = getKindOfTestCase(kindOfTestFile);
        String key = null;
        String value = null;
        while ((line = buffReader.readLine()) != null) {
            absLineNum++;
            String header = line.strip();
            if (header.equals(EMPTY_STRING)) {
                headers.put(key, value);
                break;
            } else if (Pattern.matches(pattern, header)) {
                if (key != null) {
                    headers.put(key, value);
                }
                int index = line.indexOf(COLON);
                key = header.substring(0, index).trim();
                value = header.substring(index + 1).trim();
                if (key.equals(LABELS)) {
                    String[] labels = value.split("\\s*,\\s*");
                    areLabelsDefined(labels);
                    kind = isTestCaseSkipped(selectedLabels, labels, kind);
                }
            } else {
                value = value + " " + header.trim();
            }
        }
        return kind;
    }

    private static void areLabelsDefined(String[] labels) {
        for (String label : labels) {
            if (!org.ballerinalang.test.Labels.LABELS.contains(label)) {
                Assert.fail(String.format("%s is not defined", label));
            }
        }
    }

    private static String isTestCaseSkipped(Set<String> selectedLabels, String[] labels, String kind) {
        if (selectedLabels.isEmpty()) {
            return kind;
        }
        for (String label : labels) {
            if (selectedLabels.contains(label)) {
                return SKIPPED_TEST;
            }
        }
        return kind;
    }

    public static void validateError(CompileResult result, List<Integer> errLines, String filename, int absLineNum) {
        for (int i = 0; i < result.getDiagnostics().length; i++) {
            Diagnostic diagnostic = result.getDiagnostics()[i];
            int expectedErrLine = errLines.get(i);
            Assert.assertEquals(diagnostic.location().lineRange().startLine().line() + 1 + absLineNum,
                    expectedErrLine + absLineNum, String.format("In %s, incorrect line number:", filename));
        }
    }

    public static void validateOutput(String fileName, List<String> outputValues, String[] results) {
        if (outputValues.size() != results.length) {
            Assert.fail(String.format("In %s file, Expected %s but found %s", fileName, outputValues.toString(),
                        Arrays.toString(results)));
        }
        for (int i = 0; i < outputValues.size(); i++) {
            if (!results[i].equals(outputValues.get(i))) {
                Assert.fail(String.format("In %s file, Expected %s but found %s", fileName, outputValues.toString(),
                            Arrays.toString(results)));
            }
        }
    }

    public static void validatePanic(int result, int expectedErrLine, String fileName) {
        Assert.assertEquals(result, expectedErrLine, String.format("In %s, incorrect line number:", fileName));
    }
}
