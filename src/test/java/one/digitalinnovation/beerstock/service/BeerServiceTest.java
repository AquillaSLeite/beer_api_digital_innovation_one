package one.digitalinnovation.beerstock.service;

import one.digitalinnovation.beerstock.builder.BeerDTOBuilder;
import one.digitalinnovation.beerstock.dto.BeerDTO;
import one.digitalinnovation.beerstock.entity.Beer;
import one.digitalinnovation.beerstock.exception.BeerAlreadyRegisteredException;
import one.digitalinnovation.beerstock.exception.BeerNotFoundException;
import one.digitalinnovation.beerstock.exception.BeerStockExceededException;
import one.digitalinnovation.beerstock.mapper.BeerMapper;
import one.digitalinnovation.beerstock.repository.BeerRepository;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BeerServiceTest {

    private static final long INVALID_BEER_ID = 1L;

    @Mock
    private BeerRepository beerRepository;

    private final BeerMapper beerMapper = BeerMapper.INSTANCE;

    @InjectMocks
    private BeerService beerService;

    @Test
    void whenNewBeerInformedThenShouldBeCreated() throws BeerAlreadyRegisteredException {
        //given
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(beerDTO);

        //when
        when(beerRepository.findByName(any())).thenReturn(Optional.empty());
        when(beerRepository.save(any())).thenReturn(expectedBeer);

        BeerDTO createdBeer = beerService.createBeer(beerDTO);

        //then
        assertThat(createdBeer.getId(), is(equalTo(beerDTO.getId())));
        assertThat(createdBeer.getBrand(), is(equalTo(beerDTO.getBrand())));
        assertThat(createdBeer.getMax(), is(equalTo(beerDTO.getMax())));
        assertThat(createdBeer.getName(), is(equalTo(beerDTO.getName())));
        assertThat(createdBeer.getQuantity(), is(equalTo(beerDTO.getQuantity())));
        assertThat(createdBeer.getType().toString(), is(equalTo(beerDTO.getType().toString())));
    }

    @Test
    void whenAlreadyRegisteredBeerInformedThenAnExceptionShouldBeThrown() {
        //given
        BeerDTO beerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer duplicatedBeer = beerMapper.toModel(beerDTO);

        //when
        when(beerRepository.findByName(any())).thenReturn(Optional.of(duplicatedBeer));

        //then
        assertThrows(BeerAlreadyRegisteredException.class, () -> beerService.createBeer(beerDTO));
    }

    @Test
    void whenValidBeerNameIsGivenThenReturnABeer() throws BeerNotFoundException {
        //given
        BeerDTO givenBeerDto = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer givenBeer = beerMapper.toModel(givenBeerDto);

        //when
        when(beerRepository.findByName(any())).thenReturn(Optional.of(givenBeer));

        BeerDTO serviceBeerDto = beerService.findByName(givenBeerDto.getName());

        //then
        assertThat(serviceBeerDto, is(equalTo(givenBeerDto)));
    }

    @Test
    void whenNotRegisteredBeerNameIsGivenThenThrowAnException(){
        //when
        when(beerRepository.findByName(any())).thenReturn(Optional.empty());

        assertThrows(BeerNotFoundException.class, () -> beerService.findByName(any()));
    }

    @Test
    void whenListBeerIsCalledThenReturnAListOfBeers(){
        //given
        BeerDTO givenBeerDto = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer givenBeer = beerMapper.toModel(givenBeerDto);

        //when
        when(beerRepository.findAll()).thenReturn(Collections.singletonList(givenBeer));

        List<BeerDTO> serviceListBeers = beerService.listAll();

        //then
        assertThat(serviceListBeers, is(not(empty())));
    }

    @Test
    void whenListBeerIsCalledThenReturnAnEmptyList(){
        //when
        when(beerRepository.findAll()).thenReturn(Collections.EMPTY_LIST);

        List<BeerDTO> serviceListBeers = beerService.listAll();

        //then
        assertThat(serviceListBeers, is(empty()));
    }

    @Test
    void whenExclusionIsCalledWithValidIdThenABeerShouldBeDeleted() throws BeerNotFoundException {
        //given
        BeerDTO givenBeerDto = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer givenBeer = beerMapper.toModel(givenBeerDto);

        //when
        when(beerRepository.findById(any())).thenReturn(Optional.of(givenBeer));
        doNothing().when(beerRepository).deleteById(any());

        beerService.deleteById(givenBeerDto.getId());

        //then
        verify(beerRepository, times(1)).findById(givenBeerDto.getId());
        verify(beerRepository, times(1)).deleteById(givenBeerDto.getId());
    }

    @Test
    void whenExclusionIsCalledWithInvalidIdThenExceptionShouldBeThrown() {
        //when
        when(beerRepository.findById(any())).thenReturn(Optional.empty());

        //then
        assertThrows(BeerNotFoundException.class, () -> beerService.deleteById(any()));
    }

    @Test
    void whenIncrementIsCalledThenIncrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        //given
        BeerDTO givenBeerDto = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer givenBeer = beerMapper.toModel(givenBeerDto);

        //when
        when(beerRepository.findById(any())).thenReturn(Optional.of(givenBeer));
        when(beerRepository.save(givenBeer)).thenReturn(givenBeer);

        int quantityToIncrement = 15;
        BeerDTO serviceIncrementBeer = beerService.increment(givenBeerDto.getId(), quantityToIncrement);
        int expectedQuantityAfterIncrement = givenBeerDto.getQuantity() + quantityToIncrement;

        //then
        assertThat(serviceIncrementBeer.getQuantity(), is(equalTo(expectedQuantityAfterIncrement)));
        assertThat(serviceIncrementBeer.getMax(), is(greaterThanOrEqualTo(expectedQuantityAfterIncrement)));
    }

    @Test
    void whenIncrementIsGreaterThanMaxThenThrowException(){
        //given
        BeerDTO givenBeerDto = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer givenBeer = beerMapper.toModel(givenBeerDto);

        //when
        when(beerRepository.findById(any())).thenReturn(Optional.of(givenBeer));

        int quantityToIncrement = 100;

        //then
        assertThrows(BeerStockExceededException.class, () -> beerService.increment(givenBeerDto.getId(), quantityToIncrement));
    }

    @Test
    void whenIncrementIsCalledWithInvalidIdThenThrowException(){
        //given
        int quantityToIncrement = 10;

        //when
        when(beerRepository.findById(any())).thenReturn(Optional.empty());

        //then
        assertThrows(BeerNotFoundException.class, () -> beerService.increment(INVALID_BEER_ID, quantityToIncrement));
    }


    @Test
    void whenDecrementIsCalledThenDecrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        //given
        BeerDTO givenBeerDto = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer givenBeer = beerMapper.toModel(givenBeerDto);

        //when
        when(beerRepository.findById(givenBeerDto.getId())).thenReturn(Optional.of(givenBeer));
        when(beerRepository.save(givenBeer)).thenReturn(givenBeer);

        int quantityToDecrement = 5;
        int expectedQuantityAfterDecrement = givenBeerDto.getQuantity() - quantityToDecrement;
        BeerDTO incrementedBeerDTO = beerService.decrement(givenBeerDto.getId(), quantityToDecrement);

        //then
        assertThat(incrementedBeerDTO.getQuantity(), is(equalTo(expectedQuantityAfterDecrement)));
        assertThat(expectedQuantityAfterDecrement, is(greaterThan(0)));
    }

    @Test
    void whenDecrementIsCalledToEmptyStockThenEmptyBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        //given
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);

        int quantityToDecrement = 10;
        int expectedQuantityAfterDecrement = expectedBeerDTO.getQuantity() - quantityToDecrement;
        BeerDTO incrementedBeerDTO = beerService.decrement(expectedBeerDTO.getId(), quantityToDecrement);

        //then
        assertThat(expectedQuantityAfterDecrement, is(equalTo(0)));
        assertThat(expectedQuantityAfterDecrement, is(equalTo(incrementedBeerDTO.getQuantity())));
    }

    @Test
    void whenDecrementIsLowerThanZeroThenThrowException() {
        //given
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));

        int quantityToDecrement = 80;

        //then
        assertThrows(BeerStockExceededException.class, () -> beerService.decrement(expectedBeerDTO.getId(), quantityToDecrement));
    }

    @Test
    void whenDecrementIsCalledWithInvalidIdThenThrowException() {
        //given
        int quantityToDecrement = 10;

        //when
        when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());

        //then
        assertThrows(BeerNotFoundException.class, () -> beerService.decrement(INVALID_BEER_ID, quantityToDecrement));
    }
}
