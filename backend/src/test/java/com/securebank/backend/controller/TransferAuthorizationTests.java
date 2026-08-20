package com.securebank.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

}