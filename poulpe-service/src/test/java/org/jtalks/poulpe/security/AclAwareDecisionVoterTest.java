package org.jtalks.poulpe.security;

import org.jtalks.common.model.entity.User;
import org.jtalks.common.model.entity.ComponentType;
import org.jtalks.poulpe.model.entity.PoulpeUser;
import org.jtalks.poulpe.service.UserService;
import org.mockito.Matchers;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestAttributes;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.access.AccessDecisionVoter.ACCESS_DENIED;
import static org.springframework.security.access.AccessDecisionVoter.ACCESS_GRANTED;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_SESSION;
import static org.testng.Assert.assertEquals;

/**
 * @author dionis
 */
public class AclAwareDecisionVoterTest {
    private static final String USERNAME = "USERNAME";
    private static final String AUTHORIZED = "authorizedPoulpeUser";
    private static final List<ConfigAttribute> ATTRIBUTES = Collections.emptyList();
    private AclAwareDecisionVoter voter;
    private AccessDecisionVoter baseVoter;
    private UserService userService;

    @BeforeMethod
    public void setUp() {
        baseVoter = mock(AccessDecisionVoter.class);
        userService = mock(UserService.class);

        voter = spy(new AclAwareDecisionVoter(baseVoter, userService));
    }

    @Test
    public void notAuthorized() {
        Authentication authentication = mock(Authentication.class);
        authenticatedSuccessfully(authentication);
        when(authentication.getPrincipal()).thenReturn("anonymous");

        assertEquals(voter.vote(authentication, new Object(), ATTRIBUTES), ACCESS_GRANTED);
    }

    @Test
    public void notPoulpeUserShouldFail() {
        Authentication authentication = mock(Authentication.class);
        RequestAttributes requestAttributes = mock(RequestAttributes.class);

        doReturn(requestAttributes).when(voter).getRequestAttributes();

        authenticatedSuccessfully(authentication);
        when(authentication.getPrincipal()).thenReturn(new User("user", "user@mail.com", "pass", "salt"));

        assertEquals(voter.vote(authentication, new Object(), ATTRIBUTES), ACCESS_DENIED);
    }

    @Test
    public void firstUnsuccessfulAttempt() {
        Authentication authentication = mock(Authentication.class);
        RequestAttributes requestAttributes = mock(RequestAttributes.class);

        doReturn(requestAttributes).when(voter).getRequestAttributes();

        authenticatedSuccessfully(authentication);
        userHaveNotBeenAuthorizedYet(requestAttributes);
        unsuccessfulAuthorizationFlow(authentication, createPoulpeUserWithPredefinedName());

        assertEquals(voter.vote(authentication, new Object(), ATTRIBUTES), ACCESS_DENIED);

        verify(requestAttributes).setAttribute(eq(AUTHORIZED), eq(false), eq(SCOPE_SESSION));
    }

    private PoulpeUser createPoulpeUserWithPredefinedName() {
        PoulpeUser user = new PoulpeUser();
        user.setUsername(USERNAME);
        return user;
    }

    private void unsuccessfulAuthorizationFlow(Authentication authentication, PoulpeUser poulpeUser) {
        when(authentication.getPrincipal()).thenReturn(poulpeUser);
        when(userService.accessAllowedToComponentType(eq(USERNAME), eq(ComponentType.ADMIN_PANEL))).thenReturn(false);
    }

    private void successfulAuthorizationFlow(Authentication authentication, PoulpeUser poulpeUser) {
        when(authentication.getPrincipal()).thenReturn(poulpeUser);
        when(userService.accessAllowedToComponentType(eq(USERNAME), eq(ComponentType.ADMIN_PANEL))).thenReturn(true);
    }

    private void userHaveNotBeenAuthorizedYet(RequestAttributes requestAttributes) {
        when(requestAttributes.getAttribute(eq(AUTHORIZED), eq(SCOPE_SESSION))).thenReturn(null);
    }

