package com.securebank.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedUserCannotTransferMoney() throws Exception {

        mockMvc.perform(
                post("/api/transfers")
                        .contentType("application/json")
                        .content("""
                                {
                                    "fromAccountId": 1,
                                    "toAccountId": 2,
                                    "amount": 100.00
                                }
                                """)
        )
        .andExpect(status().isUnauthorized());
    }
    
    @Test
void customerCannotTransferFromAnotherUsersAccount() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .header("Idempotency-Key", "authorization-test-001")
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 2,
                                "toAccountId": 1,
                                "amount": 1000.00
                            }
                            """)
    )
    .andExpect(status().isForbidden());
}


@Test
void customerCanTransferToAnotherUsersAccount() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .header("Idempotency-Key", "authorization-test-002")
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 1,
                                "toAccountId": 2,
                                "amount": 1000.00
                            }
                            """)
    )
    .andExpect(status().isOk());
}


@Test
void customerCannotTransferToNonexistentAccount() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .header("Idempotency-Key", "authorization-test-003")
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 1,
                                "toAccountId": 999,
                                "amount": 1000.00
                            }
                            """)
    )
    .andExpect(status().isNotFound());
}

@Test
void customerCannotTransferToSameAccount() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .header("Idempotency-Key", "authorization-test-004")
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 1,
                                "toAccountId": 1,
                                "amount": 1000.00
                            }
                            """)
    )
    .andExpect(status().isBadRequest());
}

@Test
void customerCannotTransferNegativeAmount() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .header("Idempotency-Key", "authorization-test-005")
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 1,
                                "toAccountId": 2,
                                "amount": -1000.00
                            }
                            """)
    )
    .andExpect(status().isBadRequest());
}

@Test
void customerCannotTransferZeroAmount() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .header("Idempotency-Key", "authorization-test-006")
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 1,
                                "toAccountId": 2,
                                "amount": 0.00
                            }
                            """)
    )
    .andExpect(status().isBadRequest());
}

@Test
void customerCannotTransferMoreThanAvailableBalance() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .header("Idempotency-Key", "authorization-test-007")
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 1,
                                "toAccountId": 2,
                                "amount": 15000.00
                            }
                            """)
    )
    .andExpect(status().isBadRequest());
}

@Test
void transferRequiresIdempotencyKey() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 1,
                                "toAccountId": 2,
                                "amount": 1000.00
                            }
                            """)
    )
    .andExpect(status().isBadRequest());
}

@Test
void transferAcceptsValidIdempotencyKey() throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .header("Idempotency-Key", "test-key-valid-001")
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 1,
                                "toAccountId": 2,
                                "amount": 1000.00
                            }
                            """)
    )
    .andExpect(status().isOk());
}

@Test
void retryingSameRequestWithSameIdempotencyKeyMustNotTransferTwice()
        throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    String requestBody = """
            {
                "fromAccountId": 1,
                "toAccountId": 2,
                "amount": 1000.00
            }
            """;

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .header("Idempotency-Key", "http-idempotency-001")
                    .contentType("application/json")
                    .content(requestBody)
    )
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.transferId").exists())
    .andExpect(jsonPath("$.status").value("COMPLETED"));

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .header("Idempotency-Key", "http-idempotency-001")
                    .contentType("application/json")
                    .content(requestBody)
    )
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.transferId").exists())
    .andExpect(jsonPath("$.status").value("COMPLETED"));
}

@Test
void sameIdempotencyKeyWithDifferentRequestMustBeRejected()
        throws Exception {

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userId", 5L);

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .header("Idempotency-Key", "http-conflict-001")
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 1,
                                "toAccountId": 2,
                                "amount": 1000.00
                            }
                            """)
    )
    .andExpect(status().isOk());

    mockMvc.perform(
            post("/api/transfers")
                    .session(session)
                    .header("Idempotency-Key", "http-conflict-001")
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 1,
                                "toAccountId": 2,
                                "amount": 2000.00
                            }
                            """)
    )
    .andExpect(status().isConflict());
}

@Test
void differentUserCannotReuseAnotherUsersIdempotencyKey()
        throws Exception {

    String idempotencyKey = "cross-user-key-001";

    MockHttpSession user5Session =
            new MockHttpSession();

    user5Session.setAttribute("userId", 5L);

    mockMvc.perform(
            post("/api/transfers")
                    .session(user5Session)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 1,
                                "toAccountId": 2,
                                "amount": 1000.00
                            }
                            """)
    )
    .andExpect(status().isOk());


    MockHttpSession user7Session =
            new MockHttpSession();

    user7Session.setAttribute("userId", 7L);

    mockMvc.perform(
            post("/api/transfers")
                    .session(user7Session)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType("application/json")
                    .content("""
                            {
                                "fromAccountId": 2,
                                "toAccountId": 1,
                                "amount": 1000.00
                            }
                            """)
    )
    .andExpect(status().isForbidden());
}
}
