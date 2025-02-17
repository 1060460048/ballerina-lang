// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

type Person record {|
    string firstName;
    string lastName;
    int age;
|};

function testOnConflictClauseWithNonTableTypes() returns error? {
    error onConflictError = error("Key Conflict", message = "cannot insert.");
    Person p1 = {firstName: "Alex", lastName: "George", age: 33};
    Person p2 = {firstName: "John", lastName: "David", age: 35};
    Person p3 = {firstName: "Max", lastName: "Gomaz", age: 33};
    Person[] personList = [p1, p2, p3];

    Person[] _ =
            check from var person in personList
            let int newAge = 34
            where person.age == 33
            select {
                   firstName: person.firstName,
                   lastName: person.lastName,
                   age: newAge
            }
            on conflict onConflictError;
}

type Token record {|
    readonly int idx;
    string value;
|};

type TokenTable table<Token> key(idx);

function testQueryConstructingNonTableTypeHavingInnerQueriesWithOnConflictClause() returns error? {
    Token[] _ = check from Token i in (table key(idx) from var j in [1, 1]
            select {
                idx: j,
                value: "A"
            })
        select {
            idx: i.idx,
            value: "A"
        }
        on conflict error("Duplicate Key In Outer Query");

    Token[] _ = check from int i in 1 ... 3
        from Token j in (table key(idx) from var j in [1, 1]
            select {
                idx: j,
                value: "A"
            })
        select {
            idx: j.idx,
            value: "A"
        }
        on conflict error("Duplicate Key In Outer Query");

    Token[] _ = check from int i in 1 ... 3
        join Token j in (table key(idx) from var j in [1, 1]
            select {
                idx: j,
                value: "A"
            })
        on i equals j.idx
        select {
            idx: j.idx,
            value: "A"
        }
        on conflict error("Duplicate Key In Outer Query");

    Token[] _ = check from Token i in (from var j in (from var m in (table key(idx) from var j in [1, 1]
            select {
                idx: j,
                value: "A"
            }) select m) select j)
        select {
            idx: i.idx,
            value: "A"
        }
        on conflict error("Duplicate Key In Outer Query");

    Token[] _ = check from Token i in (check from var j in (check from var m in (check table key(idx) from var j in [1, 1]
                select {
                    idx: j,
                    value: "A"
                } on conflict error("Duplicate Key In Inner Query1")) select m)
             select j
             on conflict error("Duplicate Key In Inner Query2"))
        select {
            idx: i.idx,
            value: "A"
        };

    Token[] _ = check from int i in [1, 2, 1]
        let Token[] arr = from var j in (from var m in (table key(idx) from var j in [1, 1]
                        select {
                            idx: j,
                            value: "A"
                        }) select m) select j
        select {
            idx: arr[0].idx,
            value: "A" + i.toString()
        }
        on conflict error("Duplicate Key");

    Token[] _ = check from int i in [1, 2, 1]
        let Token[] arr = check from var j in (from var m in (table key(idx) from var j in [1, 1]
                        select {
                            idx: j,
                            value: "A"
                        }) select m) select j on conflict error("Duplicate Key")
        select {
            idx: arr[0].idx,
            value: "A" + i.toString()
        }
        on conflict error("Duplicate Key");

    Token[] _ = check from int i in [1, 2, 1]
        let TokenTable tb = table []
        where tb == from var j in (from var m in (table key(idx) from var j in [1, 1]
                        select {
                            idx: j,
                            value: "A"
                        }) select m) select j
        select {
            idx: 1,
            value: "A" + i.toString()
        }
        on conflict error("Duplicate Key");
}
