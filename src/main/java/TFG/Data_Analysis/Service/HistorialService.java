package TFG.Data_Analysis.Service;

import TFG.Data_Analysis.Repository.Entity.HistorialEntity;
import TFG.Data_Analysis.Repository.HistorialRepository;
import TFG.Data_Analysis.Service.Model.DatasetModel;
import TFG.Data_Analysis.Service.Model.HistorialModel;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class HistorialService {
    @Autowired
    HistorialRepository historialRepository;
    @Autowired
    UserService userService;

    public Map<String, TreeMap<Integer, DatasetModel>> chargeUserDatasets(String email) {

        long userId = userService.getUserIdByEmail(email);

        if (historialRepository.findByUserId(userId) != null && !historialRepository.findByUserId(userId).getVersions().isEmpty()) {
            return historialRepository.findByUserId(userId).getVersions();
        }
        else {
            return new HashMap<>();
        }
    }

    public void saveDataset(long userId, Map<String, TreeMap<Integer, DatasetModel>> versions) {
        ModelMapper modelMapper = new ModelMapper();

        historialRepository.save(modelMapper.map(new HistorialModel(userId, versions), HistorialEntity.class));
    }
}