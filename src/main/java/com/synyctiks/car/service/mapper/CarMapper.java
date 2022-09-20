package com.synyctiks.car.service.mapper;

import com.synyctiks.car.domain.Car;
import com.synyctiks.car.service.dto.CarDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Car} and its DTO {@link CarDTO}.
 */
@Mapper(componentModel = "spring")
public interface CarMapper extends EntityMapper<CarDTO, Car> {}
