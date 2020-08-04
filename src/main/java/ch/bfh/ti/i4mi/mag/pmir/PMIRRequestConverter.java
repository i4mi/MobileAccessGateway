package ch.bfh.ti.i4mi.mag.pmir;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;

import ch.bfh.ti.i4mi.mag.BaseRequestConverter;
import net.ihe.gazelle.hl7v3.datatypes.AD;
import net.ihe.gazelle.hl7v3.datatypes.AdxpCity;
import net.ihe.gazelle.hl7v3.datatypes.AdxpCountry;
import net.ihe.gazelle.hl7v3.datatypes.AdxpPostalCode;
import net.ihe.gazelle.hl7v3.datatypes.AdxpState;
import net.ihe.gazelle.hl7v3.datatypes.AdxpStreetAddressLine;
import net.ihe.gazelle.hl7v3.datatypes.EnFamily;
import net.ihe.gazelle.hl7v3.datatypes.EnGiven;
import net.ihe.gazelle.hl7v3.datatypes.EnPrefix;
import net.ihe.gazelle.hl7v3.datatypes.EnSuffix;
import net.ihe.gazelle.hl7v3.datatypes.IVLTS;
import net.ihe.gazelle.hl7v3.datatypes.IVXBTS;
import net.ihe.gazelle.hl7v3.datatypes.PN;
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
	
	
	public static AD transform(Address address) {
		AD addr = new AD();
	
		// TODO Missing: district, type, use
		if (address.hasCity()) addr.addCity(element(AdxpCity.class, address.getCity()));
		if (address.hasCountry()) addr.addCountry(element(AdxpCountry.class, address.getCountry()));
		if (address.hasPostalCode()) addr.addPostalCode(element(AdxpPostalCode.class, address.getPostalCode()));
		if (address.hasState()) addr.addState(element(AdxpState.class, address.getState()));
		if (address.hasLine()) for (StringType line : address.getLine()) addr.addStreetAddressLine(element(AdxpStreetAddressLine.class, line.getValue()));
		if (address.hasPeriod()) addr.addUseablePeriod(transform(address.getPeriod()));
		return addr;
	}
	
	public static PN transform(HumanName name) { 
		PN nameElement = new PN();
		if (name.hasFamily()) nameElement.addFamily(element(EnFamily.class, name.getFamily()));
		for (StringType given : name.getGiven()) nameElement.addGiven(element(EnGiven.class, given.getValue()));
		for (StringType prefix : name.getPrefix()) nameElement.addPrefix(element(EnPrefix.class, prefix.getValue()));
		for (StringType suffix : name.getSuffix()) nameElement.addSuffix(element(EnSuffix.class, suffix.getValue()));
		if (name.hasPeriod()) nameElement.addValidTime(transform(name.getPeriod()));		    		
		return nameElement;
	}
}
