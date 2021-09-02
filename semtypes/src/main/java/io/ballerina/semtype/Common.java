/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package io.ballerina.semtype;

import io.ballerina.semtype.subtypedata.BddAllOrNothing;
import io.ballerina.semtype.subtypedata.BddNode;
import io.ballerina.semtype.typeops.BddCommonOps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Code common to implementation of multiple basic types.
 *
 * @since 2.0.0
 */
public class Common {

    public static boolean typeListIsReadOnly(SemType[] list) {
        for (SemType t : list) {
            if (!Core.isReadOnly(t)) {
                return false;
            }
        }
        return true;
    }


    public static SemType[] readOnlyTypeList(SemType[] mt) {
        List<SemType> types = new ArrayList<>();
        for (SemType s : mt) {
            SemType t;
            if (Core.isReadOnly(s)) {
                t = s;
            } else {
                t = Core.intersect(s, PredefinedType.READONLY);
            }
            types.add(t);
        }

        return types.toArray(new SemType[]{});
    }

    // [from nballerina] A Bdd represents a disjunction of conjunctions of atoms, where each atom is either positive or
    // negative (negated). Each path from the root to a leaf that is true represents one of the conjunctions
    // We walk the tree, accumulating the positive and negative conjunctions for a path as we go.
    // When we get to a leaf that is true, we apply the predicate to the accumulated conjunctions.

    public static boolean bddEvery(TypeCheckContext tc,
                                   Bdd b,
                                   Conjunction pos,
                                   Conjunction neg,
                                   BddPredicate predicate) {
        if (b instanceof BddAllOrNothing) {
            return !((BddAllOrNothing) b).isAll() || predicate.apply(tc, pos, neg);
        } else {
            BddNode bn = (BddNode) b;
            return bddEvery(tc, bn.left, Conjunction.and(bn.atom, pos), neg, predicate)
                    && bddEvery(tc, bn.middle, pos, neg, predicate)
                    && bddEvery(tc, bn.right, pos, Conjunction.and(bn.atom, neg), predicate);
        }
    }

    public static boolean bddEveryPositive(TypeCheckContext tc, Bdd b, Conjunction pos, Conjunction neg,
                                           BddPredicate predicate) {
        if (b instanceof BddAllOrNothing) {
            return !((BddAllOrNothing) b).isAll() || predicate.apply(tc, pos, neg);
        } else {
            BddNode bn = (BddNode) b;
            return bddEveryPositive(tc, bn.left, Conjunction.and(bn.atom, pos), neg, predicate)
                    && bddEveryPositive(tc, bn.middle, pos, neg, predicate)
                    && bddEveryPositive(tc, bn.right, pos, Conjunction.and(bn.atom, neg), predicate);
        }
    }

    /* [from nballerina] The goal of this is to ensure that mappingFormulaIsEmpty does
    not get an empty posList, because it will interpret that
    as `map<any|error>` rather than `map<readonly>`.
    Similarly, for listFormulaIsEmpty.
    We want to share BDDs between the RW and RO case, so we cannot change how the BDD is interpreted.
    Instead, we transform the BDD to avoid cases that would give the wrong answer.
    Atom index 0 is LIST_SUBTYPE_RO and MAPPING_SUBTYPE_RO */
    public static Bdd bddFixReadOnly(Bdd b) {
        return bddPosMaybeEmpty(b) ? BddCommonOps.bddIntersect(b, BddCommonOps.bddAtom(RecAtom.createRecAtom(0))) : b;
    }

    public static boolean bddPosMaybeEmpty(Bdd b) {
        if (b instanceof BddAllOrNothing) {
            return ((BddAllOrNothing) b).isAll();
        } else {
            BddNode bn = (BddNode) b;
            return bddPosMaybeEmpty(bn.middle) || bddPosMaybeEmpty(bn.right);
        }
    }

    public static Conjunction andIfPositive(Atom atom, Conjunction next) {
        if (atom instanceof RecAtom && ((RecAtom) atom).index < 0) {
            return next;
        }
        return Conjunction.and(atom, next);
    }

    public static SemType[] shallowCopyTypes(SemType[] v) {
        return Arrays.copyOf(v, v.length);
    }

    public static List<SemType> shallowCopyTypes(List<SemType> v) {
        return new ArrayList<>(v);
    }

    public static String[] shallowCopyStrings(String[] v) {
        return Arrays.copyOf(v, v.length);
    }

    public static boolean notIsEmpty(TypeCheckContext tc, SubtypeData d) {
        return false;
    }

    // Returns whether s1.codePoints < s2.codePoints
    public static boolean codePointCompare(String s1, String s2) {
        int cplen1 = s1.codePointCount(0, s1.length());
        int cplen2 = s1.codePointCount(0, s2.length());
        if (cplen1 < cplen2) {
            return true;
        } else if (cplen1 > cplen2) {
            return false;
        }
        int len1 = s1.length();
        for (int offset1 = 0, offset2 = 0; offset1 < len1; ) {
            int codepoint1 = s1.codePointAt(offset1);
            int codepoint2 = s2.codePointAt(offset2);
            if (codepoint1 > codepoint2) {
                return false;
            } else if (codepoint1 < codepoint2) {
                return true;
            }
            offset1 += Character.charCount(codepoint1);
            offset2 += Character.charCount(codepoint2);
        }
        return false;
    }

    /**
     * Function interface used for method references.
     *
     * @since 2.0.0
     */
    public interface BddPredicate {
        boolean apply(TypeCheckContext tc, Conjunction posList, Conjunction negList);
    }
}
