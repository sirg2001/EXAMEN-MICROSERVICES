package com.realestate.bookings.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client pour appeler MS-Properties en synchrone
 * Utilisé pour vérifier la disponibilité d'une propriété
 */
@Component
@Slf4j
public class PropertyServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ms.properties.url:http://localhost:8081/api}")
    private String propertiesServiceUrl;

    /**
     * Vérifie qu'une propriété existe et est disponible
     */
    public PropertyInfo getAvailableProperty(Long propertyId) {
        try {
            log.info("Checking availability of property: {} from MS-Properties", propertyId);

            String url = propertiesServiceUrl + "/properties/" + propertyId + "/available";

            PropertyInfo property = restTemplate.getForObject(url, PropertyInfo.class);

            if (property == null) {
                log.warn("Property not found or not available: {}", propertyId);
                throw new PropertyNotAvailableException("Property " + propertyId + " is not available");
            }

            log.info("Property is available: {}", propertyId);
            return property;

        } catch (RestClientException e) {
            log.error("Failed to call MS-Properties for property: {}", propertyId, e);
            throw new PropertyServiceUnavailableException(
                    "Failed to verify property availability. Property service is unavailable", e);
        } catch (Exception e) {
            log.error("Unexpected error while checking property availability: {}", propertyId, e);
            throw new RuntimeException("Failed to check property availability", e);
        }
    }

    /**
     * DTO pour la réponse de MS-Properties
     */
    public static class PropertyInfo {
        public Long id;
        public String title;
        public String description;
        public String address;
        public String city;
        public Double area;
        public Integer bedrooms;
        public Integer bathrooms;
        public java.math.BigDecimal pricePerNight;
        public Boolean available;
        public String ownerName;
        public String ownerEmail;
        public String ownerPhone;
    }
}

class PropertyNotAvailableException extends RuntimeException {
    public PropertyNotAvailableException(String message) {
        super(message);
    }
}

class PropertyServiceUnavailableException extends RuntimeException {
    public PropertyServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
