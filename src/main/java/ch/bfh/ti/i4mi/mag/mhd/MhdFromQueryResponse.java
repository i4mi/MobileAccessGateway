/*
 * Copyright 2020 the original author or authors.
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
package ch.bfh.ti.i4mi.mag.mhd;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp.Precision;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;

public abstract class MhdFromQueryResponse implements ToFhirTranslator<QueryResponse> {

    public String getSystem(String schemeName) {
        switch (schemeName) {
        case "2.16.840.1.113883.6.96":
            return "http://snomed.info/sct";
        }
        return schemeName;
    }

    public CodeableConcept transform(Code code) {
        return new CodeableConcept().addCoding(new Coding().setCode(code.getCode())
                .setSystem(getSystem(code.getSchemeName())).setDisplay(code.getDisplayName()==null ? "" : code.getDisplayName().getValue()));
    }

    public CodeableConcept transform(List<Code> codes) {
        CodeableConcept cc = new CodeableConcept();
        if (codes!=null) {
            for(Code code: codes) {
                cc.addCoding(new Coding().setCode(code.getCode()).setSystem(getSystem(code.getSchemeName())).setDisplay(code.getDisplayName()==null ? "" : code.getDisplayName().getValue()));
            }
        }
        return cc; 
    }
    
    public List<CodeableConcept> transformMultiple(List<Code> codes) {
        List<CodeableConcept> ccList = new ArrayList<CodeableConcept>();
        if (codes!=null) {
            for(Code code: codes) {
            	ccList.add(transform(code));               
            }
        }
        return ccList; 
    }
    
    public DateTimeType transform(Timestamp timestamp) {
    	if (timestamp == null) return null;
    	Date date = Date.from(timestamp.getDateTime().toInstant());
    	Precision precision = timestamp.getPrecision();
    	TemporalPrecisionEnum fhirPrecision;
    	switch(precision) {
    	case YEAR: fhirPrecision = TemporalPrecisionEnum.YEAR;break;
    	case DAY: fhirPrecision = TemporalPrecisionEnum.DAY;break;
    	// There is no mapping for HOUR
    	case HOUR: fhirPrecision = TemporalPrecisionEnum.MINUTE;break;
    	case MINUTE: fhirPrecision = TemporalPrecisionEnum.MINUTE;break;
    	case SECOND: fhirPrecision = TemporalPrecisionEnum.SECOND;break;
    	default: fhirPrecision = TemporalPrecisionEnum.MILLI;break;
    	}
    	return new DateTimeType(date, fhirPrecision);
    }

}
