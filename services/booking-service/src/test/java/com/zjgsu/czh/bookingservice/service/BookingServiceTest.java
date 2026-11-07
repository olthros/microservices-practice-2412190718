package com.zjgsu.czh.bookingservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    @Test
    void bookCourse_shouldReturn503_whenUserServiceInstanceListIsEmpty() {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances("user-service")).thenReturn(Collections.emptyList());
        RestTemplate restTemplate = mock(RestTemplate.class);

        BookingService service = new BookingService();
        ReflectionTestUtils.setField(service, "discoveryClient", discoveryClient);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.bookCourse(1L, 1L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        verify(discoveryClient, times(1)).getInstances("user-service");

        verifyNoInteractions(restTemplate);
    }
}