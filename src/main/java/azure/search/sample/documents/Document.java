package azure.search.sample.documents;

import com.azure.search.documents.indexes.SimpleField;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude.Include;


@JsonInclude(Include.NON_NULL)
public class Document {
    
    @JsonProperty("Id")
    @SimpleField(isKey = true)
    public String Id;

    @JsonProperty("FileName")
    public String filename;

    @JsonProperty("Content")
    public String content;

    @Override
    public String toString()
    {
        try
        {
            return new ObjectMapper().writeValueAsString(this);
        }
        catch (JsonProcessingException e)
        {
            e.printStackTrace();
            return "";
        }
    }
}