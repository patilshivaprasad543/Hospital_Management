package com.hospital.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectDownloadController.class)
class ProjectDownloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void eclipseZipDownloadsAsAttachment() throws Exception {
        mockMvc.perform(get("/download/eclipse"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Hospital_Management_Eclipse.zip\""));
    }
}
