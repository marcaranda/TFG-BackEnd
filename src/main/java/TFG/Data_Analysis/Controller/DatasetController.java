package TFG.Data_Analysis.Controller;

import TFG.Data_Analysis.Controller.Dto.DatasetDto;
import TFG.Data_Analysis.Service.DatasetService;
import TFG.Data_Analysis.Service.Model.DatasetModel;
import jakarta.servlet.http.HttpServletResponse;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/file")
public class DatasetController {
    @Autowired
    DatasetService datasetService;

    //region Post Methods
    @PostMapping(path = "/{userId}")
    public double fileReaderCSV(@RequestBody String path, @PathVariable("userId") long userId) throws IOException {
        return datasetService.fileReader(path, userId);
    }

    @PostMapping(path = "/filter/{userId}/{datasetName}")
    public double applyFilter(@RequestBody List<String> filter,@PathVariable("userId") long userId, @PathVariable("datasetName") String datasetName) {
        return datasetService.applyFilter(filter, userId, datasetName);
    }
    //endregion

    //region Get Methods
    @GetMapping(path = "/{datasetName}/{version}")
    public DatasetDto getDataset(@PathVariable("datasetName") String datasetName, @PathVariable("version") Integer version) {
        ModelMapper modelMapper = new ModelMapper();

        return modelMapper.map(datasetService.getDataset(datasetName, version), DatasetDto.class);
    }

    @GetMapping(path = "/download/{datasetName}/{version}")
    public void downloadFile(@PathVariable("datasetName") String datasetName, @PathVariable("version") Integer downloadVersion, HttpServletResponse response) throws Exception {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"example.csv\"");

        datasetService.downloadFile(datasetName, downloadVersion, response);
    }

    /*@GetMapping(path = "/homogeneusSamples")
    public double homogeneusSamples(@RequestParam(value = "newRows") Integer newRows){
        return datasetService.homogeneusSamples(newRows);
    }*/
    //endregion
    
    //region Delete Methods
    @DeleteMapping(path = "/{datasetId}")
    public void deleteDataset(@PathVariable("datasetId") Long datasetId) {
        datasetService.deleteDataset(datasetId);
    }
    //endregion
}
