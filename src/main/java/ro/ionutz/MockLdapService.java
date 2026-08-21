package ro.ionutz;

import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import ro.ionutz.exception.CheckedLdapException;
import ro.ionutz.exception.UncheckedCustomException;

import java.util.Arrays;
import java.util.Map;

@ApplicationScoped
public class MockLdapService {

    static final String[] LDAP_USER_ATTRIBUTES = {"attr1", "attr2"};
    static final String USER_ATTRIBUTES_CACHE_NAME = "ldapService-userAttributes";

    private MockPeopleServiceLdap peopleServiceLdap;

    @CacheResult(cacheName = USER_ATTRIBUTES_CACHE_NAME)
    public Uni<Map<String, String>> getUserAttributes(String username) {
        return Uni
                .createFrom()
                .item(Unchecked.supplier(() -> {
                    try {
                        return peopleServiceLdap.getAttributeValues(username, LDAP_USER_ATTRIBUTES);
                    } catch (CheckedLdapException e) {
                        throw new UncheckedCustomException("Could not retrieve for user " + username +
                                " the following user attributes from LDAP: " + Arrays.asList(LDAP_USER_ATTRIBUTES), e);
                    }
                }))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()); // works in 3.38.3 if runSubscriptionOn removed, but the LDAP call is blocking I/O and must not run on the Quarkus event loop thread in production
    }

    public void setPeopleServiceLdap(MockPeopleServiceLdap peopleServiceLdap) {
        this.peopleServiceLdap = peopleServiceLdap;
    }
}
