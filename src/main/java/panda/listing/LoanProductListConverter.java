package panda.listing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import panda.listing.enums.LoanProduct;

@Converter
public class LoanProductListConverter implements AttributeConverter<List<LoanProduct>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<LoanProduct>> LOAN_PRODUCT_LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<LoanProduct> attribute) {
        List<LoanProduct> safe = attribute == null ? List.of() : attribute;
        try {
            return OBJECT_MAPPER.writeValueAsString(safe);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize loanProducts", ex);
        }
    }

    @Override
    public List<LoanProduct> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(OBJECT_MAPPER.readValue(dbData, LOAN_PRODUCT_LIST_TYPE));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to deserialize loanProducts", ex);
        }
    }
}
