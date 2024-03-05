package TFG.Data_Analysis.Controller;

import TFG.Data_Analysis.Service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/file")
public class FileController {
    @Autowired
    FileService fileReaderService;

    //region Post Methods
    @PostMapping
    public double fileReaderCSV(@RequestBody String path) throws IOException {
        return fileReaderService.fileReader(path);
    }

    @PostMapping(path = "/filter")
    public double applyFilter(@RequestBody List<String> filter) {
        return fileReaderService.applyFilter(filter);
    }
    //endregion

    //region Get Methods
    @GetMapping(path = "/download/{version}")
    public void downloadFile(@PathVariable("version") Integer downloadVersion, HttpServletResponse response) throws Exception {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"example.csv\"");

        fileReaderService.downloadFile(downloadVersion, response);
    }
    //endregion
}
