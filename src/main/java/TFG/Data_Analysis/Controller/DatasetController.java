package TFG.Data_Analysis.Controller;

import TFG.Data_Analysis.Controller.Dto.DatasetDto;
import TFG.Data_Analysis.Service.DatasetService;
import TFG.Data_Analysis.Service.Model.DatasetModel;
import jakarta.servlet.http.HttpServletResponse;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
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
    public DatasetDto applyFilter(@RequestBody List<String> filter, @PathVariable("datasetId") long datasetId) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        return modelMapper.map(datasetService.applyFilter(filter, datasetId), DatasetDto.class);
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
    public List<DatasetDto> getHistory(@PathVariable("userId") long userId) throws Exception {
        ModelMapper modelMapper = new ModelMapper();
        List<DatasetDto> history = datasetService.getHistory(userId).stream()
                .map(elementB -> modelMapper.map(elementB, DatasetDto.class))
                .collect(Collectors.toList());

        return history;
    }

    @GetMapping(path = "/filter/datasetId/{datasetId}/improve/{improve}/type/{type}/numRows/{numRows}")
    public DatasetDto applySampleFilter(@PathVariable("datasetId") long datasetId, @PathVariable("improve") String improve, @PathVariable("type") String type, @PathVariable("numRows") int numExtraRows) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        return  modelMapper.map(datasetService.applySampleFilter(datasetId, improve, type, numExtraRows), DatasetDto.class);
    }
    //endregion
    
    //region Delete Methods
    @DeleteMapping(path = "/datasetId/{datasetId}")
    public void deleteDataset(@PathVariable("datasetId") long datasetId) throws Exception {
        datasetService.deleteDataset(datasetId);
    }
    //endregion
}
