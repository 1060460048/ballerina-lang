/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.test.expressions.binaryoperations;

import org.ballerinalang.test.BAssertUtil;
import org.ballerinalang.test.BCompileUtil;
import org.ballerinalang.test.BRunUtil;
import org.ballerinalang.test.CompileResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test functionality of bitwise left shift, bitwise right shift and bitwise unsigned right shift operations.
 *
 * @since 2.0.0
 */
public class BitwiseShiftOperationTest {
    CompileResult result, negativeResult;

    @BeforeClass
    public void setup() {
        result = BCompileUtil.compile("test-src/expressions/binaryoperations/bitwise_shift_operation.bal");
        negativeResult = BCompileUtil.compile("test-src/expressions/binaryoperations/" +
                "bitwise_shift_operation_negative.bal");
    }

    @Test(description = "Test bitwise left shift operation")
    public void testBitwiseLeftShiftOp() {
        BRunUtil.invoke(result, "testBitwiseLeftShiftOp");
    }

    @Test(description = "Test bitwise right shift operation")
    public void testBitwiseRightShiftOp() {
        BRunUtil.invoke(result, "testBitwiseRightShiftOp");
    }

    @Test(description = "Test bitwise unsigned right shift operation")
    public void testBitwiseUnsignedRightShiftOp() {
        BRunUtil.invoke(result, "testBitwiseUnsignedRightShiftOp");
    }

    @Test(description = "Test bitwise operations for nullable values")
    public void testBitWiseOperationsForNullable() {
        BRunUtil.invoke(result, "testBitwiseUnsignedRightShiftOp");
    }

    @Test(dataProvider = "dataToTestShortCircuitingInBitwiseShiftOp")
    public void testShortCircuitingInBitwiseShiftOp(String functionName) {
        BRunUtil.invoke(result, functionName);
    }

    @DataProvider
    public Object[] dataToTestShortCircuitingInBitwiseShiftOp() {
        return new Object[]{
                "testNoShortCircuitingInBitwiseLeftShiftWithNullable",
                "testNoShortCircuitingInBitwiseLeftShiftWithNonNullable",
                "testNoShortCircuitingInBitwiseSignedRightShiftWithNullable",
                "testNoShortCircuitingInBitwiseSignedRightShiftWithNonNullable",
                "testNoShortCircuitingInBitwiseUnsignedRightShiftWithNullable",
                "testNoShortCircuitingInBitwiseUnsignedRightShiftWithNonNullable"
        };
    }

    @Test(description = "Test bitwise shift operation negative scenarios")
    public void testBitwiseShiftNegativeScenarios() {
        Assert.assertEquals(negativeResult.getErrorCount(), 15);
        int index = 0;
        BAssertUtil.validateError(negativeResult, index++, "operator '<<' not defined for 'float' and 'int'",
                26, 14);
        BAssertUtil.validateError(negativeResult, index++, "operator '<<' not defined for 'int' and 'A'",
                29, 14);
        BAssertUtil.validateError(negativeResult, index++, "operator '<<' not defined for 'int' and '(int|float)'",
                32, 14);
        BAssertUtil.validateError(negativeResult, index++, "operator '<<' not defined for 'int' and 'B'",
                35, 14);
        BAssertUtil.validateError(negativeResult, index++, "operator '<<' not defined for 'int' and 'float'",
                37, 14);
        BAssertUtil.validateError(negativeResult, index++, "operator '>>' not defined for 'float' and 'int'",
                39, 14);
        BAssertUtil.validateError(negativeResult, index++, "operator '>>' not defined for 'int' and 'A'",
                41, 14);
        BAssertUtil.validateError(negativeResult, index++, "operator '>>' not defined for 'int' and '(int|float)'",
                43, 14);
        BAssertUtil.validateError(negativeResult, index++, "operator '>>' not defined for 'int' and 'B'",
                45, 14);
        BAssertUtil.validateError(negativeResult, index++, "operator '>>' not defined for 'int' and 'float'",
                47, 15);
        BAssertUtil.validateError(negativeResult, index++, "operator '>>>' not defined for 'float' and 'int'",
                49, 15);
        BAssertUtil.validateError(negativeResult, index++, "operator '>>>' not defined for 'int' and 'A'",
                51, 15);
        BAssertUtil.validateError(negativeResult, index++, "operator '>>>' not defined for 'int' and '(int|float)'",
                53, 15);
        BAssertUtil.validateError(negativeResult, index++, "operator '>>>' not defined for 'int' and 'B'",
                55, 15);
        BAssertUtil.validateError(negativeResult, index, "operator '>>>' not defined for 'int' and 'float'",
                57, 15);
    }
}
