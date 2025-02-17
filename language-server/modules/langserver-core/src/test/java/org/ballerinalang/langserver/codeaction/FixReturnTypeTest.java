/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.langserver.codeaction;

import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Test Cases for CodeActions.
 *
 * @since 2.0.0
 */
public class FixReturnTypeTest extends AbstractCodeActionTest {

    @Override
    public String getResourceDir() {
        return "fix-return-type";
    }

    @Override
    @Test(dataProvider = "codeaction-data-provider")
    public void test(String config, String source) throws IOException, WorkspaceDocumentException {
        super.test(config, source);
    }

    @DataProvider(name = "codeaction-data-provider")
    @Override
    public Object[][] dataProvider() {
        return new Object[][]{
                {"fixReturnType1.json", "fixReturnType.bal"},
                {"fixReturnType2.json", "fixReturnType.bal"},
                {"fixReturnType3.json", "fixReturnType.bal"},
                {"fixReturnType4.json", "fixReturnType.bal"},
                {"fixReturnTypeWithImports1.json", "fixReturnTypeWithImports.bal"},
                {"fixReturnTypeWithClass1.json", "fixReturnTypeInClass.bal"},
                {"fixReturnTypeWithClass2.json", "fixReturnTypeInClass.bal"},
                {"fixReturnTypeWithClass3.json", "fixReturnTypeInClass.bal"},
                {"fixReturnTypeWithService1.json", "fixReturnTypeInService.bal"},
                {"fixReturnTypeWithCheckExpr1.json", "fixReturnTypeWithCheckExpr1.bal"},
                {"fixReturnTypeWithCheckExpr2.json", "fixReturnTypeWithCheckExpr2.bal"},
                {"fixReturnTypeWithCheckExpr3.json", "fixReturnTypeWithCheckExpr3.bal"},
                {"fixReturnTypeWithCheckExpr4.json", "fixReturnTypeWithCheckExpr4.bal"}
        };
    }
}
