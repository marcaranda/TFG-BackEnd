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

    @PostMapping(path = "/filter/userId/{userId}/datasetName/{datasetName}/version/{version}")
    public DatasetDto applyFilter(@RequestBody List<String> filter,@PathVariable("userId") long userId, @PathVariable("datasetName") String datasetName, @PathVariable("version") Integer version) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        return modelMapper.map(datasetService.applyFilter(filter, userId, datasetName, version), DatasetDto.class);
    }
    //endregion

    //region Get Methods
    @GetMapping(path = "/userId/{userId}/datasetName/{datasetName}/version/{version}")
    public DatasetDto getDataset(@PathVariable("userId") Long userId, @PathVariable("datasetName") String datasetName, @PathVariable("version") Integer version) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        return modelMapper.map(datasetService.getDataset(userId, datasetName, version), DatasetDto.class);
    }

    @GetMapping(path = "/download/userId/{userId}/datasetName/{datasetName}/version/{version}")
    public void downloadFile(@PathVariable("userId") Long userId, @PathVariable("datasetName") String datasetName, @PathVariable("version") Integer downloadVersion, HttpServletResponse response) throws Exception {
        datasetService.downloadFile(userId, datasetName, downloadVersion, response);
    }

    @GetMapping(path = "/historial/userId/{userId}")
    public List<DatasetDto> getHistory(@PathVariable("userId") long userId) throws Exception {
        ModelMapper modelMapper = new ModelMapper();
        List<DatasetDto> history = datasetService.getHistory(userId).stream()
                .map(elementB -> modelMapper.map(elementB, DatasetDto.class))
                .collect(Collectors.toList());

        return history;
    }

    @GetMapping(path = "/filter/userId/{userId}/datasetName/{datasetName}/version/{version}/improve/{improve}/type/{type}")
    public DatasetDto applySampleFilter(@PathVariable("userId") long userId, @PathVariable("datasetName") String datasetName, @PathVariable("version") Integer version, @PathVariable("improve") String improve, @PathVariable("type") String type) throws Exception {
        ModelMapper modelMapper = new ModelMapper();

        return  modelMapper.map(datasetService.applySampleFilter(userId, datasetName, version, improve, type), DatasetDto.class);
    }
    //endregion
    
    //region Delete Methods
    @DeleteMapping(path = "/userId/{userId}/datasetName/{datasetName}/version/{version}")
    public void deleteDataset(@PathVariable("userId") Long userId, @PathVariable("datasetName") String datasetName, @PathVariable("version") Integer version) throws Exception {
        datasetService.deleteDataset(userId, datasetName, version);
    }
    //endregion
}
