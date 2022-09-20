package com.synyctiks.car.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.synyctiks.car.IntegrationTest;
import com.synyctiks.car.domain.Car;
import com.synyctiks.car.repository.CarRepository;
import com.synyctiks.car.service.criteria.CarCriteria;
import com.synyctiks.car.service.dto.CarDTO;
import com.synyctiks.car.service.mapper.CarMapper;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link CarResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class CarResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_COLOUR = "AAAAAAAAAA";
    private static final String UPDATED_COLOUR = "BBBBBBBBBB";

    private static final Integer DEFAULT_PRICE = 1;
    private static final Integer UPDATED_PRICE = 2;
    private static final Integer SMALLER_PRICE = 1 - 1;

    private static final String ENTITY_API_URL = "/api/cars";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CarMapper carMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCarMockMvc;

    private Car car;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Car createEntity(EntityManager em) {
        Car car = new Car().name(DEFAULT_NAME).colour(DEFAULT_COLOUR).price(DEFAULT_PRICE);
        return car;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Car createUpdatedEntity(EntityManager em) {
        Car car = new Car().name(UPDATED_NAME).colour(UPDATED_COLOUR).price(UPDATED_PRICE);
        return car;
    }

    @BeforeEach
    public void initTest() {
        car = createEntity(em);
    }

    @Test
    @Transactional
    void createCar() throws Exception {
        int databaseSizeBeforeCreate = carRepository.findAll().size();
        // Create the Car
        CarDTO carDTO = carMapper.toDto(car);
        restCarMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(carDTO)))
            .andExpect(status().isCreated());

        // Validate the Car in the database
        List<Car> carList = carRepository.findAll();
        assertThat(carList).hasSize(databaseSizeBeforeCreate + 1);
        Car testCar = carList.get(carList.size() - 1);
        assertThat(testCar.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testCar.getColour()).isEqualTo(DEFAULT_COLOUR);
        assertThat(testCar.getPrice()).isEqualTo(DEFAULT_PRICE);
    }

    @Test
    @Transactional
    void createCarWithExistingId() throws Exception {
        // Create the Car with an existing ID
        car.setId(1L);
        CarDTO carDTO = carMapper.toDto(car);

        int databaseSizeBeforeCreate = carRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCarMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(carDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Car in the database
        List<Car> carList = carRepository.findAll();
        assertThat(carList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllCars() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList
        restCarMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(car.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].colour").value(hasItem(DEFAULT_COLOUR)))
            .andExpect(jsonPath("$.[*].price").value(hasItem(DEFAULT_PRICE)));
    }

    @Test
    @Transactional
    void getCar() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get the car
        restCarMockMvc
            .perform(get(ENTITY_API_URL_ID, car.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(car.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.colour").value(DEFAULT_COLOUR))
            .andExpect(jsonPath("$.price").value(DEFAULT_PRICE));
    }

    @Test
    @Transactional
    void getCarsByIdFiltering() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        Long id = car.getId();

        defaultCarShouldBeFound("id.equals=" + id);
        defaultCarShouldNotBeFound("id.notEquals=" + id);

        defaultCarShouldBeFound("id.greaterThanOrEqual=" + id);
        defaultCarShouldNotBeFound("id.greaterThan=" + id);

        defaultCarShouldBeFound("id.lessThanOrEqual=" + id);
        defaultCarShouldNotBeFound("id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllCarsByNameIsEqualToSomething() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where name equals to DEFAULT_NAME
        defaultCarShouldBeFound("name.equals=" + DEFAULT_NAME);

        // Get all the carList where name equals to UPDATED_NAME
        defaultCarShouldNotBeFound("name.equals=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllCarsByNameIsInShouldWork() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where name in DEFAULT_NAME or UPDATED_NAME
        defaultCarShouldBeFound("name.in=" + DEFAULT_NAME + "," + UPDATED_NAME);

        // Get all the carList where name equals to UPDATED_NAME
        defaultCarShouldNotBeFound("name.in=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllCarsByNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where name is not null
        defaultCarShouldBeFound("name.specified=true");

        // Get all the carList where name is null
        defaultCarShouldNotBeFound("name.specified=false");
    }

    @Test
    @Transactional
    void getAllCarsByNameContainsSomething() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where name contains DEFAULT_NAME
        defaultCarShouldBeFound("name.contains=" + DEFAULT_NAME);

        // Get all the carList where name contains UPDATED_NAME
        defaultCarShouldNotBeFound("name.contains=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllCarsByNameNotContainsSomething() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where name does not contain DEFAULT_NAME
        defaultCarShouldNotBeFound("name.doesNotContain=" + DEFAULT_NAME);

        // Get all the carList where name does not contain UPDATED_NAME
        defaultCarShouldBeFound("name.doesNotContain=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllCarsByColourIsEqualToSomething() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where colour equals to DEFAULT_COLOUR
        defaultCarShouldBeFound("colour.equals=" + DEFAULT_COLOUR);

        // Get all the carList where colour equals to UPDATED_COLOUR
        defaultCarShouldNotBeFound("colour.equals=" + UPDATED_COLOUR);
    }

    @Test
    @Transactional
    void getAllCarsByColourIsInShouldWork() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where colour in DEFAULT_COLOUR or UPDATED_COLOUR
        defaultCarShouldBeFound("colour.in=" + DEFAULT_COLOUR + "," + UPDATED_COLOUR);

        // Get all the carList where colour equals to UPDATED_COLOUR
        defaultCarShouldNotBeFound("colour.in=" + UPDATED_COLOUR);
    }

    @Test
    @Transactional
    void getAllCarsByColourIsNullOrNotNull() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where colour is not null
        defaultCarShouldBeFound("colour.specified=true");

        // Get all the carList where colour is null
        defaultCarShouldNotBeFound("colour.specified=false");
    }

    @Test
    @Transactional
    void getAllCarsByColourContainsSomething() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where colour contains DEFAULT_COLOUR
        defaultCarShouldBeFound("colour.contains=" + DEFAULT_COLOUR);

        // Get all the carList where colour contains UPDATED_COLOUR
        defaultCarShouldNotBeFound("colour.contains=" + UPDATED_COLOUR);
    }

    @Test
    @Transactional
    void getAllCarsByColourNotContainsSomething() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where colour does not contain DEFAULT_COLOUR
        defaultCarShouldNotBeFound("colour.doesNotContain=" + DEFAULT_COLOUR);

        // Get all the carList where colour does not contain UPDATED_COLOUR
        defaultCarShouldBeFound("colour.doesNotContain=" + UPDATED_COLOUR);
    }

    @Test
    @Transactional
    void getAllCarsByPriceIsEqualToSomething() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where price equals to DEFAULT_PRICE
        defaultCarShouldBeFound("price.equals=" + DEFAULT_PRICE);

        // Get all the carList where price equals to UPDATED_PRICE
        defaultCarShouldNotBeFound("price.equals=" + UPDATED_PRICE);
    }

    @Test
    @Transactional
    void getAllCarsByPriceIsInShouldWork() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where price in DEFAULT_PRICE or UPDATED_PRICE
        defaultCarShouldBeFound("price.in=" + DEFAULT_PRICE + "," + UPDATED_PRICE);

        // Get all the carList where price equals to UPDATED_PRICE
        defaultCarShouldNotBeFound("price.in=" + UPDATED_PRICE);
    }

    @Test
    @Transactional
    void getAllCarsByPriceIsNullOrNotNull() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where price is not null
        defaultCarShouldBeFound("price.specified=true");

        // Get all the carList where price is null
        defaultCarShouldNotBeFound("price.specified=false");
    }

    @Test
    @Transactional
    void getAllCarsByPriceIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where price is greater than or equal to DEFAULT_PRICE
        defaultCarShouldBeFound("price.greaterThanOrEqual=" + DEFAULT_PRICE);

        // Get all the carList where price is greater than or equal to UPDATED_PRICE
        defaultCarShouldNotBeFound("price.greaterThanOrEqual=" + UPDATED_PRICE);
    }

    @Test
    @Transactional
    void getAllCarsByPriceIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where price is less than or equal to DEFAULT_PRICE
        defaultCarShouldBeFound("price.lessThanOrEqual=" + DEFAULT_PRICE);

        // Get all the carList where price is less than or equal to SMALLER_PRICE
        defaultCarShouldNotBeFound("price.lessThanOrEqual=" + SMALLER_PRICE);
    }

    @Test
    @Transactional
    void getAllCarsByPriceIsLessThanSomething() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where price is less than DEFAULT_PRICE
        defaultCarShouldNotBeFound("price.lessThan=" + DEFAULT_PRICE);

        // Get all the carList where price is less than UPDATED_PRICE
        defaultCarShouldBeFound("price.lessThan=" + UPDATED_PRICE);
    }

    @Test
    @Transactional
    void getAllCarsByPriceIsGreaterThanSomething() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        // Get all the carList where price is greater than DEFAULT_PRICE
        defaultCarShouldNotBeFound("price.greaterThan=" + DEFAULT_PRICE);

        // Get all the carList where price is greater than SMALLER_PRICE
        defaultCarShouldBeFound("price.greaterThan=" + SMALLER_PRICE);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultCarShouldBeFound(String filter) throws Exception {
        restCarMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(car.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].colour").value(hasItem(DEFAULT_COLOUR)))
            .andExpect(jsonPath("$.[*].price").value(hasItem(DEFAULT_PRICE)));

        // Check, that the count call also returns 1
        restCarMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultCarShouldNotBeFound(String filter) throws Exception {
        restCarMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCarMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingCar() throws Exception {
        // Get the car
        restCarMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCar() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        int databaseSizeBeforeUpdate = carRepository.findAll().size();

        // Update the car
        Car updatedCar = carRepository.findById(car.getId()).get();
        // Disconnect from session so that the updates on updatedCar are not directly saved in db
        em.detach(updatedCar);
        updatedCar.name(UPDATED_NAME).colour(UPDATED_COLOUR).price(UPDATED_PRICE);
        CarDTO carDTO = carMapper.toDto(updatedCar);

        restCarMockMvc
            .perform(
                put(ENTITY_API_URL_ID, carDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(carDTO))
            )
            .andExpect(status().isOk());

        // Validate the Car in the database
        List<Car> carList = carRepository.findAll();
        assertThat(carList).hasSize(databaseSizeBeforeUpdate);
        Car testCar = carList.get(carList.size() - 1);
        assertThat(testCar.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testCar.getColour()).isEqualTo(UPDATED_COLOUR);
        assertThat(testCar.getPrice()).isEqualTo(UPDATED_PRICE);
    }

    @Test
    @Transactional
    void putNonExistingCar() throws Exception {
        int databaseSizeBeforeUpdate = carRepository.findAll().size();
        car.setId(count.incrementAndGet());

        // Create the Car
        CarDTO carDTO = carMapper.toDto(car);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCarMockMvc
            .perform(
                put(ENTITY_API_URL_ID, carDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(carDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Car in the database
        List<Car> carList = carRepository.findAll();
        assertThat(carList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCar() throws Exception {
        int databaseSizeBeforeUpdate = carRepository.findAll().size();
        car.setId(count.incrementAndGet());

        // Create the Car
        CarDTO carDTO = carMapper.toDto(car);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCarMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(carDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Car in the database
        List<Car> carList = carRepository.findAll();
        assertThat(carList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCar() throws Exception {
        int databaseSizeBeforeUpdate = carRepository.findAll().size();
        car.setId(count.incrementAndGet());

        // Create the Car
        CarDTO carDTO = carMapper.toDto(car);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCarMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(carDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Car in the database
        List<Car> carList = carRepository.findAll();
        assertThat(carList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCarWithPatch() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        int databaseSizeBeforeUpdate = carRepository.findAll().size();

        // Update the car using partial update
        Car partialUpdatedCar = new Car();
        partialUpdatedCar.setId(car.getId());

        partialUpdatedCar.name(UPDATED_NAME).colour(UPDATED_COLOUR).price(UPDATED_PRICE);

        restCarMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCar.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedCar))
            )
            .andExpect(status().isOk());

        // Validate the Car in the database
        List<Car> carList = carRepository.findAll();
        assertThat(carList).hasSize(databaseSizeBeforeUpdate);
        Car testCar = carList.get(carList.size() - 1);
        assertThat(testCar.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testCar.getColour()).isEqualTo(UPDATED_COLOUR);
        assertThat(testCar.getPrice()).isEqualTo(UPDATED_PRICE);
    }

    @Test
    @Transactional
    void fullUpdateCarWithPatch() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        int databaseSizeBeforeUpdate = carRepository.findAll().size();

        // Update the car using partial update
        Car partialUpdatedCar = new Car();
        partialUpdatedCar.setId(car.getId());

        partialUpdatedCar.name(UPDATED_NAME).colour(UPDATED_COLOUR).price(UPDATED_PRICE);

        restCarMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCar.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedCar))
            )
            .andExpect(status().isOk());

        // Validate the Car in the database
        List<Car> carList = carRepository.findAll();
        assertThat(carList).hasSize(databaseSizeBeforeUpdate);
        Car testCar = carList.get(carList.size() - 1);
        assertThat(testCar.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testCar.getColour()).isEqualTo(UPDATED_COLOUR);
        assertThat(testCar.getPrice()).isEqualTo(UPDATED_PRICE);
    }

    @Test
    @Transactional
    void patchNonExistingCar() throws Exception {
        int databaseSizeBeforeUpdate = carRepository.findAll().size();
        car.setId(count.incrementAndGet());

        // Create the Car
        CarDTO carDTO = carMapper.toDto(car);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCarMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, carDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(carDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Car in the database
        List<Car> carList = carRepository.findAll();
        assertThat(carList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCar() throws Exception {
        int databaseSizeBeforeUpdate = carRepository.findAll().size();
        car.setId(count.incrementAndGet());

        // Create the Car
        CarDTO carDTO = carMapper.toDto(car);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCarMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(carDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Car in the database
        List<Car> carList = carRepository.findAll();
        assertThat(carList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCar() throws Exception {
        int databaseSizeBeforeUpdate = carRepository.findAll().size();
        car.setId(count.incrementAndGet());

        // Create the Car
        CarDTO carDTO = carMapper.toDto(car);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCarMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(carDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Car in the database
        List<Car> carList = carRepository.findAll();
        assertThat(carList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCar() throws Exception {
        // Initialize the database
        carRepository.saveAndFlush(car);

        int databaseSizeBeforeDelete = carRepository.findAll().size();

        // Delete the car
        restCarMockMvc.perform(delete(ENTITY_API_URL_ID, car.getId()).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Car> carList = carRepository.findAll();
        assertThat(carList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
