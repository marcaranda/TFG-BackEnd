package TFG.Data_Analysis.Helpers;

import com.opencsv.*;
import com.opencsv.exceptions.CsvException;
import org.springframework.core.io.ClassPathResource;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

@RestController
public class FileReader {
    public void fileReaderExcel(String path) throws Exception, IOException {
        ClassPathResource resource = new ClassPathResource(path);
        InputStream inputStream = resource.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        boolean end = false;
        Iterator<Row> rowIterator = sheet.iterator();
        while (rowIterator.hasNext() && !end) {
            Row row = rowIterator.next();

            // Verifica si la fila está vacía (no hay más datos)
            boolean isEmptyRow = true;
            for (int i = 0; i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                if (cell.getCellType() != CellType.BLANK) {
                    isEmptyRow = false;
                    break;
                }
            }

            if (isEmptyRow) {
                end = true; // Marca el fin cuando se encuentra una fila vacía
            } else {
                // Procesa la fila actual si no está vacía
            }
        }
        inputStream.close();
    }

    public void fileReaderCSV(String path) throws Exception, IOException {
        ClassPathResource resource = new ClassPathResource(path);
        InputStream inputStream = resource.getInputStream();

        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',') // Define el separador (coma en este caso)
                .withIgnoreQuotations(false) // Opcional: considera comillas como caracteres especiales
                .build();

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(inputStream))
                .withCSVParser(parser)
                .build()) {

            List<String[]> allData = csvReader.readAll();

            for (String[] row : allData) {
                // Aquí puedes procesar cada fila del archivo CSV
                // Cada fila es un array de strings que contiene los valores de las columnas
                // Por ejemplo, si tienes columnas "Nombre", "Edad", "Ciudad", puedes acceder a los valores así:
                // String nombre = row[0];
                // String edad = row[1];
                // String ciudad = row[2];
            }
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
        inputStream.close();
    }
}