    @Test
    public void negativeResultOfAuthorizationAfterNegativeAuthorizationResultRememberedInSession() {
        Authentication authentication = mock(Authentication.class);
        RequestAttributes requestAttributes = mock(RequestAttributes.class);

        doReturn(requestAttributes).when(voter).getRequestAttributes();

        authenticatedSuccessfully(authentication);
        previousAttemptToAuthorizeFailed(requestAttributes);
        unsuccessfulAuthorizationFlow(authentication, createPoulpeUserWithPredefinedName());

        assertEquals(voter.vote(authentication, new Object(), ATTRIBUTES), ACCESS_DENIED);

        verify(requestAttributes).setAttribute(eq(AUTHORIZED), eq(false), eq(SCOPE_SESSION));
    }

    @Test
    public void positiveResultOfAuthorizationAfterNegativeAuthorizationResultRememberedInSession() {
        Authentication authentication = mock(Authentication.class);
        RequestAttributes requestAttributes = mock(RequestAttributes.class);

        doReturn(requestAttributes).when(voter).getRequestAttributes();

        authenticatedSuccessfully(authentication);
        previousAttemptToAuthorizeFailed(requestAttributes);
        successfulAuthorizationFlow(authentication, createPoulpeUserWithPredefinedName());

        assertEquals(voter.vote(authentication, new Object(), ATTRIBUTES), ACCESS_GRANTED);

        verify(requestAttributes).setAttribute(eq(AUTHORIZED), eq(true), eq(SCOPE_SESSION));
    }

    private void previousAttemptToAuthorizeFailed(RequestAttributes requestAttributes) {
        when(requestAttributes.getAttribute(eq(AUTHORIZED), eq(SCOPE_SESSION))).thenReturn(false);
    }

    @Test
    public void successfulAuthorizationShouldBeRememberedInSession() {
        Authentication authentication = mock(Authentication.class);
        RequestAttributes requestAttributes = mock(RequestAttributes.class);

        doReturn(requestAttributes).when(voter).getRequestAttributes();

        authenticatedSuccessfully(authentication);
        userHaveNotBeenAuthorizedYet(requestAttributes);
        successfulAuthorizationFlow(authentication, createPoulpeUserWithPredefinedName());

        assertEquals(voter.vote(authentication, new Object(), ATTRIBUTES), ACCESS_GRANTED);

        verify(requestAttributes).setAttribute(eq(AUTHORIZED), eq(true), eq(SCOPE_SESSION));
    }

    @Test
    public void successfulAuthorizationResultShouldBeRememberedInSession() {
        Authentication authentication = mock(Authentication.class);
        RequestAttributes requestAttributes = mock(RequestAttributes.class);

        doReturn(requestAttributes).when(voter).getRequestAttributes();

        authenticatedSuccessfully(authentication);
        when(authentication.getPrincipal()).thenReturn(createPoulpeUserWithPredefinedName());
        userWasAuthorizedSuccessfully(requestAttributes);

        assertEquals(voter.vote(authentication, new Object(), ATTRIBUTES), ACCESS_GRANTED);
    }

    @Test
    public void supportsAttributeShouldBeDelegatedToBaseVoter() {
        ConfigAttribute configAttribute = mock(ConfigAttribute.class);
        voter.supports(configAttribute);

        verify(baseVoter).supports(eq(configAttribute));
    }

    @Test
    public void supportsClassShouldBeDelegatedToBaseVoter() {
        Class<?> clazz = Object.class;
        voter.supports(clazz);

        verify(baseVoter).supports(eq(clazz));
    }

    private void userWasAuthorizedSuccessfully(RequestAttributes requestAttributes) {
        when(requestAttributes.getAttribute(eq(AUTHORIZED), eq(SCOPE_SESSION))).thenReturn(true);
    }

    private void authenticatedSuccessfully(Authentication authentication) {
        when(baseVoter.vote(eq(authentication), anyObject(), Matchers.<Collection<ConfigAttribute>>any())).thenReturn(ACCESS_GRANTED);
        when(authentication.isAuthenticated()).thenReturn(true);
    }
}
