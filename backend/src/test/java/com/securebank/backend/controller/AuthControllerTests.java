package com.securebank.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;

import jakarta.servlet.http.HttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedUserCannotAccessMe() throws Exception {

        mockMvc.perform(
                get("/api/auth/me")
        )
        .andExpect(status().isUnauthorized());
    }

    /*  @Test
void validLoginCreatesAuthenticatedSession() throws Exception {

    mockMvc.perform(
            post("/api/auth/login")
                    .contentType("application/json")
                    .content("""
                            {
                              "email": "pavan-secure@example.com",
                              "password": "MyPassword123!"
                            }
                            """)
    )
    .andExpect(status().isOk())
    .andExpect(cookie().exists("JSESSIONID"));
} */

    @Test
void validLoginCreatesAuthenticatedSession() throws Exception {

    MvcResult result = mockMvc.perform(
            post("/api/auth/login")
                    .contentType("application/json")
                    .content("""
                            {
                              "email": "pavan-secure@example.com",
                              "password": "MyPassword123!"
                            }
                            """)
    )
    .andExpect(status().isOk())
    .andReturn();

    HttpSession session =
            result.getRequest().getSession(false);

    assertNotNull(session);
    assertEquals(5L, session.getAttribute("userId"));
}

@Test
void invalidPasswordCannotLogin() throws Exception {

    MvcResult result = mockMvc.perform(
            post("/api/auth/login")
                    .contentType("application/json")
                    .content("""
                            {
                              "email": "pavan-secure@example.com",
                              "password": "WrongPassword123!"
                            }
                            """)
    )
    .andExpect(status().isUnauthorized())
    .andReturn();

    HttpSession session =
            result.getRequest().getSession(false);

    assertTrue(
            session == null ||
            session.getAttribute("userId") == null
    );
}

@Test
void unknownEmailCannotLogin() throws Exception {

    MvcResult result = mockMvc.perform(
            post("/api/auth/login")
                    .contentType("application/json")
                    .content("""
                            {
                              "email": "does-not-exist@example.com",
                              "password": "AnyPassword123!"
                            }
                            """)
    )
    .andExpect(status().isUnauthorized())
    .andReturn();

    HttpSession session =
            result.getRequest().getSession(false);

    assertTrue(
            session == null ||
            session.getAttribute("userId") == null
    );
}

@Test
void authenticatedUserCanAccessMe() throws Exception {

    MvcResult loginResult = mockMvc.perform(
            post("/api/auth/login")
                    .contentType("application/json")
                    .content("""
                            {
                              "email": "pavan-secure@example.com",
                              "password": "MyPassword123!"
                            }
                            """)
    )
    .andExpect(status().isOk())
    .andReturn();

    HttpSession session =
            loginResult.getRequest().getSession(false);

    assertNotNull(session);
    assertEquals(5L, session.getAttribute("userId"));

    mockMvc.perform(
            get("/api/auth/me")
                    .session((MockHttpSession) session)
    )
    .andExpect(status().isOk());
}

@Test
void logoutInvalidatesSession() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    mockMvc.perform(
            post("/api/auth/logout")
                    .session(session)
    )
    .andExpect(status().isOk());

    mockMvc.perform(
            get("/api/auth/me")
                    .session(session)
    )
    .andExpect(status().isUnauthorized());
}

}