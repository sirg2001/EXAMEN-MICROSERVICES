package com.realestate.properties.controller;

import com.realestate.properties.dto.PropertyDTO;
import com.realestate.properties.service.PropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/properties")
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PropertyController {

    @Autowired
    private PropertyService propertyService;

    /**
     * Récupère une propriété par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PropertyDTO> getProperty(@PathVariable Long id) {
        log.info("GET /api/properties/{}", id);
        PropertyDTO property = propertyService.getPropertyById(id);
        return ResponseEntity.ok(property);
    }

    /**
     * Récupère une propriété disponible (utilisé par MS-Bookings)
     */
    @GetMapping("/{id}/available")
    public ResponseEntity<PropertyDTO> getAvailableProperty(@PathVariable Long id) {
        log.info("GET /api/properties/{}/available", id);
        PropertyDTO property = propertyService.getAvailableProperty(id);
        return ResponseEntity.ok(property);
    }

    /**
     * Récupère toutes les propriétés disponibles
     */
    @GetMapping("/available")
    public ResponseEntity<List<PropertyDTO>> getAllAvailableProperties() {
        log.info("GET /api/properties/available");
        List<PropertyDTO> properties = propertyService.getAllAvailableProperties();
        return ResponseEntity.ok(properties);
    }

    /**
     * Récupère les propriétés disponibles dans une ville
     */
    @GetMapping("/city/{city}/available")
    public ResponseEntity<List<PropertyDTO>> getAvailablePropertiesByCity(@PathVariable String city) {
        log.info("GET /api/properties/city/{}/available", city);
        List<PropertyDTO> properties = propertyService.getAvailablePropertiesByCity(city);
        return ResponseEntity.ok(properties);
    }

    /**
     * Crée une nouvelle propriété
     */
    @PostMapping
    public ResponseEntity<PropertyDTO> createProperty(@Valid @RequestBody PropertyDTO propertyDTO) {
        log.info("POST /api/properties - Creating new property: {}", propertyDTO.getTitle());
        PropertyDTO created = propertyService.createProperty(propertyDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour une propriété
     */
    @PutMapping("/{id}")
    public ResponseEntity<PropertyDTO> updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody PropertyDTO propertyDTO) {
        log.info("PUT /api/properties/{} - Updating property", id);
        PropertyDTO updated = propertyService.updateProperty(id, propertyDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprime une propriété
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(@PathVariable Long id) {
        log.info("DELETE /api/properties/{}", id);
        propertyService.deleteProperty(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Change la disponibilité d'une propriété
     */
    @PatchMapping("/{id}/availability")
    public ResponseEntity<PropertyDTO> toggleAvailability(
            @PathVariable Long id,
            @RequestParam Boolean available) {
        log.info("PATCH /api/properties/{}/availability - Setting to {}", id, available);
        PropertyDTO updated = propertyService.toggleAvailability(id, available);
        return ResponseEntity.ok(updated);
    }
}
