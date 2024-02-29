package TFG.Data_Analysis.Controller;

import TFG.Data_Analysis.Service.FileReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/file")
public class FileReaderController {
    @Autowired
    FileReaderService fileReaderService;

    @PostMapping
    public double fileReaderCSV(@RequestBody String path) throws IOException {
        return fileReaderService.fileReader(path);
    }
}
