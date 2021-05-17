package health.ere.ps.event;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.hl7.fhir.r4.model.Bundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class SignAndUploadBundlesEvent {
    public List<List<Bundle>> listOfListOfBundles = new ArrayList<>();
    public String bearerToken;

    static IParser jsonParser = FhirContext.forR4().newJsonParser();

    public SignAndUploadBundlesEvent() {
        
    }

    public SignAndUploadBundlesEvent(JsonObject jsonObject) {
        for(JsonValue jsonValue : jsonObject.getJsonArray("payload")) {
            List<Bundle> bundles = new ArrayList<>();
            if(jsonValue instanceof JsonArray) {
                for(JsonValue singleBundle : (JsonArray) jsonValue) {
                    bundles.add(jsonParser.parseResource(Bundle.class, singleBundle.toString()));
                }
            }
            listOfListOfBundles.add(bundles);
        }
    }

}