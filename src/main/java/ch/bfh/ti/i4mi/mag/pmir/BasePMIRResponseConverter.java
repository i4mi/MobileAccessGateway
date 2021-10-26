package ch.bfh.ti.i4mi.mag.pmir;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;

import net.ihe.gazelle.hl7v3.datatypes.ED;

public class BasePMIRResponseConverter {

	public OperationOutcome error(IssueType type, String diagnostics) {
		OperationOutcome result = new OperationOutcome();
		
		OperationOutcomeIssueComponent issue = result.addIssue();
		issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
		issue.setCode(type);
		issue.setDiagnostics(diagnostics);
		return result;
	}
	
	public String toText(ED in) {
		StringBuffer result = new StringBuffer();
		for (java.io.Serializable obj : in.getMixed()) {
			if (obj instanceof String) {
				result.append((String) obj);
			}
		}
		return result.toString();
	}

}
