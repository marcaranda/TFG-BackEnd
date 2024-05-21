package TFG.Data_Analysis;

import TFG.Data_Analysis.Controller.DatasetController;
import TFG.Data_Analysis.Helpers.Error.NotFound;
import TFG.Data_Analysis.Service.Model.DatasetModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DatasetController.class)
public class DatasetControllerTest extends AbstractBaseControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    public void shouldReturnDatasetList() throws Exception {
        Map<String, List<DatasetModel>> expectedDatasets = new HashMap<>();
        ArrayList<DatasetModel> listDatasets = new ArrayList<>();
        DatasetModel datasetModel = new DatasetModel();
        datasetModel.setDatasetId(1L);
        datasetModel.setDatasetName("test");
        datasetModel.setVersion(0);
        listDatasets.add(datasetModel);
        expectedDatasets.put("test", listDatasets);

        when(datasetService.getHistory(1, "name", "test", "test")).thenReturn(expectedDatasets);

        mockMvc.perform(get("/file/historial/userId/1?orderBy=name&search=test&datasetName=test"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.test[0].datasetName").value("test"))
                .andExpect(jsonPath("$.test[0].version").value(0));
    }

    @Test
    public void shouldReturnDataset() throws Exception {
        DatasetModel expectedDataset = new DatasetModel();
        expectedDataset.setDatasetId(1L);
        expectedDataset.setDatasetName("test");
        expectedDataset.setVersion(0);

        when(datasetService.getDataset(1L)).thenReturn(expectedDataset);

        mockMvc.perform(get("/file/datasetId/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.datasetName").value("test"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    public void shouldReturnDatasetNotFound() throws Exception {
        when(datasetService.getDataset(1L)).thenThrow(new NotFound("La versión solicitada no existe."));

        mockMvc.perform(get("/file/datasetId/1"))
                .andExpect(status().isNotFound());
    }
}
