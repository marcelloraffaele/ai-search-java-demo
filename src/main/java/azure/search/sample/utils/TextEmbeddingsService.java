package azure.search.sample.utils;

import java.util.List;

public interface TextEmbeddingsService {
    
    public List<List<Float>> createEmbeddingBatch(List<String> texts);
}