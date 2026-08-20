package com.securebank.backend.controller;

import com.securebank.backend.dto.TransferRequest;
import com.securebank.backend.exception.AccessDeniedException;
import com.securebank.backend.service.TransferService;


import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpSession;


import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferControllerUnitTests {

    @Test
    void customerCannotTransferFromAnotherUsersAccount() throws Exception {

        TransferService transferService =
                mock(TransferService.class);

        doThrow(new AccessDeniedException("Access denied"))
        .when(transferService)
        .transfer(
                org.mockito.ArgumentMatchers.any(TransferRequest.class),
                org.mockito.ArgumentMatchers.eq(5L)
        );

        TransferController controller =
                new TransferController(transferService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new com.securebank.backend.exception.GlobalExceptionHandler())
                .build();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 5L);

        mockMvc.perform(
                post("/api/transfers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
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