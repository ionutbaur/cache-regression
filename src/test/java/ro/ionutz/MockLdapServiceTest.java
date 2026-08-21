package ro.ionutz;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.ionutz.exception.CheckedLdapException;
import ro.ionutz.exception.UncheckedCustomException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static ro.ionutz.MockLdapService.LDAP_USER_ATTRIBUTES;
import static ro.ionutz.MockLdapService.USER_ATTRIBUTES_CACHE_NAME;

@QuarkusTest
class MockLdapServiceTest {

    private static final String USERNAME = "fakeUsername";
    private static final Map<String, String> LDAP_ATTRIBUTES_VALUES = Map.of("fakeAttrKey", "fakeAttrVal");

    @Inject
    MockLdapService ldapService;

    @Inject
    @CacheName(USER_ATTRIBUTES_CACHE_NAME)
    Cache userAttributesCache;

    private MockPeopleServiceLdap peopleServiceLdap;

    @BeforeEach
    void setUp() {
        userAttributesCache.invalidateAll()
                .await()
                .indefinitely();

        peopleServiceLdap = mock(MockPeopleServiceLdap.class);
        ldapService.setPeopleServiceLdap(peopleServiceLdap);
    }

    @Test
    void getUserAttributes_isCached_whenNoErrors() throws CheckedLdapException {
        when(peopleServiceLdap.getAttributeValues(USERNAME, LDAP_USER_ATTRIBUTES))
                .thenReturn(LDAP_ATTRIBUTES_VALUES);

        Map<String, String> firstCallFreshResult = ldapService.getUserAttributes(USERNAME)
                .await()
                .indefinitely();
        Map<String, String> secondCallCachedResult = ldapService.getUserAttributes(USERNAME)
                .await()
                .indefinitely();

        verify(peopleServiceLdap, times(1)).getAttributeValues(USERNAME, LDAP_USER_ATTRIBUTES);
        assertEquals(LDAP_ATTRIBUTES_VALUES, firstCallFreshResult);
        assertEquals(LDAP_ATTRIBUTES_VALUES, secondCallCachedResult);
    }

    @Test
    void getUserAttributes_notCached_whenErrors() throws CheckedLdapException {
        when(peopleServiceLdap.getAttributeValues(USERNAME, LDAP_USER_ATTRIBUTES))
                .thenThrow(new CheckedLdapException("First call LDAP error"))
                .thenThrow(new CheckedLdapException("Second call LDAP error"))
                .thenReturn(LDAP_ATTRIBUTES_VALUES);

        UncheckedCustomException firstCallException = assertThrows(UncheckedCustomException.class, () -> ldapService.getUserAttributes(USERNAME)
                .await()
                .indefinitely()
        );
        assertEquals("First call LDAP error", firstCallException.getCause().getMessage());

        UncheckedCustomException secondCallException = assertThrows(UncheckedCustomException.class, () -> ldapService.getUserAttributes(USERNAME)
                .await()
                .indefinitely()
        );
        assertEquals("Second call LDAP error", secondCallException.getCause().getMessage());

        Map<String, String> thirdCallSuccessResult = ldapService.getUserAttributes(USERNAME)
                .await()
                .indefinitely();

        verify(peopleServiceLdap, times(3)).getAttributeValues(USERNAME, LDAP_USER_ATTRIBUTES);
        assertEquals(LDAP_ATTRIBUTES_VALUES, thirdCallSuccessResult);
    }

}