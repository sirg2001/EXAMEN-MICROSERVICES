package com.realestate.properties.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Zip code is required")
    @Positive(message = "Zip code must be positive")
    private Integer zipCode;

    @NotNull(message = "Area is required")
    @Positive(message = "Area must be positive")
    private Double area;

    @NotNull(message = "Number of bedrooms is required")
    @PositiveOrZero(message = "Bedrooms must be 0 or positive")
    private Integer bedrooms;

    @NotNull(message = "Number of bathrooms is required")
    @PositiveOrZero(message = "Bathrooms must be 0 or positive")
    private Integer bathrooms;

    @NotNull(message = "Price per night is required")
    @Positive(message = "Price must be positive")
    private BigDecimal pricePerNight;

    private Boolean available;

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotBlank(message = "Owner email is required")
    @Email(message = "Owner email must be valid")
    private String ownerEmail;

    @NotBlank(message = "Owner phone is required")
    private String ownerPhone;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
