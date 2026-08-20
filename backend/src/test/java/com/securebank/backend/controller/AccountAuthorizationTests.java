package com.securebank.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedUserCannotAccessAccount() throws Exception {

        mockMvc.perform(
                get("/api/accounts/1")
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
void customerCanAccessOwnAccount() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    mockMvc.perform(
            get("/api/accounts/1")
                    .session(session)
    )
    .andExpect(status().isOk());
}

    @Test
    void customerCannotAccessOthersAccount() throws Exception {

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 5L);

        mockMvc.perform(
                get("/api/accounts/2")
                        .session(session)
        )
        .andExpect(status().isForbidden());
    }

@Test
void adminCanAccessAnotherUsersAccount() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 22L);

    mockMvc.perform(
            get("/api/accounts/1")
                    .session(session)
    )
    .andExpect(status().isOk());
}

@Test
void nonexistentAuthenticatedUserCannotAccessAccount() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 99999L);

    mockMvc.perform(
            get("/api/accounts/1")
                    .session(session)
    )
    .andExpect(status().isUnauthorized());
}



}