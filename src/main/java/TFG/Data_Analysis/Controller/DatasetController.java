package TFG.Data_Analysis.Controller;

import TFG.Data_Analysis.Controller.Dto.DatasetDto;
import TFG.Data_Analysis.Helpers.FilterListDto;
import TFG.Data_Analysis.Service.DatasetService;
import TFG.Data_Analysis.Service.Model.DatasetModel;
import jakarta.servlet.http.HttpServletResponse;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/file")
public class DatasetController {
    @Autowired
    DatasetService datasetService;

    //region Post Methods
    @PostMapping(path = "userId/{userId}")
    public DatasetDto fileReaderCSV(@RequestBody MultipartFile file, @PathVariable("userId") long userId) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        return modelMapper.map(datasetService.fileReader(file, userId), DatasetDto.class);
    }

    @PostMapping(path = "/filter/datasetId/{datasetId}")
    public DatasetDto applyFilter(@RequestBody FilterListDto filterListDto, @PathVariable("datasetId") long datasetId) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        return modelMapper.map(datasetService.applyFilter(filterListDto.getTitlesFilter(), filterListDto.getRowsWanted(), datasetId), DatasetDto.class);
    }
    //endregion

    //region Get Methods
    @GetMapping(path = "/datasetId/{datasetId}")
    public DatasetDto getDataset(@PathVariable("datasetId") long datasetId) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        return modelMapper.map(datasetService.getDataset(datasetId), DatasetDto.class);
    }

    @GetMapping(path = "/download/datasetId/{datasetId}")
    public void downloadFile(@PathVariable("datasetId") long datasetId, HttpServletResponse response) throws Exception {
        datasetService.downloadFile(datasetId, response);
    }

    @GetMapping(path = "/historial/userId/{userId}")
    public Map<String, List<DatasetDto>> getHistory(@PathVariable("userId") long userId, @RequestParam(required = false, value = "orderBy") String order,
                                                    @RequestParam(required = false, value = "search") String search,
                                                    @RequestParam(required = false, value = "datasetName") String datasetName) throws Exception {
        ModelMapper modelMapper = new ModelMapper();
        Map<String, List<DatasetDto>> history = datasetService.getHistory(userId, order, search, datasetName).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .map(model -> modelMapper.map(model, DatasetDto.class))
                    .collect(Collectors.toList())
            ));

        return history;
    }

    @PutMapping(path = "/filter/datasetId/{datasetId}/improve/{improve}/type/{type}")
    public DatasetDto applySampleFilter(@PathVariable("datasetId") long datasetId, @PathVariable("improve") String improve, @PathVariable("type") String type, @RequestParam(value = "numInitialRows") int numInitialRows, @RequestParam(value = "numWantedRows") int numWantedRows, @RequestParam(value = "sliderValue") double sliderValue, @RequestBody List<Boolean> initialRows) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        return  modelMapper.map(datasetService.applySampleFilter(datasetId, improve, type, numInitialRows, numWantedRows, sliderValue, initialRows), DatasetDto.class);
    }
    //endregion
    
    //region Delete Methods
    @DeleteMapping(path = "/datasetId/{datasetId}")
    public void deleteDataset(@PathVariable("datasetId") long datasetId) throws Exception {
        datasetService.deleteDataset(datasetId);
    }
    //endregion
}
