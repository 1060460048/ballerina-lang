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
package io.ballerina.semtype.typeops;

import io.ballerina.semtype.Common;
import io.ballerina.semtype.Conjunction;
import io.ballerina.semtype.Core;
import io.ballerina.semtype.MappingAtomicType;
import io.ballerina.semtype.PredefinedType;
import io.ballerina.semtype.SemType;
import io.ballerina.semtype.TypeCheckContext;
import io.ballerina.semtype.UniformTypeOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// todo: use this to place common things between Ro and RW, if there are non; delete this.
/**
 * Common mapping related methods operate on SubtypeData.
 *
 * @since 2.0.0
 */
public abstract class MappingCommonOps extends CommonOps implements UniformTypeOps {

    public static boolean mappingFormulaIsEmpty(TypeCheckContext tc, Conjunction posList, Conjunction negList) {
        MappingAtomicType combined;
        if (posList == null) {
            combined = MappingAtomicType.from(new String[0], new SemType[0], PredefinedType.TOP);
        } else {
            // combine all the positive atoms using intersection
            combined = tc.mappingAtomType(posList.atom);
            Conjunction p = posList.next;
            while (true) {
                if (p == null) {
                    break;
                } else {
                    MappingAtomicType m = intersectMapping(combined, tc.mappingAtomType(p.atom));
                    if (m == null) {
                        return true;
                    } else {
                        combined = m;
                    }
                    p = p.next;
                }
            }
           for (SemType t : combined.types) {
                if (Core.isEmpty(tc, t)) {
                    return true;
                }
            }

        }
        return !mappingInhabited(tc, combined, negList);
    }

    private static boolean mappingInhabited(TypeCheckContext tc, MappingAtomicType pos, Conjunction negList) {
        if (negList == null) {
            return true;
        } else {
            MappingAtomicType neg = tc.mappingAtomType(negList.atom);

            FieldPairs pairing;

            if (pos.names != neg.names) {
                // If this negative type has required fields that the positive one does not allow
                // or vice-versa, then this negative type has no effect,
                // so we can move on to the next one

                // Deal the easy case of two closed records fast.
                if (Core.isNever(pos.rest) && Core.isNever(neg.rest)) {
                    return mappingInhabited(tc, pos, negList.next);
                }
                pairing = new FieldPairs(pos, neg);
                for (FieldPair fieldPair : pairing) {
                    if (Core.isNever(fieldPair.type1) || Core.isNever(fieldPair.type2)) {
                        return mappingInhabited(tc, pos, negList.next);
                    }
                }
                pairing.itr.reset();
            } else {
                pairing = new FieldPairs(pos, neg);
            }

            if (!Core.isEmpty(tc, Core.diff(pos.rest, neg.rest))) {
                return true;
            }
            for (FieldPair fieldPair : pairing) {
                SemType d = Core.diff(fieldPair.type1, fieldPair.type2);
                if (!Core.isEmpty(tc, d)) {
                    MappingAtomicType mt;
                    Optional<Integer> i = pairing.itr.index1(fieldPair.name);
                    if (i.isEmpty()) {
                        // the posType came from the rest type
                        mt = insertField(pos, fieldPair.name, d);
                    } else {
                        SemType[] posTypes = Common.shallowCopyTypes(pos.types);
                        posTypes[i.get()] = d;
                        mt = MappingAtomicType.from(pos.names, posTypes, pos.rest);
                    }
                    if (mappingInhabited(tc, mt, negList.next)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static MappingAtomicType insertField(MappingAtomicType m, String name, SemType t) {
        String[] names = Common.shallowCopyStrings(m.names);
        SemType[] types = Common.shallowCopyTypes(m.types);
        int i = names.length;
        while (true) {
            if (i == 0 || name.codePoints() <= names[i - 1].codePoints()) {
                names[i] = name;
                types[i] = t;
                break;
            }
            names[i] = names[i - 1];
            types[i] = types[i - 1];
            i -= 1;
        }
        return MappingAtomicType.from(names, types, m.rest);
    }

    private static MappingAtomicType intersectMapping(MappingAtomicType m1, MappingAtomicType m2) {
        List<String> names = new ArrayList<>();
        List<SemType> types = new ArrayList<>();
        FieldPairs pairing = new FieldPairs(m1, m2);
        for (FieldPair fieldPair : pairing) {
            names.add(fieldPair.name);
            SemType t = Core.intersect(fieldPair.type1, fieldPair.type2);
            if (Core.isNever(t)) {
                return null;
            }
            types.add(t);
        }
        SemType rest = Core.intersect(m1.rest, m2.rest);
        return MappingAtomicType.from(names.toArray(new String[0]), types.toArray(new SemType[0]), rest);
    }
}
