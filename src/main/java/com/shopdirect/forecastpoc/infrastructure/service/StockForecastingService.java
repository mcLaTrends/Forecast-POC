package com.shopdirect.forecastpoc.infrastructure.service;

import com.shopdirect.forecastpoc.infrastructure.dao.StockDao;
import com.shopdirect.forecastpoc.infrastructure.model.ForecastingModelResult;
import com.shopdirect.forecastpoc.infrastructure.model.ForecastingResult;
import com.shopdirect.forecastpoc.infrastructure.model.ProductStockData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

@Component
public class StockForecastingService {

    private final StockDao stockDao;

    @Autowired
    public StockForecastingService(StockDao stockDao) {
        this.stockDao = stockDao;
    }

    public ForecastingResult getForecastings(int numWeeks){
        List<ProductStockData> fullProductStockData = stockDao.getProductStockData().stream()
                .sorted(Comparator.comparing(ProductStockData::getDate)).collect(Collectors.toList());

        List<ForecastingModelResult> forecastings = calculatePastForecastings(fullProductStockData);
        List<LocalDate> nextDates = getNextWeekDates(numWeeks, fullProductStockData.get(fullProductStockData.size() - 1))
                .collect(Collectors.toList());
        Stream<ProductStockData> naivePredictions = StockForecastingModels.naivePrediction(fullProductStockData.stream(),
                nextDates.stream());
        int indexNaive = forecastings.get(0).getName().equals("naive") ? 0 : 1;
        forecastings.get(indexNaive).getForecastedValues().addAll(naivePredictions.collect(Collectors.toList()));

        Stream<ProductStockData> averagePredictions = StockForecastingModels.averagePrediction(fullProductStockData.stream(),
                nextDates.stream());
        forecastings.get(Math.abs(indexNaive - 1)).getForecastedValues().addAll(averagePredictions.collect(Collectors.toList()));

        return new ForecastingResult(forecastings, fullProductStockData);
    }

    /*
    -should get past forecasting values from a db in the future
    -For now, it recalculates the whole forecasting history
    */
    private List<List<ProductStockData>> getPastForecastings(List<ProductStockData> fullProductStockData){
        Stream<ProductStockData> naiveForecastings = Stream.of();
        Stream<ProductStockData> averageForecastings = Stream.of();
        Stream<ProductStockData> naiveForecasting, averageForecasting;
        for(int i = 1; i < fullProductStockData.size(); i++){
            LocalDate date = fullProductStockData.get(i).getDate();
            naiveForecasting = StockForecastingModels.naivePrediction(fullProductStockData.stream().limit(i),
                    Stream.of(date));
            naiveForecastings = Stream.concat(naiveForecastings, naiveForecasting);

            averageForecasting = StockForecastingModels.averagePrediction(fullProductStockData.stream().limit(i),
                    Stream.of(date));
            averageForecastings = Stream.concat(averageForecastings, averageForecasting);

        }
        return Arrays.asList(naiveForecastings.collect(Collectors.toList()),
                averageForecastings.collect(Collectors.toList()));
    }

    public List<ForecastingModelResult> calculatePastForecastings(List<ProductStockData> fullProductStockData){
        Map<LocalDate, ProductStockData> actualValues = fullProductStockData.stream().collect(toMap(ProductStockData::getDate, prod -> prod));

        List<List<ProductStockData>> pastForecastings = getPastForecastings(fullProductStockData);
        List<ProductStockData> naiveForecastings = pastForecastings.get(0);
        List<ProductStockData> averageForecastings = pastForecastings.get(1);
        Double naiveError = calculateError(naiveForecastings, actualValues);
        Double averageError = calculateError(averageForecastings, actualValues);

        return Stream.of(new ForecastingModelResult(naiveForecastings, naiveError, "naive"),
                new ForecastingModelResult(averageForecastings, averageError, "average"))
                .sorted(Comparator.comparing(ForecastingModelResult::getError)).collect(Collectors.toList());
    }

    private static Stream<LocalDate> getNextWeekDates(int numberWeeks, ProductStockData lastStockData){
        return Stream
                .iterate(lastStockData.getDate().plusDays(7),
                        localDate -> localDate.plusDays(7))
                .limit(numberWeeks);
    }

    private Double calculateError(List<ProductStockData> forecastedValues, Map<LocalDate, ProductStockData> actualValues){
        OptionalDouble error = forecastedValues.stream().map(prod ->
                ((double) Math.abs(prod.getStockValue() - actualValues.get(prod.getDate()).getStockValue()))
                            / (1 + actualValues.get(prod.getDate()).getStockValue()))
                .mapToDouble(elem -> elem)
                .average();

        return error.isPresent() ? 100 * error.getAsDouble() : null;
    }
}
