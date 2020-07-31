package ch.bfh.ti.i4mi.mag.pmir;

import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Period;

import ch.bfh.ti.i4mi.mag.BaseRequestConverter;
import net.ihe.gazelle.hl7v3.datatypes.IVLTS;
import net.ihe.gazelle.hl7v3.datatypes.IVXBTS;
import net.ihe.gazelle.hl7v3.datatypes.TEL;
import net.ihe.gazelle.hl7v3.datatypes.TS;

/**
 * base class for PMIR/PIX request converters
 * @author alexander kreutz
 *
 */
public class PMIRRequestConverter extends BaseRequestConverter {

	public static <T extends net.ihe.gazelle.hl7v3.datatypes.ST> T element(Class<T> cl, String content) {
		try {
			T instance = cl.getDeclaredConstructor().newInstance();
			instance.addMixed(content);
			return instance;
		} catch (Exception e) { return null; }
	}
	
	public static IVLTS transform(Period period) {
		if (period == null) return null;
		IVLTS result = new IVLTS();
		if (period.hasStart()) result.setLow(transform(period.getStartElement()));
		if (period.hasEnd()) result.setHigh(transform(period.getEndElement()));
		return result;
	}
	
	public static IVXBTS transform(DateTimeType date) {
		IVXBTS result = new IVXBTS();
		result.setValue(date.getAsV3());
		return result;
	}
	
	public static TS transform(DateType date) {
		if (date == null || !(date.hasValue())) return null;
		TS result = new TS();
		result.setValue(date.getValueAsString().replace("-",""));
		return result;
	}
	
	public static TEL transform(ContactPoint contactPoint) {
        TEL telecom = new TEL();
    			        		        	
    	ContactPoint.ContactPointUse use = contactPoint.getUse();		        	
    	if (use != null) telecom.setUse(use.toString());
    	telecom.setValue(contactPoint.getValue());
    	if (contactPoint.hasPeriod()) telecom.addUseablePeriod(transform(contactPoint.getPeriod()));
    	
    	return telecom;
	}
	  
}
