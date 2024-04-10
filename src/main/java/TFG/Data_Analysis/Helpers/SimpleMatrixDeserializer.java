package TFG.Data_Analysis.Helpers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;

import java.io.IOException;

public class SimpleMatrixDeserializer extends JsonDeserializer<SimpleMatrix> {
    @Override
    public SimpleMatrix deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        int numRows = node.get("numRows").asInt();
        int numCols = node.get("numCols").asInt();
        JsonNode dataNode = node.get("ddrm").get("data");

        DMatrixRMaj ddrm = new DMatrixRMaj(numRows, numCols);
        for (int i = 0; i < ddrm.data.length; i++) {
            ddrm.data[i] = dataNode.get(i).asDouble();
        }

        return SimpleMatrix.wrap(ddrm);
    }
}
