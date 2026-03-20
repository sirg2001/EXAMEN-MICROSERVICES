package com.realestate.properties.service;

import com.realestate.properties.dto.PropertyDTO;
import com.realestate.properties.entity.Property;
import com.realestate.properties.exception.PropertyNotFoundException;
import com.realestate.properties.repository.PropertyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    /**
     * Récupère une propriété par ID avec caching
     */
    @Cacheable(value = "properties", key = "#id")
    public PropertyDTO getPropertyById(Long id) {
        log.info("Fetching property from database: {}", id);

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Property not found: {}", id);
                    return new PropertyNotFoundException("Property with id " + id + " not found");
                });

        return mapToDTO(property);
    }

    /**
     * Récupère une propriété disponible par ID
     * Utilisé par MS-Bookings pour vérifier la disponibilité
     */
    public PropertyDTO getAvailableProperty(Long id) {
        log.info("Checking availability for property: {}", id);

        Property property = propertyRepository.findByIdAndAvailableTrue(id)
                .orElseThrow(() -> {
                    log.warn("Available property not found: {}", id);
                    return new PropertyNotFoundException("Property with id " + id + " is not available");
                });

        return mapToDTO(property);
    }

    /**
     * Récupère toutes les propriétés disponibles
     */
    public List<PropertyDTO> getAllAvailableProperties() {
        log.info("Fetching all available properties");

        List<Property> properties = propertyRepository.findByAvailableTrue();

        return properties.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les propriétés disponibles dans une ville
     */
    public List<PropertyDTO> getAvailablePropertiesByCity(String city) {
        log.info("Fetching available properties in city: {}", city);

        List<Property> properties = propertyRepository.findByCityAndAvailableTrue(city);

        return properties.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Crée une nouvelle propriété
     */
    @CacheEvict(value = "properties", allEntries = true)
    public PropertyDTO createProperty(PropertyDTO dto) {
        log.info("Creating new property: {}", dto.getTitle());

        Property property = Property.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .address(dto.getAddress())
                .city(dto.getCity())
                .zipCode(dto.getZipCode())
                .area(dto.getArea())
                .bedrooms(dto.getBedrooms())
                .bathrooms(dto.getBathrooms())
                .pricePerNight(dto.getPricePerNight())
                .available(true)
                .ownerName(dto.getOwnerName())
                .ownerEmail(dto.getOwnerEmail())
                .ownerPhone(dto.getOwnerPhone())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Property saved = propertyRepository.save(property);
        log.info("Property created successfully with id: {}", saved.getId());

        return mapToDTO(saved);
    }

    /**
     * Met à jour une propriété existante
     */
    @CacheEvict(value = "properties", key = "#id")
    public PropertyDTO updateProperty(Long id, PropertyDTO dto) {
        log.info("Updating property: {}", id);

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Property not found for update: {}", id);
                    return new PropertyNotFoundException("Property with id " + id + " not found");
                });

        property.setTitle(dto.getTitle());
        property.setDescription(dto.getDescription());
        property.setAddress(dto.getAddress());
        property.setCity(dto.getCity());
        property.setZipCode(dto.getZipCode());
        property.setArea(dto.getArea());
        property.setBedrooms(dto.getBedrooms());
        property.setBathrooms(dto.getBathrooms());
        property.setPricePerNight(dto.getPricePerNight());
        property.setAvailable(dto.getAvailable() != null ? dto.getAvailable() : true);
        property.setOwnerName(dto.getOwnerName());
        property.setOwnerEmail(dto.getOwnerEmail());
        property.setOwnerPhone(dto.getOwnerPhone());

        Property updated = propertyRepository.save(property);
        log.info("Property updated successfully: {}", id);

        return mapToDTO(updated);
    }

    /**
     * Supprime une propriété
     */
    @CacheEvict(value = "properties", key = "#id")
    public void deleteProperty(Long id) {
        log.info("Deleting property: {}", id);

        if (!propertyRepository.existsById(id)) {
            log.warn("Property not found for deletion: {}", id);
            throw new PropertyNotFoundException("Property with id " + id + " not found");
        }

        propertyRepository.deleteById(id);
        log.info("Property deleted successfully: {}", id);
    }

    /**
     * Change la disponibilité d'une propriété
     */
    @CacheEvict(value = "properties", key = "#id")
    public PropertyDTO toggleAvailability(Long id, Boolean available) {
        log.info("Toggling availability for property: {} to {}", id, available);

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException("Property with id " + id + " not found"));

        property.setAvailable(available);
        Property updated = propertyRepository.save(property);

        log.info("Property availability updated: {}", id);

        return mapToDTO(updated);
    }

    /**
     * Convertir Entity vers DTO
     */
    private PropertyDTO mapToDTO(Property property) {
        return PropertyDTO.builder()
                .id(property.getId())
                .title(property.getTitle())
                .description(property.getDescription())
                .address(property.getAddress())
                .city(property.getCity())
                .zipCode(property.getZipCode())
                .area(property.getArea())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .pricePerNight(property.getPricePerNight())
                .available(property.getAvailable())
                .ownerName(property.getOwnerName())
                .ownerEmail(property.getOwnerEmail())
                .ownerPhone(property.getOwnerPhone())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }
}
