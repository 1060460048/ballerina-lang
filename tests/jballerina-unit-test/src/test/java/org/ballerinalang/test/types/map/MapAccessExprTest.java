/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.test.types.map;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.internal.util.exceptions.BLangRuntimeException;
import org.ballerinalang.test.BAssertUtil;
import org.ballerinalang.test.BCompileUtil;
import org.ballerinalang.test.BRunUtil;
import org.ballerinalang.test.CompileResult;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Map access expression test.
 *
 * @since 0.8.0
 */
public class MapAccessExprTest {

    private CompileResult compileResult, resultNegative, resultSemanticsNegative;

    @BeforeClass
    public void setup() {
        compileResult = BCompileUtil.compile("test-src/types/map/map-access-expr.bal");
        resultNegative = BCompileUtil.compile("test-src/types/map/map-access-negative.bal");
        resultSemanticsNegative = BCompileUtil.compile("test-src/types/map/map-access-semantics-negative.bal");
    }

    @Test(description = "Test map access expression")
    public void testMapAccessExpr() {
        Object[] args = {(100), (5)};
        Object returns = BRunUtil.invoke(compileResult, "mapAccessTest", args);

        Assert.assertSame(returns.getClass(), Long.class);

        long actual = (long) returns;
        long expected = 105;
        Assert.assertEquals(actual, expected);
    }

    @Test(description = "Test map access through var keyword")
    public void testAccessThroughVar() {
        Object[] args = {};
        Object returns = BRunUtil.invoke(compileResult, "testAccessThroughVar", args);

        Assert.assertTrue(returns instanceof BString);

        String expectedStr = "x:a, y:b, z:c, ";
        String actualStr = returns.toString();
        Assert.assertEquals(actualStr, expectedStr);
    }

    @Test(description = "Test map return value")
    public void testArrayReturnValueTest() {
        Object[] args = {StringUtils.fromString("Chanaka"), StringUtils.fromString("Fernando")};
        Object returns = BRunUtil.invoke(compileResult, "mapReturnTest", args);

        Assert.assertTrue(returns instanceof  BMap);

        BMap mapValue = (BMap) returns;
        Assert.assertEquals(mapValue.size(), 3);

        Assert.assertEquals(mapValue.get(StringUtils.fromString("fname")).toString(), "Chanaka");
        Assert.assertEquals(mapValue.get(StringUtils.fromString("lname")).toString(), "Fernando");
        Assert.assertEquals(mapValue.get(StringUtils.fromString("ChanakaFernando")).toString(), "ChanakaFernando");

    }

    @Test(description = "Test nested map access")
    public void testNestedMapAccess() {
        CompileResult incorrectCompileResult = BCompileUtil.compile("test-src/types/map/nested-map-access.bal");
        Assert.assertEquals(incorrectCompileResult.getDiagnostics().length, 1);
        BAssertUtil.validateError(incorrectCompileResult, 0, "invalid operation: type 'any' does not support " +
                "member access", 4, 12);
    }

    @Test(description = "Test array access expression as the index of a map")
    public void testArrayAccessAsIndexOfMapt() {
        Object[] args = {};
        Object returns = BRunUtil.invoke(compileResult, "testArrayAccessAsIndexOfMapt", args);

        Assert.assertTrue(returns instanceof BString);

        Assert.assertEquals(returns.toString(), "Supun");
    }

    @Test(description = "Test map clear.")
    public void testMapRemoveAll() {
        Object[] args = {};
        Object returns = BRunUtil.invoke(compileResult, "testMapRemoveAll", args);

        Assert.assertSame(returns.getClass(), Long.class);

        Assert.assertEquals(returns, new Long(0));
    }

    @Test(description = "Test map has key positive.")
    public void testHasKeyPositive() {
        Object[] args = {};
        Object returns = BRunUtil.invoke(compileResult, "testHasKeyPositive", args);

        Assert.assertSame(returns.getClass(), Boolean.class);

        Assert.assertEquals(returns, Boolean.TRUE);
    }

    @Test(description = "Test map has key negative.")
    public void testHasKeyNegative() {
        Object[] args = {};
        Object returns = BRunUtil.invoke(compileResult, "testHasKeyNegative", args);

        Assert.assertSame(returns.getClass(), Boolean.class);

        Assert.assertEquals(returns, Boolean.FALSE);
    }

