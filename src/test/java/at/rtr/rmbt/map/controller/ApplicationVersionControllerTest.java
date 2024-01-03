package at.rtr.rmbt.map.controller;

import at.rtr.rmbt.map.TestUtils;
import at.rtr.rmbt.map.constant.URIConstants;
import at.rtr.rmbt.map.dto.ApplicationVersionResponse;
import at.rtr.rmbt.map.service.ApplicationVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@ExtendWith(SpringExtension.class)
class ApplicationVersionControllerTest {
    private MockMvc mockMvc;

    @MockBean
    private ApplicationVersionService applicationVersionService;

    @BeforeEach
    void setUp() throws Exception {
        ApplicationVersionController applicationVersionController = new ApplicationVersionController(applicationVersionService);
        mockMvc = MockMvcBuilders.standaloneSetup(applicationVersionController)
                .build();
    }

    @Test
    void testGetApplicationVersion() throws Exception {
        ApplicationVersionResponse response = new ApplicationVersionResponse("1","a","b");
        when(applicationVersionService.getApplicationVersion()).thenReturn(response);
        mockMvc.perform(MockMvcRequestBuilders.get(URIConstants.VERSION))
                .andDo(print())
                .andExpect(content().json(TestUtils.asJsonString(response)));
    }
}