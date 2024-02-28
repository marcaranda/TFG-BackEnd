package TFG.Data_Analysis.Helpers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVParser;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileReader {
    @PostMapping
    public void fileReaderCSV(@RequestBody String path) throws Exception, IOException {
        try (Reader reader = Files.newBufferedReader(Paths.get(path));
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
                Map<String, Integer> headerMap = csvParser.getHeaderMap();

                for (CSVRecord csvRecord : csvParser) {
                    for (String columnName : headerMap.keySet()) {
                        String columnValue = csvRecord.get("ColumnName");
                        // Procesar los datos como se requiera
                        System.out.println(columnValue);
                    }
                }
        }
    }
}