    @Test(description = "Test get map values.")
    public void testGetMapValues() {
        Object[] args = {};
        BArray returns = (BArray) BRunUtil.invoke(compileResult, "testGetMapValues", args);

        Assert.assertEquals(returns.size(), 2);
        Assert.assertTrue(returns.get(0) instanceof BString);
        Assert.assertEquals(returns.get(0).toString(), "Supun");
        Assert.assertTrue(returns.get(1) instanceof BString);
        Assert.assertEquals(returns.get(1).toString(), "Colombo");
    }

    @Test(description = "Map access negative scenarios", groups = {"disableOnOldParser"})
    public void testNegativeSemantics() {
        Assert.assertEquals(resultSemanticsNegative.getDiagnostics().length, 4);
        int index = 0;
        BAssertUtil.validateError(resultSemanticsNegative, index++,
                "incompatible types: expected 'string', found " + "'int'", 4, 20);
        BAssertUtil.validateError(resultSemanticsNegative, index++,
                "invalid operation: type 'map' does not support field access", 9, 13);
        BAssertUtil.validateError(resultSemanticsNegative, index++, "missing identifier", 9, 20);
        BAssertUtil.validateError(resultSemanticsNegative, index++, "missing identifier", 9, 21);
    }

    @Test(description = "Map access negative scenarios")
    public void negativeTest() {
        Assert.assertEquals(resultNegative.getDiagnostics().length, 3);
        int index = 0;

        // uninitialized map access
        BAssertUtil.validateError(resultNegative, index++, "variable 'ints' is not initialized", 9, 5);
        BAssertUtil.validateError(resultNegative, index++, "variable 'ints' is not initialized", 11, 40);
        BAssertUtil.validateError(resultNegative, index, "variable 'm4' is not initialized", 34, 12);
    }

    @Test(description = "Test map remove key positive.")
    public void testMapRemovePositive() {
        Object[] args = {};
        BArray returns = (BArray) BRunUtil.invoke(compileResult, "testMapRemovePositive", args);

        Assert.assertEquals(returns.size(), 3);
        Assert.assertSame(returns.get(0).getClass(), Boolean.class);
        Assert.assertSame(returns.get(1).getClass(), Boolean.class);
        Assert.assertSame(returns.get(2).getClass(), Boolean.class);

        Assert.assertEquals(returns.get(0), Boolean.TRUE);
        Assert.assertEquals(returns.get(1), Boolean.TRUE);
        Assert.assertEquals(returns.get(2), Boolean.FALSE);
    }

    @Test(expectedExceptions = {BLangRuntimeException.class},
            expectedExceptionsMessageRegExp = "error: \\{ballerina/lang.map\\}KeyNotFound \\{\"message\":\"cannot " +
                    "find key 'fname2'.*")
    public void testMapRemoveNegative() {
        BRunUtil.invoke(compileResult, "testMapRemoveNegative");
    }

    @Test(description = "Test removeIfHasKey if key exists.")
    public void testRemoveIfHasKeyPositive1() {
        Object returns = BRunUtil.invoke(compileResult, "testRemoveIfHasKeyPositive1");
        Assert.assertTrue((Boolean) returns, "Expected booleans to be identified as equal");
    }

    @Test(description = "Test removeIfHasKey if key does not exist.")
    public void testRemoveIfHasKeyNegative1() {
        Object returns = BRunUtil.invoke(compileResult, "testRemoveIfHasKeyNegative1");
        Assert.assertFalse((Boolean) returns, "Expected booleans to be identified as equal");
    }

    @Test(description = "Test removeIfHasKey if key exists.")
    public void testRemoveIfHasKeyPositive2() {
        Object returns = BRunUtil.invoke(compileResult, "testRemoveIfHasKeyPositive2");
        Assert.assertTrue((Boolean) returns, "Expected booleans to be identified as equal");
    }

    @Test(description = "Test removeIfHasKey if key does not exist.")
    public void testRemoveIfHasKeyNegative2() {
        Object returns = BRunUtil.invoke(compileResult, "testRemoveIfHasKeyNegative2");
        Assert.assertFalse((Boolean) returns, "Expected booleans to be identified as equal");
    }

    @Test(description = "Test to check toString for map of maps.")
    public void testMapToString() {
        Object returns = BRunUtil.invoke(compileResult, "testMapToString");
        BString value = (BString) returns;
        Assert.assertEquals(value.toString(), "typedesc map<map<json>>");
    }

    @AfterClass
    public void tearDown() {
        compileResult = null;
        resultNegative = null;
        resultSemanticsNegative = null;
    }
}
