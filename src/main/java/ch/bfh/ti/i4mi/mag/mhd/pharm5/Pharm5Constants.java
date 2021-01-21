/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.bfh.ti.i4mi.mag.mhd.pharm5;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Oliver Egger
 */
public interface Pharm5Constants {

    final String PHARM5_OPERATION_NAME = "$find-medication-list";
    final String PHARM5_STATUS = "status";
    final String PHARM5_PATIENT_IDENTIFIER = "patient.identifier";
    final String PHARM5_FORMAT = "format";

    // needs to be extended
    Set<String> PHARM5_PARAMETERS = new HashSet<>(Arrays.asList(
            PHARM5_STATUS,
            PHARM5_PATIENT_IDENTIFIER,
            PHARM5_FORMAT));
}
