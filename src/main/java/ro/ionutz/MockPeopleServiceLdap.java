package ro.ionutz;

import jakarta.enterprise.context.ApplicationScoped;
import ro.ionutz.exception.CheckedLdapException;

import java.util.Map;

@ApplicationScoped
public class MockPeopleServiceLdap {

    public Map<String, String> getAttributeValues(String username, String... attrNames) throws CheckedLdapException {
        return Map.of("attr1", "value1",
                "attr2", "value2"
        );
    }
}
