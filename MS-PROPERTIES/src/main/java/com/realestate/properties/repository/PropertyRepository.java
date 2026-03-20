package com.realestate.properties.repository;

import com.realestate.properties.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByCity(String city);

    List<Property> findByAvailableTrue();

    List<Property> findByCityAndAvailableTrue(String city);

    Optional<Property> findByIdAndAvailableTrue(Long id);
}
