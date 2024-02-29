package TFG.Data_Analysis.Service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileReaderService {
    public void fileReader(String path) throws IOException {
        Map<Integer, Map<String, Float>> dataset = new HashMap<>();
        int numFila = 1;

        try (Reader reader = Files.newBufferedReader(Paths.get(path));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());) {
            Map<String, Integer> headerMap = csvParser.getHeaderMap();

            for (CSVRecord csvRecord : csvParser) {
                Map<String, Float> fila = new HashMap<>();
                for (String columnName : headerMap.keySet()) {
                    Float columnValue = Float.valueOf(csvRecord.get(columnName));
                    // Procesar los datos como se requiera
                    fila.put(columnName, columnValue);
                }
                dataset.put(numFila, fila);
                ++numFila;
            }
        }
    }
}
