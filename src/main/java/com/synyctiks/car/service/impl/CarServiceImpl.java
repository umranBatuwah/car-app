package com.synyctiks.car.service.impl;

import com.synyctiks.car.domain.Car;
import com.synyctiks.car.repository.CarRepository;
import com.synyctiks.car.service.CarService;
import com.synyctiks.car.service.dto.CarDTO;
import com.synyctiks.car.service.mapper.CarMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link Car}.
 */
@Service
@Transactional
public class CarServiceImpl implements CarService {

    private final Logger log = LoggerFactory.getLogger(CarServiceImpl.class);

    private final CarRepository carRepository;

    private final CarMapper carMapper;

    public CarServiceImpl(CarRepository carRepository, CarMapper carMapper) {
        this.carRepository = carRepository;
        this.carMapper = carMapper;
    }

    @Override
    public CarDTO save(CarDTO carDTO) {
        log.debug("Request to save Car : {}", carDTO);
        Car car = carMapper.toEntity(carDTO);
        car = carRepository.save(car);
        return carMapper.toDto(car);
    }

    @Override
    public CarDTO update(CarDTO carDTO) {
        log.debug("Request to update Car : {}", carDTO);
        Car car = carMapper.toEntity(carDTO);
        car = carRepository.save(car);
        return carMapper.toDto(car);
    }

    @Override
    public Optional<CarDTO> partialUpdate(CarDTO carDTO) {
        log.debug("Request to partially update Car : {}", carDTO);

        return carRepository
            .findById(carDTO.getId())
            .map(existingCar -> {
                carMapper.partialUpdate(existingCar, carDTO);

                return existingCar;
            })
            .map(carRepository::save)
            .map(carMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarDTO> findAll() {
        log.debug("Request to get all Cars");
        return carRepository.findAll().stream().map(carMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarDTO> findOne(Long id) {
        log.debug("Request to get Car : {}", id);
        return carRepository.findById(id).map(carMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete Car : {}", id);
        carRepository.deleteById(id);
    }
}
